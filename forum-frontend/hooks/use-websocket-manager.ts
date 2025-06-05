'use client'

import { useEffect, useState, useCallback } from 'react'
import WebSocketManager from '@/lib/websocket-manager'
import { OnlineUser, UserStatus, OnlineCount, Notification, NotificationCount } from '@/lib/types'

interface WebSocketHookState {
  isConnected: boolean
  isConnecting: boolean
  onlineUsers: OnlineUser[]
  onlineCount: number
  error: string | null
  reconnectAttempts: number
}

interface UseWebSocketManagerOptions {
  /** Whether to auto-connect when token is available */
  autoConnect?: boolean
  /** Authentication token (optional, will use localStorage if not provided) */
  token?: string
  /** Whether WebSocket should be enabled */
  enabled?: boolean
}

/**
 * React hook for using the singleton WebSocket manager
 * 
 * @example
 * ```tsx
 * function MyComponent() {
 *   const { 
 *     isConnected, 
 *     onlineUsers, 
 *     sendMessage,
 *     subscribe 
 *   } = useWebSocketManager()
 *   
 *   useEffect(() => {
 *     const unsubscribe = subscribe('notification', (notification) => {
 *       console.log('New notification:', notification)
 *     })
 *     return unsubscribe
 *   }, [subscribe])
 *   
 *   return <div>Connected: {isConnected ? 'Yes' : 'No'}</div>
 * }
 * ```
 */
export function useWebSocketManager(options: UseWebSocketManagerOptions = {}) {
  const { autoConnect = true, token, enabled = true } = options
  
  const [state, setState] = useState<WebSocketHookState>(() => {
    const manager = WebSocketManager.getInstance()
    return manager.getState()
  })

  const manager = WebSocketManager.getInstance()

  // Subscribe to connection status changes
  useEffect(() => {
    const unsubscribe = manager.subscribe('connection-status', (connectionState) => {
      setState(prevState => ({
        ...prevState,
        ...connectionState
      }))
    })

    // Initialize state from manager
    const initialState = manager.getState()
    setState(initialState)

    return unsubscribe
  }, [manager])

  // Subscribe to online users changes
  useEffect(() => {
    const unsubscribe = manager.subscribe('online-users', (users: OnlineUser[]) => {
      setState(prevState => ({
        ...prevState,
        onlineUsers: users
      }))
    })

    return unsubscribe
  }, [manager])

  // Subscribe to online count changes
  useEffect(() => {
    const unsubscribe = manager.subscribe('online-count', (count: OnlineCount) => {
      setState(prevState => ({
        ...prevState,
        onlineCount: count.count
      }))
    })

    return unsubscribe
  }, [manager])

  // Auto-connect effect
  useEffect(() => {
    if (!enabled) {
      manager.disconnect()
      return
    }

    if (autoConnect) {
      const authToken = token || localStorage.getItem('authToken')
      if (authToken && !state.isConnected && !state.isConnecting) {
        manager.connect(authToken)
      }
    }
  }, [manager, autoConnect, token, enabled, state.isConnected, state.isConnecting])

  // Memoized functions
  const connect = useCallback((authToken?: string) => {
    return manager.connect(authToken)
  }, [manager])

  const disconnect = useCallback(() => {
    manager.disconnect()
  }, [manager])

  const sendMessage = useCallback((destination: string, body: any) => {
    manager.sendMessage(destination, body)
  }, [manager])

  const sendHeartbeat = useCallback(() => {
    manager.sendHeartbeat()
  }, [manager])

  const subscribe = useCallback(<T = any>(
    event: 'connection-status' | 'user-status' | 'online-count' | 'online-users' | 'notification' | 'unread-count' | 'error',
    callback: (data: T) => void
  ) => {
    return manager.subscribe(event, callback)
  }, [manager])

  return {
    // State
    ...state,
    
    // Actions
    connect,
    disconnect,
    sendMessage,
    sendHeartbeat,
    subscribe,
    
    // Manager instance (for advanced usage)
    manager
  }
}

/**
 * Hook specifically for subscribing to user status changes
 */
export function useUserStatus(callback: (status: UserStatus) => void) {
  const { subscribe } = useWebSocketManager({ autoConnect: false })
  
  useEffect(() => {
    return subscribe('user-status', callback)
  }, [subscribe, callback])
}

/**
 * Hook specifically for subscribing to notifications
 */
export function useNotifications(callback: (notification: Notification) => void) {
  const { subscribe } = useWebSocketManager({ autoConnect: false })
  
  useEffect(() => {
    return subscribe('notification', callback)
  }, [subscribe, callback])
}

/**
 * Hook specifically for subscribing to unread count changes
 */
export function useUnreadCount(callback: (count: NotificationCount) => void) {
  const { subscribe } = useWebSocketManager({ autoConnect: false })
  
  useEffect(() => {
    return subscribe('unread-count', callback)
  }, [subscribe, callback])
}

/**
 * Hook for getting online users with real-time updates
 */
export function useOnlineUsers() {
  const { onlineUsers, onlineCount, isConnected } = useWebSocketManager()
  
  return {
    onlineUsers,
    onlineCount,
    isConnected
  }
}

export default useWebSocketManager