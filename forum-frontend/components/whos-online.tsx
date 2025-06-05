'use client'

import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { OnlineStatus } from '@/components/online-status'
import { useWebSocketContext } from '@/components/websocket-provider'
import { useAuth } from '@/components/auth-provider'
import { OnlineUser, UserStatus } from '@/lib/types'
import { Users } from 'lucide-react'
import Link from 'next/link'

export function WhosOnline() {
  const [userStatuses, setUserStatuses] = useState<Map<string, boolean>>(new Map())
  const [mounted, setMounted] = useState(false)
  const { isAuthenticated, username: currentUsername } = useAuth()
  
  // Use the centralized WebSocket context instead of creating a new connection
  const { onlineUsers, onlineCount, isConnected, subscribe } = useWebSocketContext()
  

  useEffect(() => {
    setMounted(true)
  }, [])

  // Subscribe to user status changes with throttling
  useEffect(() => {
    let updateTimeout: NodeJS.Timeout | null = null
    
    const unsubscribe = subscribe('user-status', (status: UserStatus) => {
      // Debounce rapid status updates to prevent UI thrashing
      if (updateTimeout) {
        clearTimeout(updateTimeout)
      }
      
      updateTimeout = setTimeout(() => {
        setUserStatuses(prev => new Map(prev.set(status.username, status.isOnline)))
      }, 100) // 100ms debounce
    })

    return () => {
      if (updateTimeout) clearTimeout(updateTimeout)
      unsubscribe()
    }
  }, [subscribe])

  // Initialize user statuses when online users change
  useEffect(() => {
    if (onlineUsers.length > 0) {
      const statusMap = new Map<string, boolean>()
      onlineUsers.forEach(user => statusMap.set(user.username, true))
      setUserStatuses(statusMap)
    }
  }, [onlineUsers])

  // Merge online users with real-time status updates and optimize for performance
  const displayUsers = onlineUsers
    .slice(0, 20) // Limit processing to first 20 users for performance
    .filter(user => user.username !== currentUsername) // Filter out current user
    .map(user => ({
      ...user,
      isOnline: userStatuses.get(user.username) ?? true
    }))
    .filter(user => user.isOnline)

  const getInitials = (username: string) => {
    return username.substring(0, 1).toUpperCase()
  }

  return (
    <Card className="w-full max-w-sm">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <Users className="w-4 h-4" />
          Who's Online
          {isConnected && (
            <span className="text-sm font-normal text-muted-foreground">
              ({onlineCount})
            </span>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {!mounted ? (
          <div className="text-sm text-muted-foreground text-center py-4">
            <div className="flex items-center justify-center gap-2">
              <div className="w-2 h-2 bg-gray-400 rounded-full" />
              Loading...
            </div>
          </div>
        ) : !isAuthenticated ? (
          <div className="text-sm text-muted-foreground text-center py-4">
            <div className="flex items-center justify-center gap-2">
              <div className="w-2 h-2 bg-yellow-500 rounded-full" />
              Please log in
            </div>
          </div>
        ) : !isConnected ? (
          <div className="text-sm text-muted-foreground text-center py-4">
            <div className="flex items-center justify-center gap-2">
              <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
              Connecting...
            </div>
          </div>
        ) : displayUsers.length === 0 ? (
          <div className="text-sm text-muted-foreground text-center py-4">
            No users online
          </div>
        ) : (
          <div className="space-y-3">
            {displayUsers.slice(0, 10).map((user) => (
              <Link 
                key={user.username} 
                href={`/profile/${user.username}`}
                className="flex items-center gap-3 hover:bg-accent hover:text-accent-foreground rounded-md p-2 -mx-2 transition-colors"
              >
                <div className="relative">
                  <Avatar className="w-8 h-8">
                    <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white text-xs">
                      {getInitials(user.username)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="absolute -bottom-0.5 -right-0.5">
                    <OnlineStatus isOnline={user.isOnline} size="sm" />
                  </div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">
                    {user.username}
                  </p>
                </div>
              </Link>
            ))}
            {displayUsers.length > 10 && (
              <div className="text-xs text-muted-foreground text-center pt-2 border-t">
                and {displayUsers.length - 10} more...
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}