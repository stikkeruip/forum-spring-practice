"use client"

import { useState, useEffect, useCallback } from "react"
import { Bell, Check, CheckCheck, Heart, MessageCircle, Trash2, Users } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import type { Notification, NotificationCount } from "@/lib/types"
import { formatDistanceToNow } from "date-fns"
import { useWebSocketContext } from "@/components/websocket-provider"

interface NotificationsProps {
  token?: string
}

export function Notifications({ token }: NotificationsProps) {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [isOpen, setIsOpen] = useState(false)

  // Use the centralized WebSocket context instead of creating a new connection
  const { isConnected, subscribe } = useWebSocketContext()

  // Fetch notifications from API
  const fetchNotifications = async () => {
    if (!token) {
      setNotifications([])
      setUnreadCount(0)
      return
    }

    try {
      setLoading(true)
      const response = await fetch('/api/notifications', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) throw new Error('Failed to fetch notifications')

      const data = await response.json()
      setNotifications(data.content || [])
      
      // Fetch unread count
      const countResponse = await fetch('/api/notifications/unread-count', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })
      
      if (countResponse.ok) {
        const countData: NotificationCount = await countResponse.json()
        setUnreadCount(countData.count)
      }
    } catch (error) {
      console.error('Failed to fetch notifications:', error)
      setNotifications([])
      setUnreadCount(0)
    } finally {
      setLoading(false)
    }
  }

  // Mark notifications as read
  const markAsRead = async (notificationIds: number[]) => {
    if (!token) {
      return
    }

    try {
      await fetch('/api/notifications/mark-read', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ notificationIds })
      })

      // Update local state
      setNotifications(prev => prev.map(n => 
        notificationIds.includes(n.id) ? { ...n, read: true } : n
      ))
      setUnreadCount(prev => Math.max(0, prev - notificationIds.length))
    } catch (error) {
      console.error('Failed to mark notifications as read:', error)
    }
  }

  // Mark all notifications as read
  const markAllAsRead = async () => {
    if (!token) {
      return
    }

    try {
      await fetch('/api/notifications/mark-all-read', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      setNotifications(prev => prev.map(n => ({ ...n, read: true })))
      setUnreadCount(0)
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error)
    }
  }

  // Get notification icon
  const getNotificationIcon = (type: Notification['type']) => {
    switch (type) {
      case 'POST_LIKED':
      case 'COMMENT_LIKED':
        return <Heart className="h-4 w-4 text-red-500" />
      case 'POST_COMMENTED':
      case 'COMMENT_REPLIED':
        return <MessageCircle className="h-4 w-4 text-blue-500" />
      case 'POST_DELETED_BY_MODERATOR':
      case 'COMMENT_DELETED_BY_MODERATOR':
        return <Trash2 className="h-4 w-4 text-orange-500" />
      default:
        return <Bell className="h-4 w-4" />
    }
  }

  // Handle real-time notification updates via WebSocket subscription
  useEffect(() => {
    const unsubscribeNotification = subscribe('notification', (notification: Notification) => {
      setNotifications(prev => {
        // Add new notification and limit to 50 for performance
        const updated = [notification, ...prev].slice(0, 50)
        // Optional: Remove duplicates based on ID
        const seen = new Set()
        return updated.filter(n => {
          if (seen.has(n.id)) return false
          seen.add(n.id)
          return true
        })
      })
      setUnreadCount(prev => prev + 1)
    })

    const unsubscribeUnreadCount = subscribe('unread-count', (countData: NotificationCount) => {
      setUnreadCount(countData.count)
    })

    return () => {
      unsubscribeNotification()
      unsubscribeUnreadCount()
    }
  }, [subscribe])

  // Load notifications on mount
  useEffect(() => {
    fetchNotifications()
  }, [token])

  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <Badge 
              variant="destructive" 
              className="absolute -top-1 -right-1 h-5 w-5 flex items-center justify-center text-xs p-0"
            >
              {unreadCount > 99 ? '99+' : unreadCount}
            </Badge>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="end">
        <Card className="border-0 shadow-none">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium">Notifications</CardTitle>
              {unreadCount > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={markAllAsRead}
                  className="h-auto p-1 text-xs"
                >
                  <CheckCheck className="h-3 w-3 mr-1" />
                  Mark all read
                </Button>
              )}
            </div>
          </CardHeader>
          <Separator />
          <CardContent className="p-0">
            {loading ? (
              <div className="p-4 text-center text-sm text-muted-foreground">
                Loading notifications...
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-center text-sm text-muted-foreground">
                No notifications yet
              </div>
            ) : (
              <ScrollArea className="h-[400px]">
                <div className="space-y-1">
                  {notifications.map((notification) => (
                    <div
                      key={notification.id}
                      className={`p-3 cursor-pointer hover:bg-accent transition-colors ${
                        !notification.read ? 'bg-accent/50' : ''
                      }`}
                      onClick={() => {
                        if (!notification.read) {
                          markAsRead([notification.id])
                        }
                      }}
                    >
                      <div className="flex gap-3">
                        <div className="flex-shrink-0">
                          {getNotificationIcon(notification.type)}
                        </div>
                        <div className="flex-1 space-y-1">
                          <p className="text-sm leading-tight">
                            {notification.message}
                          </p>
                          <div className="flex items-center gap-2">
                            <p className="text-xs text-muted-foreground">
                              {formatDistanceToNow(new Date(notification.createdDate), { addSuffix: true })}
                            </p>
                            {!notification.read && (
                              <div className="h-2 w-2 bg-blue-500 rounded-full" />
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            )}
          </CardContent>
        </Card>
      </PopoverContent>
    </Popover>
  )
}