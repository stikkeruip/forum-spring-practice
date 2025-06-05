'use client'

import React, { Component, ReactNode } from 'react'
import { AlertTriangle, RefreshCw, Wifi, WifiOff } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

interface WebSocketErrorBoundaryState {
  hasError: boolean
  error: Error | null
  errorInfo: React.ErrorInfo | null
  retryCount: number
  isRecovering: boolean
}

interface WebSocketErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
  maxRetries?: number
  onError?: (error: Error, errorInfo: React.ErrorInfo) => void
}

/**
 * Error boundary specifically designed for WebSocket-related failures
 * Provides graceful degradation and recovery mechanisms
 */
export class WebSocketErrorBoundary extends Component<
  WebSocketErrorBoundaryProps,
  WebSocketErrorBoundaryState
> {
  private retryTimeout: NodeJS.Timeout | null = null
  private readonly maxRetries: number

  constructor(props: WebSocketErrorBoundaryProps) {
    super(props)
    
    this.maxRetries = props.maxRetries ?? 3
    
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      retryCount: 0,
      isRecovering: false
    }
  }

  static getDerivedStateFromError(error: Error): Partial<WebSocketErrorBoundaryState> {
    return {
      hasError: true,
      error
    }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    this.setState({
      error,
      errorInfo
    })

    // Log error for monitoring
    console.error('WebSocket Error Boundary caught an error:', error, errorInfo)
    
    // Call optional error callback
    this.props.onError?.(error, errorInfo)

    // Attempt automatic recovery for certain error types
    if (this.shouldAttemptAutoRecovery(error)) {
      this.attemptAutoRecovery()
    }
  }

  componentWillUnmount() {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout)
    }
  }

  /**
   * Determine if automatic recovery should be attempted
   */
  private shouldAttemptAutoRecovery(error: Error): boolean {
    const recoverable = [
      'WebSocket connection failed',
      'Network Error',
      'Connection lost',
      'STOMP Error'
    ]
    
    return recoverable.some(msg => error.message.includes(msg)) && 
           this.state.retryCount < this.maxRetries
  }

  /**
   * Attempt automatic recovery with exponential backoff
   */
  private attemptAutoRecovery = () => {
    if (this.state.retryCount >= this.maxRetries) {
      return
    }

    this.setState({ isRecovering: true })

    const delay = Math.min(1000 * Math.pow(2, this.state.retryCount), 30000) // Max 30s delay

    this.retryTimeout = setTimeout(() => {
      this.setState(prevState => ({
        hasError: false,
        error: null,
        errorInfo: null,
        retryCount: prevState.retryCount + 1,
        isRecovering: false
      }))
    }, delay)
  }

  /**
   * Manual retry triggered by user
   */
  private handleManualRetry = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      retryCount: 0,
      isRecovering: false
    })
  }

  /**
   * Reset error state completely
   */
  private handleReset = () => {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout)
    }

    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      retryCount: 0,
      isRecovering: false
    })
  }

  /**
   * Get user-friendly error message
   */
  private getErrorMessage(): string {
    const error = this.state.error
    if (!error) return 'An unknown error occurred'

    if (error.message.includes('WebSocket')) {
      return 'Connection lost. Some features may not work properly.'
    }
    if (error.message.includes('Network')) {
      return 'Network connection issue. Please check your internet connection.'
    }
    if (error.message.includes('Authentication')) {
      return 'Authentication failed. Please log in again.'
    }
    
    return 'A connection error occurred. Some features may be unavailable.'
  }

  /**
   * Get severity level for styling
   */
  private getErrorSeverity(): 'warning' | 'destructive' {
    const error = this.state.error
    if (!error) return 'warning'

    if (error.message.includes('Authentication') || error.message.includes('Authorization')) {
      return 'destructive'
    }
    
    return 'warning'
  }

  render() {
    if (this.state.hasError) {
      // Custom fallback if provided
      if (this.props.fallback) {
        return this.props.fallback
      }

      const canRetry = this.state.retryCount < this.maxRetries
      const severity = this.getErrorSeverity()

      return (
        <div className="w-full max-w-2xl mx-auto p-4">
          <Alert variant={severity} className="mb-4">
            <AlertTriangle className="h-4 w-4" />
            <AlertTitle className="flex items-center gap-2">
              <WifiOff className="h-4 w-4" />
              Connection Issue
            </AlertTitle>
            <AlertDescription>
              {this.getErrorMessage()}
            </AlertDescription>
          </Alert>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Wifi className="h-5 w-5" />
                Connection Issue
              </CardTitle>
              <CardDescription>
                Some features may not work properly. You can still browse content, 
                but live updates and notifications are temporarily disabled.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-col sm:flex-row gap-2">
                {canRetry && !this.state.isRecovering && (
                  <Button onClick={this.handleManualRetry} variant="default">
                    <RefreshCw className="w-4 h-4 mr-2" />
                    Retry Connection
                  </Button>
                )}
                
                {this.state.isRecovering && (
                  <Button disabled variant="secondary">
                    <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                    Reconnecting...
                  </Button>
                )}

                <Button onClick={this.handleReset} variant="outline">
                  Reset
                </Button>

                <Button 
                  onClick={() => window.location.reload()} 
                  variant="secondary"
                >
                  Reload Page
                </Button>
              </div>

              {this.state.retryCount >= this.maxRetries && (
                <Alert variant="destructive">
                  <AlertDescription>
                    Maximum retry attempts reached. Please refresh the page or check your network connection.
                  </AlertDescription>
                </Alert>
              )}

              {process.env.NODE_ENV === 'development' && this.state.error && (
                <details className="mt-4 p-4 bg-gray-100 rounded-lg">
                  <summary className="cursor-pointer font-medium">
                    Error Details (Development)
                  </summary>
                  <pre className="mt-2 text-sm overflow-auto">
                    {this.state.error.stack}
                  </pre>
                  {this.state.errorInfo && (
                    <pre className="mt-2 text-sm overflow-auto">
                      {this.state.errorInfo.componentStack}
                    </pre>
                  )}
                </details>
              )}
            </CardContent>
          </Card>
        </div>
      )
    }

    return this.props.children
  }
}

/**
 * Hook version for functional components
 */
export function useWebSocketErrorBoundary() {
  const [error, setError] = React.useState<Error | null>(null)

  const resetError = React.useCallback(() => {
    setError(null)
  }, [])

  const captureError = React.useCallback((error: Error) => {
    setError(error)
  }, [])

  return {
    error,
    resetError,
    captureError,
    hasError: error !== null
  }
}