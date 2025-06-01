"use client"

import type React from "react"

export default function ForumLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-center px-4 mx-auto">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-full bg-gradient-to-br from-violet-500 to-indigo-700"></div>
            <span className="text-xl font-semibold tracking-tight">Forum</span>
          </div>
        </div>
      </header>

      <main className="flex-1">
        <div className="container max-w-4xl py-6 px-4 mx-auto">{children}</div>
      </main>

      <footer className="border-t py-4">
        <div className="container flex justify-center text-sm text-muted-foreground mx-auto">
          © 2025 Forum. All rights reserved.
        </div>
      </footer>
    </div>
  )
}
