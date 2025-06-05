'use client'

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { OnlineUser, UserStatus, OnlineCount, Notification, NotificationCount } from '@/lib/types'

type EventType = 
  | 'connection-status'
  | 'user-status'
  | 'online-count' 
  | 'online-users'
  | 'notification'
  | 'unread-count'
  | 'error'

type EventCallback<T = any> = (data: T) => void

interface WebSocketManagerState {
  isConnected: boolean
  isConnecting: boolean
  onlineUsers: OnlineUser[]
  onlineCount: number
  error: string | null
  reconnectAttempts: number
}

/**
 * Singleton WebSocket Manager - Ensures only one WebSocket connection per app instance
 * 
 * Usage:
 * const manager = WebSocketManager.getInstance()
 * const unsubscribe = manager.subscribe('online-users', (users) => setUsers(users))
 * manager.connect(token)
 * 
 * // Cleanup
 * unsubscribe()
 */
class WebSocketManager {
  private static instance: WebSocketManager
  private client: Client | null = null
  private subscribers: Map<string, Set<EventCallback>> = new Map()
  private heartbeatInterval: NodeJS.Timeout | null = null
  private reconnectTimeout: NodeJS.Timeout | null = null
  private currentToken: string | null = null
  private connectionPromise: Promise<void> | null = null
  
  // Performance optimization - throttling and debouncing
  private lastHeartbeat: number = 0
  private eventThrottleMap: Map<string, number> = new Map()
  private readonly heartbeatThrottleMs = 10000 // Minimum 10s between heartbeats
  private readonly eventThrottleMs = 1000 // Throttle rapid events to max 1 per second
  private readonly maxOnlineUsers = 100 // Limit online users list for performance
  private readonly maxStoredNotifications = 50 // Limit notification history
  
  // Circuit breaker pattern for error handling
  private circuitBreakerState: 'CLOSED' | 'OPEN' | 'HALF_OPEN' = 'CLOSED'
  private consecutiveFailures = 0
  private lastFailureTime = 0
  private readonly maxFailures = 5 // Open circuit after 5 consecutive failures
  private readonly circuitBreakerTimeout = 30000 // 30 seconds before trying again
  private readonly circuitBreakerHalfOpenTimeout = 10000 // 10 seconds for half-open state
  
  // Enhanced error tracking
  private errorHistory: Array<{ timestamp: number; error: string; type: string }> = []
  private readonly maxErrorHistory = 20
  
  private state: WebSocketManagerState = {
    isConnected: false,
    isConnecting: false,
    onlineUsers: [],
    onlineCount: 0,
    error: null,
    reconnectAttempts: 0
  }

  private readonly maxReconnectAttempts = 5
  private readonly heartbeatIntervalMs = 15000 // 15 seconds - more frequent
  private readonly baseReconnectDelay = 1000 // 1 second

  private constructor() {
    // Private constructor for singleton pattern
  }

  /**
   * Get the singleton instance
   */
  static getInstance(): WebSocketManager {
    if (!WebSocketManager.instance) {
      WebSocketManager.instance = new WebSocketManager()
    }
    return WebSocketManager.instance
  }

  /**
   * Subscribe to WebSocket events
   * @param event Event type to listen to
   * @param callback Function to call when event occurs
   * @returns Unsubscribe function
   */
  subscribe<T = any>(event: EventType, callback: EventCallback<T>): () => void {
    if (!this.subscribers.has(event)) {
      this.subscribers.set(event, new Set())
    }
    
    const eventSubscribers = this.subscribers.get(event)!
    eventSubscribers.add(callback)
    
    // Return unsubscribe function
    return () => {
      eventSubscribers.delete(callback)
      if (eventSubscribers.size === 0) {
        this.subscribers.delete(event)
      }
    }
  }

