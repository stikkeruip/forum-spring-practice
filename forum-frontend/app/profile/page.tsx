"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/components/auth-provider"
import ForumLayout from "@/components/forum-layout"
import { Card, CardContent } from "@/components/ui/card"

export default function ProfilePage() {
    const router = useRouter()
    const { isAuthenticated, username } = useAuth()

    useEffect(() => {
        if (isAuthenticated && username) {
            // Redirect to the user's specific profile page
            router.push(`/profile/${username}`)
        }
    }, [isAuthenticated, username, router])

    // Show loading or not authenticated message while redirecting
    if (!isAuthenticated || !username) {
        return (
            <ForumLayout>
                <div className="max-w-4xl mx-auto">
                    <Card>
                        <CardContent className="pt-6">
                            <div className="text-center text-muted-foreground">
                                <p>Please log in to view your profile</p>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </ForumLayout>
        )
    }

    // Show loading while redirecting
    return (
        <ForumLayout>
            <div className="max-w-4xl mx-auto">
                <Card>
                    <CardContent className="pt-6">
                        <div className="text-center text-muted-foreground">
                            <p>Loading profile...</p>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </ForumLayout>
    )
}