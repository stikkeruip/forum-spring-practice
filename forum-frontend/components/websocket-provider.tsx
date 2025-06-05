'use client'

import { createContext, useContext, useEffect } from 'react'
import { useWebSocketManager } from '@/hooks/use-websocket-manager'
import { useAuth } from '@/components/auth-provider'
import { WebSocketErrorBoundary } from '@/components/websocket-error-boundary'
import { OnlineUser, UserStatus, OnlineCount } from '@/lib/types'

interface WebSocketContextType {
  isConnected: boolean
  isConnecting: boolean
  onlineUsers: OnlineUser[]
  onlineCount: number
  error: string | null
  reconnectAttempts: number
  connect: (token?: string) => Promise<void>
  disconnect: () => void
  sendHeartbeat: () => void
  sendMessage: (destination: string, body: any) => void
  subscribe: <T = any>(event: 'connection-status' | 'user-status' | 'online-count' | 'online-users' | 'notification' | 'unread-count' | 'error', callback: (data: T) => void) => () => void
  resetCircuitBreaker: () => void
  getErrorStats: () => any
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined)

interface WebSocketProviderProps {
  children: React.ReactNode
}

export function WebSocketProvider({ children }: WebSocketProviderProps) {
  const { isAuthenticated, token } = useAuth()
  
  // Use the singleton WebSocket manager with controlled connection
  const webSocketState = useWebSocketManager({
    autoConnect: false, // We'll manually control connection based on auth state
    token,
    enabled: isAuthenticated
  })

  // Connect/disconnect based on authentication state
  useEffect(() => {
    if (isAuthenticated && token) {
      if (!webSocketState.isConnected && !webSocketState.isConnecting) {
        webSocketState.connect(token)
      }
    } else if (!isAuthenticated) {
      webSocketState.disconnect()
      // Reset circuit breaker and clear state on logout
      webSocketState.manager.resetCircuitBreaker()
    }
  }, [isAuthenticated, token, webSocketState.isConnected, webSocketState.isConnecting])


  return (
    <WebSocketErrorBoundary
      onError={(error, errorInfo) => {
        console.error('WebSocket Error Boundary:', error?.message || error, errorInfo)
        // Could send to monitoring service here
      }}
    >
      <WebSocketContext.Provider value={{
        ...webSocketState,
        error: webSocketState.error && typeof webSocketState.error === 'string' ? webSocketState.error : webSocketState.error instanceof Error ? webSocketState.error.message : null,
        resetCircuitBreaker: () => webSocketState.manager.resetCircuitBreaker(),
        getErrorStats: () => webSocketState.manager.getErrorStats()
      }}>
        {children}
      </WebSocketContext.Provider>
    </WebSocketErrorBoundary>
  )
}

export function useWebSocketContext() {
  const context = useContext(WebSocketContext)
  if (context === undefined) {
    throw new Error('useWebSocketContext must be used within a WebSocketProvider')
  }
  return context
}