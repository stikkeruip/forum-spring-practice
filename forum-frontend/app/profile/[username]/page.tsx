"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import { useAuth } from "@/components/auth-provider"
import ForumLayout from "@/components/forum-layout"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { useToast } from "@/hooks/use-toast"
import { CalendarDays, MessageSquare, Heart, HeartCrack, Trash2, MoreHorizontal, RotateCcw } from "lucide-react"
import { restorePost } from "@/lib/data"
import { FriendButton } from "@/components/friend-button"
import type { UserProfile, ProfilePost } from "@/lib/types"

export default function UserProfilePage() {
    const params = useParams()
    const username = params.username as string
    const { isAuthenticated, username: currentUsername, token, role } = useAuth()
    const [profile, setProfile] = useState<UserProfile | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [restoringPosts, setRestoringPosts] = useState<Set<number>>(new Set())
    const { toast } = useToast()

    useEffect(() => {
        if (!username) {
            setError("Username not provided")
            setLoading(false)
            return
        }

        const fetchProfile = async () => {
            try {
                // For now, if it's the current user, use the existing profile endpoint
                // In the future, we'll need a new endpoint to get any user's profile
                if (isAuthenticated && username === currentUsername) {
                    const response = await fetch('/api/profile', {
                        method: 'GET',
                        headers: {
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json'
                        }
                    })

                    if (!response.ok) {
                        throw new Error('Failed to fetch profile')
                    }

                    const profileData: UserProfile = await response.json()
                    setProfile(profileData)
                } else {
                    // For other users, we need a new endpoint
                    const response = await fetch(`/api/users/${username}/profile`, {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                            ...(token && { 'Authorization': `Bearer ${token}` })
                        }
                    })

                    if (!response.ok) {
                        if (response.status === 404) {
                            throw new Error('User not found')
                        }
                        throw new Error('Failed to fetch profile')
                    }

                    const profileData: UserProfile = await response.json()
                    setProfile(profileData)
                }
            } catch (err) {
                console.error('Error fetching profile:', err)
                setError(err.message || 'Failed to load profile data')
            } finally {
                setLoading(false)
            }
        }

        fetchProfile()
    }, [username, isAuthenticated, token, currentUsername])

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        })
    }

    const formatDateTime = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    if (loading) {
        return (
            <ForumLayout>
                <div className="max-w-4xl mx-auto space-y-6">
                <Card>
                    <CardHeader>
                        <Skeleton className="h-8 w-48" />
                        <Skeleton className="h-4 w-32" />
                    </CardHeader>
                </Card>
                
                <div className="space-y-4">
                    <Skeleton className="h-6 w-32" />
                    {[1, 2, 3].map((i) => (
                        <Card key={i}>
                            <CardHeader>
                                <Skeleton className="h-6 w-3/4" />
                                <Skeleton className="h-4 w-1/2" />
                            </CardHeader>
                            <CardContent>
                                <Skeleton className="h-20 w-full" />
                            </CardContent>
                        </Card>
                    ))}
                </div>
                </div>
            </ForumLayout>
        )
    }

    if (error) {
        return (
            <ForumLayout>
                <div className="max-w-4xl mx-auto">
                    <Card>
                        <CardContent className="pt-6">
                            <div className="text-center text-destructive">
                                <p>{error}</p>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </ForumLayout>
        )
    }

    if (!profile) {
        return (
            <ForumLayout>
                <div className="max-w-4xl mx-auto">
                    <Card>
                        <CardContent className="pt-6">
                            <div className="text-center text-muted-foreground">
                                <p>Profile not found</p>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </ForumLayout>
        )
    }

    // Check if this is the current user's profile or if the viewer is admin/moderator
    const isOwnProfile = isAuthenticated && username === currentUsername
    const canSeeDeletedPosts = isOwnProfile || role === 'ADMIN' || role === 'MODERATOR'

    const handleRestore = async (post: ProfilePost) => {
        if (!isAuthenticated || !token) {
            toast({
                title: "Authentication required",
                description: "You must be logged in to restore posts.",
                variant: "destructive"
            })
            return
        }

        // Check if user can restore this post
        const isOwner = currentUsername === post.owner
        const isAdminOrModerator = role === 'ADMIN' || role === 'MODERATOR'
        const canRestore = isAdminOrModerator || (isOwner && post.deletedBy === currentUsername)

        if (!canRestore) {
            const message = isOwner && post.deletedBy !== currentUsername 
                ? "You cannot restore a post deleted by an administrator or moderator."
                : "You are not authorized to restore this post."
            
            toast({
                title: "Unauthorized",
                description: message,
                variant: "destructive"
            })
            return
        }

        setRestoringPosts(prev => new Set(prev).add(post.postId))
        
        try {
            await restorePost(post.postId.toString(), token)

            toast({
                title: "Post restored",
                description: "The post has been restored successfully."
            })

            // Remove the post from deleted posts and add it to active posts
            setProfile(prevProfile => {
                if (!prevProfile) return null
                
                return {
                    ...prevProfile,
                    posts: [...prevProfile.posts, { ...post, deletedDate: null, deletedBy: null }],
                    deletedPosts: prevProfile.deletedPosts.filter(p => p.postId !== post.postId)
                }
            })
        } catch (error) {
            console.error("Error restoring post:", error)
            toast({
                title: "Failed to restore post",
                description: error.message || "Please try again.",
                variant: "destructive"
            })
        } finally {
            setRestoringPosts(prev => {
                const newSet = new Set(prev)
                newSet.delete(post.postId)
                return newSet
            })
        }
    }

    return (
        <ForumLayout>
            <div className="max-w-4xl mx-auto space-y-6">
            {/* Profile Header */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-violet-500 to-indigo-700 flex items-center justify-center text-white font-semibold text-lg">
                                {profile.username.charAt(0).toUpperCase()}
                            </div>
                            <div>
                                <h1 className="text-2xl font-bold">
                                    {profile.username}
                                    {isOwnProfile && <span className="text-sm font-normal text-muted-foreground ml-2">(You)</span>}
                                </h1>
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <CalendarDays className="h-4 w-4" />
                                    <span>Joined {formatDate(profile.createdDate)}</span>
                                </div>
                            </div>
                        </div>
                        
                        {/* Friend Button */}
                        <FriendButton targetUsername={profile.username} />
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="text-center">
                            <div className="text-2xl font-bold text-primary">{profile.posts.length}</div>
                            <div className="text-sm text-muted-foreground">Active Posts</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-primary">
                                {profile.posts.reduce((total, post) => total + post.likeCount, 0)}
                            </div>
                            <div className="text-sm text-muted-foreground">Total Likes</div>
                        </div>
                        <div className="text-center">
                            <div className="text-2xl font-bold text-primary">
                                {profile.posts.reduce((total, post) => total + post.commentCount, 0)}
                            </div>
                            <div className="text-sm text-muted-foreground">Total Comments</div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Active Posts Section */}
            <div className="space-y-4">
                <h2 className="text-xl font-semibold">
                    {isOwnProfile ? 'Your' : `${profile.username}'s`} Active Posts ({profile.posts.length})
                </h2>
                
                {profile.posts.length === 0 ? (
                    <Card>
                        <CardContent className="pt-6">
                            <div className="text-center text-muted-foreground">
                                <p>{isOwnProfile ? "You haven't" : `${profile.username} hasn't`} created any posts yet.</p>
                            </div>
                        </CardContent>
                    </Card>
                ) : (
                    <div className="space-y-4">
                        {profile.posts.map((post: ProfilePost) => (
                            <Card key={post.postId}>
                                <CardHeader>
                                    <div className="flex items-start justify-between">
                                        <div className="space-y-1 flex-1">
                                            <CardTitle className="text-lg">
                                                {post.title}
                                            </CardTitle>
                                            <div className="flex items-center gap-4 text-sm text-muted-foreground">
                                                <span>Created: {formatDateTime(post.createdDate)}</span>
                                                {post.updatedDate !== post.createdDate && (
                                                    <span>Updated: {formatDateTime(post.updatedDate)}</span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </CardHeader>
                                <CardContent>
                                    <p className="text-muted-foreground mb-4 line-clamp-3">
                                        {post.content}
                                    </p>
                                    
                                    <div className="flex items-center gap-6 text-sm">
                                        <div className="flex items-center gap-1 text-green-600">
                                            <Heart className="h-4 w-4" />
                                            <span>{post.likeCount}</span>
                                        </div>
                                        <div className="flex items-center gap-1 text-red-600">
                                            <HeartCrack className="h-4 w-4" />
                                            <span>{post.dislikeCount}</span>
                                        </div>
                                        <div className="flex items-center gap-1 text-blue-600">
                                            <MessageSquare className="h-4 w-4" />
                                            <span>{post.commentCount}</span>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                )}
            </div>

            {/* Deleted Posts Section - Only visible to own profile or admins/moderators */}
            {canSeeDeletedPosts && profile.deletedPosts && profile.deletedPosts.length > 0 && (
                <div className="space-y-4">
                    <h2 className="text-xl font-semibold text-destructive">
                        {isOwnProfile ? 'Your' : `${profile.username}'s`} Deleted Posts ({profile.deletedPosts.length})
                    </h2>
                    
                    <div className="space-y-4">
                        {profile.deletedPosts.map((post: ProfilePost) => (
                            <Card key={post.postId} className="opacity-60 border-destructive/20">
                                <CardHeader>
                                    <div className="flex items-start justify-between">
                                        <div className="space-y-1 flex-1">
                                            <CardTitle className="text-lg flex items-center gap-2">
                                                {post.title}
                                                <Badge variant="destructive" className="text-xs">
                                                    <Trash2 className="h-3 w-3 mr-1" />
                                                    Deleted
                                                </Badge>
                                            </CardTitle>
                                            <div className="flex items-center gap-4 text-sm text-muted-foreground">
                                                <span>Created: {formatDateTime(post.createdDate)}</span>
                                                {post.deletedDate && (
                                                    <span className="text-destructive">Deleted: {formatDateTime(post.deletedDate)}</span>
                                                )}
                                                {post.deletedBy && (
                                                    <span className="text-destructive">by {post.deletedBy}</span>
                                                )}
                                            </div>
                                        </div>
                                        
                                        {/* Three dots menu for restore functionality */}
                                        {isAuthenticated && (
                                            <DropdownMenu>
                                                <DropdownMenuTrigger asChild>
                                                    <Button variant="ghost" size="icon" className="h-8 w-8 hover:bg-muted border border-gray-200">
                                                        <MoreHorizontal className="h-4 w-4 text-gray-600" />
                                                        <span className="sr-only">Open menu</span>
                                                    </Button>
                                                </DropdownMenuTrigger>
                                                <DropdownMenuContent align="end">
                                                    {(() => {
                                                        const isOwner = currentUsername === post.owner
                                                        const isAdminOrModerator = role === 'ADMIN' || role === 'MODERATOR'
                                                        const canRestore = isAdminOrModerator || (isOwner && post.deletedBy === currentUsername)
                                                        
                                                        return canRestore ? (
                                                            <DropdownMenuItem 
                                                                onClick={() => handleRestore(post)}
                                                                disabled={restoringPosts.has(post.postId)}
                                                                className="text-green-600 focus:text-green-600"
                                                            >
                                                                <RotateCcw className="h-4 w-4 mr-2" />
                                                                {restoringPosts.has(post.postId) ? 'Restoring...' : 'Restore post'}
                                                            </DropdownMenuItem>
                                                        ) : (
                                                            <DropdownMenuItem disabled className="text-muted-foreground">
                                                                <RotateCcw className="h-4 w-4 mr-2" />
                                                                Cannot restore
                                                            </DropdownMenuItem>
                                                        )
                                                    })()}
                                                </DropdownMenuContent>
                                            </DropdownMenu>
                                        )}
                                    </div>
                                </CardHeader>
                                <CardContent>
                                    <p className="text-muted-foreground mb-4 line-clamp-3">
                                        {post.content}
                                    </p>
                                    
                                    <div className="flex items-center gap-6 text-sm">
                                        <div className="flex items-center gap-1 text-green-600">
                                            <Heart className="h-4 w-4" />
                                            <span>{post.likeCount}</span>
                                        </div>
                                        <div className="flex items-center gap-1 text-red-600">
                                            <HeartCrack className="h-4 w-4" />
                                            <span>{post.dislikeCount}</span>
                                        </div>
                                        <div className="flex items-center gap-1 text-blue-600">
                                            <MessageSquare className="h-4 w-4" />
                                            <span>{post.commentCount}</span>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                </div>
            )}
            </div>
        </ForumLayout>
    )
}