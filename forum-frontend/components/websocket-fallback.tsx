'use client'

import React, { useState, useEffect, useCallback } from 'react'
import { useWebSocketContext } from '@/components/websocket-provider'
import { OnlineUser } from '@/lib/types'

interface WebSocketFallbackProps {
  children: React.ReactNode
  fallbackPollingInterval?: number
}

/**
 * Provides fallback mechanisms when WebSocket is unavailable
 * Implements polling-based updates as a graceful degradation
 */
export function WebSocketFallback({ 
  children, 
  fallbackPollingInterval = 30000 // 30 seconds
}: WebSocketFallbackProps) {
  const { isConnected, error, resetCircuitBreaker } = useWebSocketContext()
  const [fallbackMode, setFallbackMode] = useState(false)
  const [pollingData, setPollingData] = useState<{
    onlineUsers: OnlineUser[]
    onlineCount: number
    lastUpdate: number
  }>({
    onlineUsers: [],
    onlineCount: 0,
    lastUpdate: 0
  })

  // Monitor connection health
  useEffect(() => {
    const shouldEnableFallback = !isConnected && error !== null
    
    if (shouldEnableFallback !== fallbackMode) {
      setFallbackMode(shouldEnableFallback)
    }
  }, [isConnected, error, fallbackMode])

  // Polling fallback for critical data
  useEffect(() => {
    if (!fallbackMode) return

    const pollForUpdates = async () => {
      try {
        const token = localStorage.getItem('authToken')
        if (!token) return

        // Poll for online users count (simplified)
        const response = await fetch('/api/users/online-count', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        })

        if (response.ok) {
          const data = await response.json()
          setPollingData(prev => ({
            ...prev,
            onlineCount: data.count || 0,
            lastUpdate: Date.now()
          }))
        }
      } catch (error) {
        console.error('Polling fallback failed:', error)
      }
    }

    // Initial poll
    pollForUpdates()

    // Set up polling interval
    const interval = setInterval(pollForUpdates, fallbackPollingInterval)

    return () => clearInterval(interval)
  }, [fallbackMode, fallbackPollingInterval])


  if (fallbackMode) {
    const timeSinceLastUpdate = pollingData.lastUpdate 
      ? Math.floor((Date.now() - pollingData.lastUpdate) / 1000)
      : 0

    return (
      <div className="min-h-screen bg-background">
        {/* Render children with fallback context - notification banner removed */}
        <FallbackContext.Provider value={{
          isFallbackMode: true,
          onlineCount: pollingData.onlineCount,
          onlineUsers: pollingData.onlineUsers,
          lastUpdate: pollingData.lastUpdate,
          manualRefresh: () => window.location.reload()
        }}>
          {children}
        </FallbackContext.Provider>
      </div>
    )
  }

  return (
    <>
      {/* Reconnection popup removed */}
      {children}
    </>
  )
}

// Context for fallback mode
interface FallbackContextType {
  isFallbackMode: boolean
  onlineCount: number
  onlineUsers: OnlineUser[]
  lastUpdate: number
  manualRefresh: () => void
}

const FallbackContext = React.createContext<FallbackContextType>({
  isFallbackMode: false,
  onlineCount: 0,
  onlineUsers: [],
  lastUpdate: 0,
  manualRefresh: () => {}
})

export function useFallbackContext() {
  return React.useContext(FallbackContext)
}

/**
 * Hook to check if we're in fallback mode and get fallback data
 */
export function useWebSocketFallback() {
  const { isConnected, error } = useWebSocketContext()
  const fallbackContext = useFallbackContext()
  
  return {
    isFallbackMode: fallbackContext.isFallbackMode,
    isRealTimeAvailable: isConnected && !error,
    fallbackData: {
      onlineCount: fallbackContext.onlineCount,
      onlineUsers: fallbackContext.onlineUsers,
      lastUpdate: fallbackContext.lastUpdate
    },
    actions: {
      manualRefresh: fallbackContext.manualRefresh
    }
  }
}