  /**
   * Emit event to all subscribers with throttling for performance
   */
  private emit<T>(event: EventType, data: T): void {
    // Throttle high-frequency events to prevent UI thrashing
    if (this.shouldThrottleEvent(event)) {
      return
    }

    const eventSubscribers = this.subscribers.get(event)
    if (eventSubscribers) {
      eventSubscribers.forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error(`Error in WebSocket event callback for ${event}:`, error)
        }
      })
    }
  }

  /**
   * Check if event should be throttled for performance
   */
  private shouldThrottleEvent(event: EventType): boolean {
    // Never throttle connection status changes - they're critical
    if (event === 'connection-status') {
      return false
    }
    
    const now = Date.now()
    const lastEmit = this.eventThrottleMap.get(event) || 0
    
    if (now - lastEmit < this.eventThrottleMs) {
      return true // Throttle this event
    }
    
    this.eventThrottleMap.set(event, now)
    return false
  }

  /**
   * Update internal state and notify subscribers
   */
  private updateState(updates: Partial<WebSocketManagerState>): void {
    // Ensure error is always a string or null
    if (updates.error && typeof updates.error !== 'string') {
      updates.error = String(updates.error)
    }
    
    this.state = { ...this.state, ...updates }
    const statusUpdate = {
      isConnected: this.state.isConnected,
      isConnecting: this.state.isConnecting,
      error: this.state.error,
      reconnectAttempts: this.state.reconnectAttempts
    }
    this.emit('connection-status', statusUpdate)
  }

  /**
   * Connect to WebSocket server with circuit breaker pattern
   */
  async connect(token?: string): Promise<void> {
    const authToken = token || this.currentToken || (typeof window !== 'undefined' ? localStorage.getItem('authToken') : null)
    
    if (!authToken) {
      const error = 'No authentication token found'
      this.recordError(error, 'authentication')
      this.updateState({ error })
      this.emit('error', error)
      return
    }

    // Check circuit breaker state
    if (!this.canAttemptConnection()) {
      const error = `Connection circuit breaker is ${this.circuitBreakerState}. Waiting before retry.`
      this.updateState({ error })
      this.emit('error', error)
      return
    }

    // Check if token is expired
    if (this.isTokenExpired(authToken)) {
      const error = 'Authentication token expired'
      this.recordError(error, 'authentication')
      this.updateState({ error })
      this.emit('error', error)
      this.clearAuthAndReload()
      return
    }

    // If already connected with same token, do nothing
    if (this.client?.connected && this.currentToken === authToken) {
      this.onConnectionSuccess()
      return
    }

    // If there's already a connection in progress, wait for it
    if (this.connectionPromise) {
      return this.connectionPromise
    }

    // If connecting state but no promise, reset state
    if (this.state.isConnecting && !this.connectionPromise) {
      this.updateState({ isConnecting: false })
    }

    this.currentToken = authToken
    this.updateState({ isConnecting: true, error: null })

    // Create and store the connection promise
    this.connectionPromise = this.createConnection(authToken)
      .then(() => {
        this.onConnectionSuccess()
      })
      .catch((error) => {
        this.onConnectionFailure(error)
        throw error
      })
      .finally(() => {
        // Clear the promise when connection attempt is done
        this.connectionPromise = null
      })

    try {
      await this.connectionPromise
    } catch (error) {
      console.error('WebSocketManager: Connection failed:', error)
      this.updateState({ 
        isConnecting: false, 
        error: error instanceof Error ? error.message : 'Connection failed' 
      })
    }
  }

  /**
   * Create the actual WebSocket connection
   */
  private async createConnection(authToken: string): Promise<void> {
    // Clean up existing connection without updating state
    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
    this.cleanup()

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${authToken}`
      },
      reconnectDelay: this.calculateReconnectDelay(),
      heartbeatIncoming: 10000,  // 10 seconds - more lenient
      heartbeatOutgoing: 10000,  // 10 seconds - more lenient
    })

    return new Promise((resolve, reject) => {
      client.onConnect = () => {
        this.updateState({ 
          isConnected: true, 
          isConnecting: false, 
          error: null,
          reconnectAttempts: 0 
        })
        
        this.setupSubscriptions(client)
        this.startHeartbeat()
        this.requestInitialData()
        
        resolve()
      }

      client.onStompError = (frame) => {
        const error = frame.headers['message'] || 'WebSocket connection error'
        console.error('WebSocketManager STOMP Error:', error)
        this.updateState({ 
          isConnected: false, 
          isConnecting: false, 
          error 
        })
        this.emit('error', error)
        reject(new Error(error))
      }

      client.onWebSocketClose = () => {
        this.updateState({ isConnected: false, isConnecting: false })
        this.cleanup()
        this.attemptReconnect()
      }

      client.onWebSocketError = (error) => {
        console.error('WebSocketManager Error:', error)
        const errorMessage = 'WebSocket connection failed'
        this.updateState({ 
          isConnected: false, 
          isConnecting: false, 
          error: errorMessage 
        })
        this.emit('error', errorMessage)
        reject(new Error(errorMessage))
      }

      this.client = client
      client.activate()
    })
  }

  /**
   * Set up all WebSocket subscriptions
   */
  private setupSubscriptions(client: Client): void {
    // User status changes
    client.subscribe('/topic/user-status', (message) => {
      try {
        const status: UserStatus = JSON.parse(message.body)
        this.emit('user-status', status)
      } catch (error) {
        console.error('Error parsing user status message:', error)
      }
    })

    // Online count updates
    client.subscribe('/topic/online-count', (message) => {
      try {
        const count: OnlineCount = JSON.parse(message.body)
        this.state.onlineCount = count.count
        this.emit('online-count', count)
      } catch (error) {
        console.error('Error parsing online count message:', error)
      }
    })

    // Online users list with memory optimization
    client.subscribe('/topic/online-users', (message) => {
      try {
        const users: OnlineUser[] = JSON.parse(message.body)
        // Limit online users list to prevent memory bloat
        this.state.onlineUsers = users.slice(0, this.maxOnlineUsers)
        this.emit('online-users', this.state.onlineUsers)
      } catch (error) {
        console.error('Error parsing online users message:', error)
      }
    })

    // Heartbeat responses
    client.subscribe('/user/queue/heartbeat-response', (message) => {
      // Heartbeat acknowledged - connection is healthy
    })

    // User notifications
    client.subscribe('/user/queue/notifications', (message) => {
      try {
        const notification: Notification = JSON.parse(message.body)
        this.emit('notification', notification)
      } catch (error) {
        console.error('Error parsing notification message:', error)
      }
    })

    // Unread count updates
    client.subscribe('/user/queue/unread-count', (message) => {
      try {
        const countData: NotificationCount = JSON.parse(message.body)
        this.emit('unread-count', countData)
      } catch (error) {
        console.error('Error parsing unread count message:', error)
      }
    })
  }

  /**
   * Start sending heartbeats to keep connection alive with throttling
   */
  private startHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
    }
    
    this.heartbeatInterval = setInterval(() => {
      if (this.client?.connected) {
        try {
          // Add jitter to prevent thundering herd with multiple users
          const jitter = Math.random() * 5000 // 0-5 second random delay
          setTimeout(() => {
            if (this.client?.connected) {
              this.client.publish({
                destination: '/app/heartbeat',
                body: 'ping'
              })
              this.lastHeartbeat = Date.now()
            }
          }, jitter)
        } catch (error) {
          console.error('Failed to send heartbeat:', error)
        }
      }
    }, this.heartbeatIntervalMs)
  }

  /**
   * Request initial data after connection
   */
  private requestInitialData(): void {
    // Request initial data immediately after connection
    if (this.client?.connected) {
      try {
        this.client.publish({
          destination: '/app/online-users',
          body: ''
        })
        
        this.client.publish({
          destination: '/app/online-count',
          body: ''
        })
      } catch (error) {
        console.error('WebSocketManager: Error sending initial data requests:', error)
      }
    }
  }

  /**
   * Calculate reconnection delay with exponential backoff
   */
  private calculateReconnectDelay(): number {
    return Math.min(
      this.baseReconnectDelay * Math.pow(2, this.state.reconnectAttempts), 
      30000
    )
  }

  /**
   * Attempt to reconnect with exponential backoff
   */
  private attemptReconnect(): void {
    if (this.state.reconnectAttempts >= this.maxReconnectAttempts) {
      const error = 'Maximum reconnection attempts reached'
      this.updateState({ error })
      this.emit('error', error)
      return
    }

    const delay = this.calculateReconnectDelay()
    
    this.reconnectTimeout = setTimeout(() => {
      this.updateState({ reconnectAttempts: this.state.reconnectAttempts + 1 })
      this.connect()
    }, delay)
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    this.cleanup()
    
    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
    
    // Clear token and connection promise
    this.currentToken = null
    this.connectionPromise = null
    
    this.updateState({ 
      isConnected: false, 
      isConnecting: false,
      error: null,
      reconnectAttempts: 0,
      onlineUsers: [], // Clear online users on disconnect
      onlineCount: 0   // Clear online count on disconnect
    })
  }

  /**
   * Clean up timers and intervals
   */
  private cleanup(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }
  }

  /**
   * Send a message through the WebSocket
   */
  sendMessage(destination: string, body: any): void {
    if (!this.client?.connected) {
      console.warn('WebSocketManager: Cannot send message, not connected')
      return
    }

    try {
      this.client.publish({
        destination,
        body: typeof body === 'string' ? body : JSON.stringify(body)
      })
    } catch (error) {
      console.error('WebSocketManager: Failed to send message:', error)
    }
  }

  /**
   * Send heartbeat manually with throttling
   */
  sendHeartbeat(): void {
    const now = Date.now()
    if (now - this.lastHeartbeat < this.heartbeatThrottleMs) {
      return // Throttle manual heartbeats
    }
    
    this.sendMessage('/app/heartbeat', 'ping')
    this.lastHeartbeat = now
  }

  /**
   * Get current connection state
   */
  getState(): WebSocketManagerState {
    return { ...this.state }
  }

  /**
   * Check if token is expired
   */
  private isTokenExpired(token: string): boolean {
    try {
      const [, payload] = token.split('.')
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
      return decoded.exp && decoded.exp * 1000 < Date.now()
    } catch (e) {
      console.error('Failed to decode token:', e)
      return true
    }
  }

  /**
   * Clear authentication and reload page
   */
  private clearAuthAndReload(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('authToken')
      localStorage.removeItem('username')
      window.location.reload()
    }
  }

  /**
   * Circuit breaker: check if connection attempt is allowed
   */
  private canAttemptConnection(): boolean {
    const now = Date.now()
    
    switch (this.circuitBreakerState) {
      case 'CLOSED':
        return true
        
      case 'OPEN':
        if (now - this.lastFailureTime > this.circuitBreakerTimeout) {
          this.circuitBreakerState = 'HALF_OPEN'
          return true
        }
        return false
        
      case 'HALF_OPEN':
        return true
        
      default:
        return false
    }
  }

  /**
   * Handle successful connection for circuit breaker
   */
  private onConnectionSuccess(): void {
    this.circuitBreakerState = 'CLOSED'
    this.consecutiveFailures = 0
    this.lastFailureTime = 0
  }

  /**
   * Handle connection failure for circuit breaker
   */
  private onConnectionFailure(error: Error): void {
    this.consecutiveFailures++
    this.lastFailureTime = Date.now()
    
    this.recordError(error.message, 'connection')
    
    if (this.consecutiveFailures >= this.maxFailures) {
      this.circuitBreakerState = 'OPEN'
      this.emit('error', `Circuit breaker opened after ${this.maxFailures} consecutive failures`)
    } else if (this.circuitBreakerState === 'HALF_OPEN') {
      this.circuitBreakerState = 'OPEN'
    }
  }

  /**
   * Record error for tracking and analysis
   */
  private recordError(message: string, type: string): void {
    this.errorHistory.push({
      timestamp: Date.now(),
      error: message,
      type
    })
    
    // Keep only recent errors
    if (this.errorHistory.length > this.maxErrorHistory) {
      this.errorHistory = this.errorHistory.slice(-this.maxErrorHistory)
    }
    
    // Emit error for monitoring
    this.emit('error', { message, type, timestamp: Date.now() })
  }

  /**
   * Get error statistics for monitoring
   */
  getErrorStats(): {
    circuitBreakerState: string
    consecutiveFailures: number
    recentErrors: Array<{ timestamp: number; error: string; type: string }>
    errorsByType: Record<string, number>
  } {
    const errorsByType: Record<string, number> = {}
    
    this.errorHistory.forEach(error => {
      errorsByType[error.type] = (errorsByType[error.type] || 0) + 1
    })
    
    return {
      circuitBreakerState: this.circuitBreakerState,
      consecutiveFailures: this.consecutiveFailures,
      recentErrors: this.errorHistory.slice(-10), // Last 10 errors
      errorsByType
    }
  }

  /**
   * Force reset circuit breaker (for manual recovery)
   */
  resetCircuitBreaker(): void {
    this.circuitBreakerState = 'CLOSED'
    this.consecutiveFailures = 0
    this.lastFailureTime = 0
    this.errorHistory = []
  }

  /**
   * Reset the singleton instance (for testing purposes)
   */
  static resetInstance(): void {
    if (WebSocketManager.instance) {
      WebSocketManager.instance.disconnect()
      WebSocketManager.instance = null as any
    }
  }
}

export default WebSocketManager