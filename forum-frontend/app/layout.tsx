import type { Metadata } from 'next'
import './globals.css'
import { AuthProvider } from '@/components/auth-provider'
import { WebSocketProvider } from '@/components/websocket-provider'
import { WebSocketFallback } from '@/components/websocket-fallback'
import { Toaster } from '@/components/ui/toaster'

export const metadata: Metadata = {
  title: 'Forum App',
  description: 'A modern forum application',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <WebSocketProvider>
            <WebSocketFallback>
              {children}
              <Toaster />
            </WebSocketFallback>
          </WebSocketProvider>
        </AuthProvider>
      </body>
    </html>
  )
}
