'use client'

import { cn } from '@/lib/utils'

interface OnlineStatusProps {
  isOnline: boolean
  size?: 'sm' | 'md' | 'lg'
  className?: string
  showText?: boolean
}

export function OnlineStatus({ 
  isOnline, 
  size = 'md', 
  className,
  showText = false 
}: OnlineStatusProps) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8'
  }

  const textSizeClasses = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-base'
  }

  return (
    <div className={cn('flex items-center gap-1.5', className)}>
      <div 
        className={cn(
          'rounded-full border-2 border-background',
          sizeClasses[size],
          isOnline 
            ? 'bg-green-500 shadow-green-500/20 shadow-sm' 
            : 'bg-gray-400 dark:bg-gray-600'
        )}
        title={isOnline ? 'Online' : 'Offline'}
      />
      {showText && (
        <span className={cn(
          'font-medium',
          textSizeClasses[size],
          isOnline 
            ? 'text-green-600 dark:text-green-400' 
            : 'text-gray-500 dark:text-gray-400'
        )}>
          {isOnline ? 'Online' : 'Offline'}
        </span>
      )}
    </div>
  )
}