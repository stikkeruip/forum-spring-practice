"use client"

import { useState, useEffect, useCallback } from "react"
import { useWebSocket } from "@/hooks/use-websocket"
import type { Notification, NotificationCount } from "@/lib/types"

interface UseNotificationsProps {
  token?: string
  enabled?: boolean
}

interface NotificationData {
  notifications: Notification[]
  unreadCount: number
  loading: boolean
  error: string | null
}

export function useNotifications({ token, enabled = true }: UseNotificationsProps) {
  const [data, setData] = useState<NotificationData>({
    notifications: [],
    unreadCount: 0,
    loading: false,
    error: null
  })

  const { isConnected, sendMessage } = useWebSocket({
    enabled: enabled && !!token,
    token
  })

  // Fetch notifications from API
  const fetchNotifications = useCallback(async () => {
    if (!token || !enabled) return

    try {
      setData(prev => ({ ...prev, loading: true, error: null }))
      
      const [notificationsResponse, countResponse] = await Promise.all([
        fetch('/api/notifications', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }),
        fetch('/api/notifications/unread-count', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        })
      ])

      if (!notificationsResponse.ok || !countResponse.ok) {
        throw new Error('Failed to fetch notifications')
      }

      const notificationsData = await notificationsResponse.json()
      const countData: NotificationCount = await countResponse.json()

      setData(prev => ({
        ...prev,
        notifications: notificationsData.content || [],
        unreadCount: countData.count,
        loading: false
      }))
    } catch (error) {
      setData(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Unknown error',
        loading: false
      }))
    }
  }, [token, enabled])

  // Mark notifications as read
  const markAsRead = useCallback(async (notificationIds: number[]) => {
    if (!token) return

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
      setData(prev => ({
        ...prev,
        notifications: prev.notifications.map(n => 
          notificationIds.includes(n.id) ? { ...n, read: true } : n
        ),
        unreadCount: Math.max(0, prev.unreadCount - notificationIds.length)
      }))
    } catch (error) {
      console.error('Failed to mark notifications as read:', error)
    }
  }, [token])

  // Mark all notifications as read
  const markAllAsRead = useCallback(async () => {
    if (!token) return

    try {
      await fetch('/api/notifications/mark-all-read', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      setData(prev => ({
        ...prev,
        notifications: prev.notifications.map(n => ({ ...n, read: true })),
        unreadCount: 0
      }))
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error)
    }
  }, [token])

  // Handle real-time notification updates
  useEffect(() => {
    if (!isConnected || !enabled) return

    const handleNotification = (notification: Notification) => {
      setData(prev => ({
        ...prev,
        notifications: [notification, ...prev.notifications].slice(0, 50), // Keep only last 50
        unreadCount: prev.unreadCount + 1
      }))
    }

    const handleUnreadCountUpdate = (countData: NotificationCount) => {
      setData(prev => ({
        ...prev,
        unreadCount: countData.count
      }))
    }

    // Subscribe to notification events
    window.addEventListener('notification-received', handleNotification as any)
    window.addEventListener('unread-count-updated', handleUnreadCountUpdate as any)

    return () => {
      window.removeEventListener('notification-received', handleNotification as any)
      window.removeEventListener('unread-count-updated', handleUnreadCountUpdate as any)
    }
  }, [isConnected, enabled])

  // Initial fetch
  useEffect(() => {
    if (enabled && token) {
      fetchNotifications()
    }
  }, [enabled, token, fetchNotifications])

  return {
    ...data,
    markAsRead,
    markAllAsRead,
    refetch: fetchNotifications
  }
}