"use client"

import type React from "react"
import Link from "next/link"
import ProgressiveAuth from "@/components/progressive-auth"
import { WhosOnline } from "@/components/whos-online"

export default function ForumLayout({ children, isCreating, setIsCreating }: { children: React.ReactNode; isCreating?: boolean; setIsCreating?: (value: boolean) => void }) {
    return (
        <div className="flex min-h-screen flex-col bg-background">
            <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
                <div className="container flex h-16 items-center justify-between px-4 mx-auto">
                    {/* Logo */}
                    <Link href="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
                        <div className="h-8 w-8 rounded-full bg-gradient-to-br from-violet-500 to-indigo-700"></div>
                        <span className="text-xl font-semibold tracking-tight">Forum</span>
                    </Link>

                    {/* Create Post Button */}
                    <div className="absolute left-1/2 transform -translate-x-1/2">
                        <button
                            onClick={() => setIsCreating?.(true)}
                            disabled={isCreating}
                            className="inline-flex items-center justify-center rounded-md bg-gradient-to-r from-violet-500 to-indigo-600 hover:from-violet-600 hover:to-indigo-700 text-white px-4 py-2 text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isCreating ? "Creating..." : "Create Post"}
                        </button>
                    </div>

                    {/* Progressive Auth Component */}
                    <ProgressiveAuth />
                </div>
            </header>

            <main className="flex-1">
                <div className="container py-6 px-4 mx-auto">
                    <div className="flex gap-6 max-w-6xl mx-auto">
                        <div className="flex-1 max-w-4xl">{children}</div>
                        <aside className="hidden lg:block w-80 flex-shrink-0">
                            <div className="sticky top-20">
                                <WhosOnline />
                            </div>
                        </aside>
                    </div>
                </div>
            </main>

            <footer className="border-t py-4">
                <div className="container flex justify-center text-sm text-muted-foreground mx-auto">
                    © 2025 Forum. All rights reserved.
                </div>
            </footer>
        </div>
    )
}