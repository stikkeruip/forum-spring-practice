"use client"

import { useState, useEffect, useRef } from "react"
import Link from "next/link"
import { MessageSquare, Heart, Share2, MoreHorizontal, Eye, X, Send, ThumbsDown, Trash2, ChevronDown, ChevronUp, Reply } from "lucide-react"

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator } from "@/components/ui/dropdown-menu"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Separator } from "@/components/ui/separator"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { useToast } from "@/hooks/use-toast"
import { useAuth } from "@/components/auth-provider"
import { useWebSocketContext } from "@/components/websocket-provider"
import { OnlineStatus } from "@/components/online-status"
import { reactToComment } from "@/lib/data"
import type { Post, Comment } from "@/lib/types"

// Store GSAP instances outside component to prevent re-initialization
let gsapInstance: any = null
let scrollTriggerInstance: any = null
let isGsapLoading = false
let isGsapLoaded = false

// Initialize GSAP only once
const initializeGSAP = async () => {
  if (isGsapLoaded || isGsapLoading) return { gsap: gsapInstance, ScrollTrigger: scrollTriggerInstance }

  isGsapLoading = true

  try {
    // Dynamically import GSAP only on client side
    const [gsapModule, scrollTriggerModule] = await Promise.all([
      import("gsap").then(mod => mod.gsap || mod.default),
      import("gsap/ScrollTrigger").then(mod => mod.ScrollTrigger || mod.default)
    ])

    // Register plugin
    if (gsapModule && scrollTriggerModule) {
      gsapModule.registerPlugin(scrollTriggerModule)
      gsapInstance = gsapModule
      scrollTriggerInstance = scrollTriggerModule
      isGsapLoaded = true
      console.log("GSAP initialized successfully")
    }
  } catch (error) {
    console.error("Failed to initialize GSAP:", error)
  } finally {
    isGsapLoading = false
  }

  return { gsap: gsapInstance, ScrollTrigger: scrollTriggerInstance }
}

export default function PostList({ posts, isCreating, setIsCreating, onPostCreated, onPostDeleted }: { posts: Post[]; isCreating: boolean; setIsCreating: (value: boolean) => void; onPostCreated: (newPost: Post) => void; onPostDeleted: (postId: string) => void }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [animationsReady, setAnimationsReady] = useState(false)
  const contextRef = useRef<any>(null)

  useEffect(() => {
    let mounted = true

    const setupAnimations = async () => {
      // Only run on client
      if (typeof window === "undefined") return

      const { gsap, ScrollTrigger } = await initializeGSAP()

      if (!mounted || !gsap || !ScrollTrigger || !containerRef.current) return

      // Clean up previous context
      if (contextRef.current) {
        contextRef.current.revert()
      }

      try {
        contextRef.current = gsap.context(() => {
          // Set initial state without animations (cards visible immediately)
          gsap.set(".post-card", {
            y: 0,
            opacity: 1,
            scale: 1,
            rotationX: 0,
          })

          if (mounted) {
            setAnimationsReady(true)
          }
        }, containerRef)
      } catch (error) {
        console.error("Animation setup error:", error)
      }
    }

    setupAnimations()

    return () => {
      mounted = false
      if (contextRef.current) {
        contextRef.current.revert()
      }
    }
  }, [posts.length])

  return (
      <div ref={containerRef} className="space-y-8">
        {isCreating && (
          <CreatePostCard 
            onCancel={() => setIsCreating(false)}
            onSubmit={(newPost) => {
              onPostCreated(newPost)
              setIsCreating(false)
            }}
          />
        )}
        {posts.map((post, index) => (
            <PostCard
                key={post.id}
                post={post}
                index={index}
                animationsReady={animationsReady}
                onPostDeleted={onPostDeleted}
            />
        ))}
      </div>
  )
}

function CreatePostCard({ onCancel, onSubmit }: { onCancel: () => void; onSubmit: (newPost: Post) => void }) {
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const cardRef = useRef<HTMLDivElement>(null)
  const { toast } = useToast()
  const { token, isAuthenticated, username } = useAuth()

  const handleSubmit = async () => {
    if (!title.trim() || !content.trim()) return
    
    setIsSubmitting(true)
    
    try {
      console.log('Auth state:', { isAuthenticated, token: token ? 'present' : 'missing' })
      
      if (!isAuthenticated || !token) {
        throw new Error('You must be logged in to create a post')
      }

      // API call to create post
      const response = await fetch('/api/posts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          title: title.trim(),
          content: content.trim()
        })
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Failed to create post')
      }

      const createdPost = await response.json()
      console.log('API Response:', createdPost)
      
      // Transform API response to match Post type
      const newPost: Post = {
        id: (createdPost.postId || createdPost.id || Date.now()).toString(),
        title: createdPost.title,
        content: createdPost.content,
        author: {
          id: username || 'unknown',
          name: username || 'Unknown User',
          avatar: "/placeholder.svg?height=40&width=40"
        },
        date: createdPost.createdDate ? new Date(createdPost.createdDate).toLocaleDateString() : new Date().toLocaleDateString(),
        likes: createdPost.likeCount || 0,
        dislikes: createdPost.dislikeCount || 0,
        comments: createdPost.commentCount || 0,
        userReaction: createdPost.userReaction || null // Include user reaction state
      }
      
      onSubmit(newPost)
      
      // Show success toast
      toast({
        title: "Post created!",
        description: "Your post has been successfully published.",
      })
    } catch (error) {
      console.error("Error creating post:", error)
      
      // Show error toast
      toast({
        title: "Failed to create post",
        description: error.message || "Please try again.",
        variant: "destructive"
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  useEffect(() => {
    // Animate in the create post card
    const animateIn = async () => {
      if (cardRef.current) {
        const { gsap } = await initializeGSAP()
        if (gsap) {
          gsap.fromTo(cardRef.current, 
            { y: -50, opacity: 0, scale: 0.95 },
            { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "power2.out" }
          )
        }
      }
    }
    animateIn()
  }, [])

  return (
    <div
      ref={cardRef}
      className="post-card group relative overflow-hidden rounded-2xl border bg-gradient-to-br from-card to-card/50 text-card-foreground shadow-lg backdrop-blur-sm"
    >
      {/* Animated background gradient */}
      <div className="absolute inset-0 bg-gradient-to-r from-violet-500/10 via-transparent to-indigo-500/10" />

      <div className="relative z-10">
        <div className="flex items-center justify-between p-6 pb-4">
          <div className="flex items-center gap-4">
            <Avatar className="ring-2 ring-violet-500/50">
              <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white">
                {username ? username.substring(0, 1).toUpperCase() : 'Y'}
              </AvatarFallback>
            </Avatar>
            <div>
              <p className="font-medium text-violet-600">{username || 'User'}</p>
              <p className="text-sm text-muted-foreground">Creating new post...</p>
            </div>
          </div>

          <Button
            variant="ghost"
            size="icon"
            onClick={onCancel}
            className="h-8 w-8 text-muted-foreground hover:text-destructive"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="px-6 space-y-4">
          <Input
            placeholder="What's your post title?"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="text-xl font-bold border-none px-0 shadow-none focus-visible:ring-0 placeholder:text-muted-foreground/60"
          />
          
          <Textarea
            placeholder="Share your thoughts..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="min-h-[120px] border-none px-0 shadow-none focus-visible:ring-0 resize-none placeholder:text-muted-foreground/60"
          />
        </div>

        <div className="px-6 pb-6 pt-4">
          <Separator className="mb-4" />
          
          <div className="flex items-center justify-between">
            <div className="text-sm text-muted-foreground">
              {title.length > 0 && content.length > 0 ? "Ready to post" : "Add a title and content to continue"}
            </div>
            
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={onCancel}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              
              <Button
                onClick={handleSubmit}
                disabled={!title.trim() || !content.trim() || isSubmitting}
                className="bg-gradient-to-r from-violet-500 to-indigo-600 hover:from-violet-600 hover:to-indigo-700 text-white gap-2"
              >
                {isSubmitting ? (
                  <>
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                    Posting...
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4" />
                    Post
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function PostCard({ post, index, animationsReady, onPostDeleted }: { post: Post; index: number; animationsReady: boolean; onPostDeleted: (postId: string) => void }) {
  const [userReaction, setUserReaction] = useState<'LIKE' | 'DISLIKE' | null>(post.userReaction || null)
  const [likeCount, setLikeCount] = useState(post.likes)
  const [dislikeCount, setDislikeCount] = useState(post.dislikes || 0)
  const [isExpanded, setIsExpanded] = useState(false)
  const [showCommentBox, setShowCommentBox] = useState(false)
  const [commentText, setCommentText] = useState("")
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [showModal, setShowModal] = useState(false)
  const [comments, setComments] = useState<Comment[]>([])
  const [isLoadingComments, setIsLoadingComments] = useState(false)
  const [modalCommentText, setModalCommentText] = useState("")
  const [isSubmittingModalComment, setIsSubmittingModalComment] = useState(false)
  const cardRef = useRef<HTMLDivElement>(null)
  const contentRef = useRef<HTMLDivElement>(null)
  const imageRef = useRef<HTMLImageElement>(null)
  const commentBoxRef = useRef<HTMLDivElement>(null)
  const { toast } = useToast()
  const { token, isAuthenticated, username, role } = useAuth()
  const { onlineUsers } = useWebSocketContext()
  const [isDeleting, setIsDeleting] = useState(false)

  // Check if the post author is online
  const isAuthorOnline = onlineUsers.some(user => user.username === post.author.id)

  const handleReaction = async (reactionType: 'LIKE' | 'DISLIKE') => {
    if (!isAuthenticated || !token) {
      toast({
        title: "Authentication required",
        description: "You must be logged in to react to posts.",
        variant: "destructive"
      })
      return
    }

    try {
      const response = await fetch(`/api/posts/${post.id}/reactions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ reactionType })
      })

      if (!response.ok) {
        throw new Error('Failed to react to post')
      }

      const updatedPost = await response.json()
      
      // Update counts based on API response
      setLikeCount(updatedPost.likeCount || 0)
      setDislikeCount(updatedPost.dislikeCount || 0)
      
      // Update user reaction based on server response
      setUserReaction(updatedPost.userReaction || null)

      // Heart explosion animation for likes
      if (reactionType === 'LIKE' && userReaction !== 'LIKE' && cardRef.current && animationsReady) {
        const { gsap } = await initializeGSAP()
        if (!gsap) return

        const heart = cardRef.current.querySelector(".heart-icon")
        if (heart) {
          gsap.fromTo(
              heart,
              { scale: 1 },
              {
                scale: 1.5,
                duration: 0.2,
                yoyo: true,
                repeat: 1,
                ease: "power2.inOut",
              }
          )

          // Create floating hearts
          for (let i = 0; i < 5; i++) {
            const floatingHeart = document.createElement("div")
            floatingHeart.innerHTML = "❤️"
            floatingHeart.style.position = "absolute"
            floatingHeart.style.pointerEvents = "none"
            floatingHeart.style.fontSize = "12px"
            floatingHeart.style.zIndex = "1000"

            const rect = heart.getBoundingClientRect()
            floatingHeart.style.left = `${rect.left + Math.random() * 20}px`
            floatingHeart.style.top = `${rect.top}px`

            document.body.appendChild(floatingHeart)

            gsap.to(floatingHeart, {
              y: -50,
              x: `random(-30, 30)`,
              opacity: 0,
              scale: 0,
              duration: 1,
              ease: "power2.out",
              onComplete: () => floatingHeart.remove(),
            })
          }
        }
      }
      
    } catch (error) {
      console.error("Error reacting to post:", error)
      toast({
        title: "Failed to react",
        description: "Please try again.",
        variant: "destructive"
      })
    }
  }

  const handleComment = async () => {
    if (!isAuthenticated || !token) {
      toast({
        title: "Authentication required",
        description: "You must be logged in to comment.",
        variant: "destructive"
      })
      return
    }

    if (!commentText.trim()) {
      toast({
        title: "Comment cannot be empty",
        description: "Please enter a comment before submitting.",
        variant: "destructive"
      })
      return
    }

    setIsSubmittingComment(true)

    try {
      const response = await fetch(`/api/posts/${post.id}/comments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          content: commentText.trim()
        })
      })

      if (!response.ok) {
        throw new Error('Failed to post comment')
      }

      // Success
      toast({
        title: "Comment posted!",
        description: "Your comment has been added successfully."
      })

      // Clear the comment text and hide the box
      setCommentText("")
      setShowCommentBox(false)

      // Optionally refresh the post or increment comment count
      // For now, just increment the local comment count
      // In a full app, you'd want to refresh the post data

    } catch (error) {
      console.error("Error posting comment:", error)
      toast({
        title: "Failed to post comment",
        description: "Please try again.",
        variant: "destructive"
      })
    } finally {
      setIsSubmittingComment(false)
    }
  }

  const toggleCommentBox = async () => {
    if (!isAuthenticated || !token) {
      toast({
        title: "Authentication required",
        description: "You must be logged in to comment.",
        variant: "destructive"
      })
      return
    }

    const newShowCommentBox = !showCommentBox
    setShowCommentBox(newShowCommentBox)

    // Simple animation without interfering with interaction
    if (newShowCommentBox && commentBoxRef.current && animationsReady) {
      const { gsap } = await initializeGSAP()
      if (gsap) {
        gsap.fromTo(commentBoxRef.current,
          { opacity: 0 },
          { 
            opacity: 1, 
            duration: 0.15, 
            ease: "power2.out",
            onComplete: () => {
              // Ensure the element is fully interactive after animation
              if (commentBoxRef.current) {
                commentBoxRef.current.style.pointerEvents = 'auto'
              }
            }
          }
        )
      }
    }
  }

  const toggleExpand = async () => {
    const newExpanded = !isExpanded
    setIsExpanded(newExpanded)

    if (contentRef.current && animationsReady) {
      const { gsap } = await initializeGSAP()
      if (gsap) {
        gsap.to(contentRef.current, {
          height: newExpanded ? "auto" : "100px",
          duration: 0.5,
          ease: "power2.inOut",
        })
      }
    }
  }

  const handleImageLoad = async () => {
    if (imageRef.current && animationsReady) {
      const { gsap } = await initializeGSAP()
      if (gsap) {
        gsap.fromTo(
            imageRef.current,
            { scale: 1.2, opacity: 0 },
            { scale: 1, opacity: 1, duration: 0.8, ease: "power2.out" }
        )
      }
    }
  }

  const fetchComments = async () => {
    setIsLoadingComments(true)
    try {
      const response = await fetch(`/api/posts/${post.id}/comments`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      
      if (!response.ok) {
        throw new Error('Failed to fetch comments')
      }
      
      const fetchedComments = await response.json()
      
      // Initialize userReaction field for all comments
      const commentsWithUserReaction = fetchedComments.map((comment: Comment) => ({
        ...comment,
        likeCount: comment.likeCount || 0,
        dislikeCount: comment.dislikeCount || 0,
        userReaction: comment.userReaction || null
      }))
      
      setComments(commentsWithUserReaction)
    } catch (error) {
      console.error('Failed to fetch comments:', error)
      setComments([])
    } finally {
      setIsLoadingComments(false)
    }
  }

  const handleModalCommentSubmit = async () => {
    if (!modalCommentText.trim() || !isAuthenticated || !token) return

    setIsSubmittingModalComment(true)
    try {
      const response = await fetch(`/api/posts/${post.id}/comments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ content: modalCommentText.trim() })
      })

      if (!response.ok) {
        throw new Error('Failed to post comment')
      }

      toast({
        title: "Comment posted",
        description: "Your comment has been added successfully."
      })

      setModalCommentText("")
      // Refresh comments
      await fetchComments()
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to post comment. Please try again.",
        variant: "destructive"
      })
    } finally {
      setIsSubmittingModalComment(false)
    }
  }

  const handleDeleteComment = async (commentId: number) => {
    if (!isAuthenticated || !token) {
      toast({
        title: "Authentication required",
        description: "You must be logged in to delete comments.",
        variant: "destructive"
      })
      return
    }

    try {
      const response = await fetch(`/api/posts/${post.id}/comments/${commentId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (!response.ok) {
        throw new Error('Failed to delete comment')
      }

      toast({
        title: "Comment deleted",
        description: "The comment has been deleted successfully."
      })

      // Refresh comments
      await fetchComments()
    } catch (error) {
      console.error("Error deleting comment:", error)
      toast({
        title: "Failed to delete comment",
        description: "Please try again.",
        variant: "destructive"
      })
    }
  }

  const handleCommentReaction = async (commentId: number, reactionType: 'LIKE' | 'DISLIKE') => {
    if (!isAuthenticated || !token) {
      toast({
        title: "Authentication required",
        description: "You must be logged in to react to comments.",
        variant: "destructive"
      })
      return
    }

    try {
      const updatedComment = await reactToComment(post.id, commentId, reactionType, token)
      
      // Helper function to recursively update comments and replies
      const updateCommentInTree = (comments: Comment[]): Comment[] => {
        return comments.map(comment => {
          if (comment.id === commentId) {
            // Use server response for accurate reaction state
            return { 
              ...comment, 
              likeCount: updatedComment.likeCount || 0,
              dislikeCount: updatedComment.dislikeCount || 0,
              userReaction: updatedComment.userReaction || null
            }
          }
          
          // If this comment has replies, recursively update them
          if (comment.replies && comment.replies.length > 0) {
            return {
              ...comment,
              replies: updateCommentInTree(comment.replies)
            }
          }
          
          return comment
        })
      }
      
      // Update the comments state with the recursive function
      setComments(prevComments => updateCommentInTree(prevComments))

    } catch (error) {
      console.error("Error reacting to comment:", error)
      toast({
        title: "Failed to react to comment",
        description: "Please try again.",
        variant: "destructive"
      })
    }
  }

  const openModal = () => {
    setShowModal(true)
    fetchComments()
  }

  const handleDelete = async () => {
    if (!isAuthenticated || !token) {
      toast({
        title: "Authentication required",
        description: "You must be logged in to delete posts.",
        variant: "destructive"
      })
      return
    }

    // Check if user can delete this post
    const canDelete = username === post.author.id || role === 'ADMIN' || role === 'MODERATOR'
    if (!canDelete) {
      toast({
        title: "Unauthorized",
        description: "You can only delete your own posts.",
        variant: "destructive"
      })
      return
    }

    setIsDeleting(true)
    try {
      const response = await fetch(`/api/posts/${post.id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (!response.ok) {
        throw new Error('Failed to delete post')
      }

      toast({
        title: "Post deleted",
        description: "The post has been deleted successfully."
      })

      // Animate out the card before removing
      if (cardRef.current && animationsReady) {
        const { gsap } = await initializeGSAP()
        if (gsap) {
          await gsap.to(cardRef.current, {
            opacity: 0,
            scale: 0.95,
            y: -20,
            duration: 0.3,
            ease: "power2.inOut"
          })
        }
      }

      onPostDeleted(post.id)
    } catch (error) {
      console.error("Error deleting post:", error)
      toast({
        title: "Failed to delete post",
        description: "Please try again.",
        variant: "destructive"
      })
    } finally {
      setIsDeleting(false)
    }
  }


  return (
      <div
          ref={cardRef}
          className="post-card group relative overflow-hidden rounded-2xl border bg-gradient-to-br from-card to-card/50 text-card-foreground shadow-lg backdrop-blur-sm transition-all duration-300 hover:shadow-2xl"
          style={{
            transformStyle: "preserve-3d",
            perspective: "1000px",
          }}
      >
        {/* Animated background gradient */}
        <div className="absolute inset-0 bg-gradient-to-r from-violet-500/5 via-transparent to-indigo-500/5 opacity-0 transition-opacity duration-500 group-hover:opacity-100" />

        {/* Glowing border effect */}
        <div className="absolute inset-0 rounded-2xl bg-gradient-to-r from-violet-500 to-indigo-500 opacity-0 blur-sm transition-opacity duration-500 group-hover:opacity-20" />

        <div className="relative z-10">
          <div className="flex items-center justify-between p-6">
            <div className="flex items-center gap-4">
              <div className="relative">
                <Avatar className="ring-2 ring-transparent transition-all duration-300 group-hover:ring-violet-500/50">
                  <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white">
                    {post.author.name.substring(0, 1).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div className="absolute -bottom-1 -right-1">
                  <OnlineStatus isOnline={isAuthorOnline} size="sm" />
                </div>
              </div>
              <div>
                <Link href={`/profile/${post.author.id}`} className="font-medium transition-colors hover:text-violet-600 hover:underline">
                  {post.author.name}
                </Link>
                <p className="text-sm text-muted-foreground">{post.date}</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1 text-xs text-muted-foreground">
                <Eye className="h-3 w-3" />
                <span>{post.likes * 12 + post.comments * 8 + 150}</span>
              </div>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8 hover:bg-transparent">
                    <MoreHorizontal className="h-4 w-4" />
                    <span className="sr-only">Open menu</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="animate-in slide-in-from-top-2">
                  <DropdownMenuItem>Save post</DropdownMenuItem>
                  <DropdownMenuItem>Report</DropdownMenuItem>
                  <DropdownMenuItem>Hide</DropdownMenuItem>
                  {(username === post.author.id || role === 'ADMIN' || role === 'MODERATOR') && (
                    <>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem 
                        onClick={handleDelete}
                        disabled={isDeleting}
                        className="text-destructive focus:text-destructive"
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        {isDeleting ? 'Deleting...' : 'Delete post'}
                      </DropdownMenuItem>
                    </>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>

          <div ref={contentRef} className="px-6 pb-4" style={{ height: isExpanded ? "auto" : "100px", overflow: "hidden" }}>
            <h3 className="mb-3 text-xl font-bold tracking-tight transition-colors group-hover:text-violet-700">
              {post.title}
            </h3>
            <p className="text-muted-foreground leading-relaxed">
              {post.content}
            </p>
          </div>

          {post.content.length > 150 && (
              <div className="px-6 pb-2">
                <Button
                    variant="link"
                    size="sm"
                    onClick={toggleExpand}
                    className="p-0 h-auto text-violet-600 hover:text-violet-700"
                >
                  {isExpanded ? "Show less" : "Read more"}
                </Button>
              </div>
          )}

          {post.image && (
              <div className="relative mx-6 mb-4 overflow-hidden rounded-xl">
                <img
                    ref={imageRef}
                    src={post.image || "/placeholder.svg"}
                    alt="Post attachment"
                    className="w-full object-cover transition-transform duration-700 group-hover:scale-105"
                    style={{ maxHeight: "300px", opacity: animationsReady ? 1 : 0 }}
                    onLoad={handleImageLoad}
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
              </div>
          )}

          <div className="px-6 pb-6">
            <div className="mb-4 flex items-center text-sm text-muted-foreground">
            <span className="flex items-center gap-1">
              <Heart className="h-3 w-3" />
              {likeCount} likes
            </span>
              <span className="mx-3">•</span>
              <span className="flex items-center gap-1">
              <ThumbsDown className="h-3 w-3" />
              {dislikeCount} dislikes
            </span>
              <span className="mx-3">•</span>
              <button 
                onClick={openModal}
                className="flex items-center gap-1 hover:text-blue-500 transition-colors cursor-pointer"
              >
                <MessageSquare className="h-3 w-3" />
                {post.comments} comments
              </button>
            </div>

            <Separator className="mb-4" />

            <div className="flex items-center justify-between">
              <Button
                  variant="ghost"
                  size="sm"
                  className={`gap-2 transition-all duration-300 hover:bg-transparent hover:text-rose-500 ${
                      userReaction === 'LIKE' ? "text-rose-500" : "text-muted-foreground hover:text-rose-500"
                  }`}
                  onClick={() => handleReaction('LIKE')}
              >
                <Heart
                    className="heart-icon h-4 w-4 transition-all duration-200"
                    fill={userReaction === 'LIKE' ? "currentColor" : "none"}
                />
                <span>Like</span>
              </Button>

              <Button
                  variant="ghost"
                  size="sm"
                  className={`gap-2 transition-all duration-300 hover:bg-transparent hover:text-orange-500 ${
                      userReaction === 'DISLIKE' ? "text-orange-500" : "text-muted-foreground hover:text-orange-500"
                  }`}
                  onClick={() => handleReaction('DISLIKE')}
              >
                <ThumbsDown
                    className="h-4 w-4 transition-all duration-200"
                    fill={userReaction === 'DISLIKE' ? "currentColor" : "none"}
                />
                <span>Dislike</span>
              </Button>

              <Button
                  variant="ghost"
                  size="sm"
                  className={`gap-2 transition-all duration-300 hover:bg-transparent hover:text-blue-500 ${
                      showCommentBox ? "text-blue-500" : "text-muted-foreground"
                  }`}
                  onClick={toggleCommentBox}
              >
                <MessageSquare className="h-4 w-4" />
                <span>Comment</span>
              </Button>

              <Button
                  variant="ghost"
                  size="sm"
                  className="gap-2 text-muted-foreground transition-all duration-300 hover:bg-transparent hover:text-green-500"
              >
                <Share2 className="h-4 w-4" />
                <span>Share</span>
              </Button>
            </div>
          </div>
        </div>

        {/* Comment Box */}
        {showCommentBox && (
          <div 
            ref={commentBoxRef}
            className="relative z-50 border-t bg-white p-4 space-y-3"
            style={{ pointerEvents: 'auto' }}
          >
            <div className="flex items-start gap-3">
              <Avatar className="ring-2 ring-blue-500/50">
                <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white">
                  {username ? username.substring(0, 1).toUpperCase() : 'U'}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 space-y-3 relative z-50">
                <Textarea
                  placeholder="Write a comment..."
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  className="relative z-50 min-h-[80px] resize-none focus-visible:ring-2 focus-visible:ring-blue-500"
                  disabled={isSubmittingComment}
                />
                <div className="flex items-center justify-between">
                  <div className="text-sm text-muted-foreground">
                    {commentText.length > 0 ? `${commentText.length} characters` : "Share your thoughts..."}
                  </div>
                  <div className="flex items-center gap-2 relative z-50">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setShowCommentBox(false)
                        setCommentText("")
                      }}
                      disabled={isSubmittingComment}
                      className="relative z-50"
                    >
                      Cancel
                    </Button>
                    <Button
                      onClick={handleComment}
                      disabled={!commentText.trim() || isSubmittingComment}
                      className="relative z-50 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white"
                      size="sm"
                    >
                      {isSubmittingComment ? (
                        <>
                          <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                          Posting...
                        </>
                      ) : (
                        <>
                          <Send className="h-4 w-4" />
                          Post Comment
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Post Detail Modal */}
        <Dialog open={showModal} onOpenChange={setShowModal}>
          <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="sr-only">Post Details</DialogTitle>
            </DialogHeader>
            
            {/* Large Post View */}
            <div className="space-y-6">
              {/* Post Header */}
              <div className="flex items-center gap-4">
                <div className="relative">
                  <Avatar className="h-12 w-12">
                    <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white text-lg">
                      {post.author.name.substring(0, 1).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <div className="absolute -bottom-1 -right-1">
                    <OnlineStatus isOnline={isAuthorOnline} size="md" />
                  </div>
                </div>
                <div>
                  <Link href={`/profile/${post.author.id}`} className="font-semibold text-lg hover:text-violet-600 hover:underline">
                    {post.author.name}
                  </Link>
                  <p className="text-sm text-muted-foreground">{post.date}</p>
                </div>
              </div>

              {/* Post Content */}
              <div className="space-y-4">
                <h2 className="text-2xl font-bold">{post.title}</h2>
                <div className="text-lg leading-relaxed whitespace-pre-wrap">
                  {post.content}
                </div>
                {post.image && (
                  <div className="rounded-lg overflow-hidden">
                    <img 
                      src={post.image} 
                      alt="Post image" 
                      className="w-full h-auto object-cover"
                    />
                  </div>
                )}
              </div>

              {/* Post Stats */}
              <div className="flex items-center gap-6 text-sm text-muted-foreground border-y py-4">
                <span className="flex items-center gap-1">
                  <Heart className="h-4 w-4" />
                  {likeCount} likes
                </span>
                <span className="flex items-center gap-1">
                  <ThumbsDown className="h-4 w-4" />
                  {dislikeCount} dislikes
                </span>
                <span className="flex items-center gap-1">
                  <MessageSquare className="h-4 w-4" />
                  {comments.length} comments
                </span>
              </div>

              {/* Comments Section */}
              <div className="space-y-4">
                <h3 className="text-xl font-semibold">Comments</h3>
                
                {/* Comment Input */}
                {isAuthenticated && (
                  <div className="border rounded-lg p-4 space-y-3">
                    <div className="flex items-start gap-3">
                      <Avatar>
                        <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white">
                          {username ? username.substring(0, 1).toUpperCase() : 'U'}
                        </AvatarFallback>
                      </Avatar>
                      <div className="flex-1 space-y-3">
                        <Textarea
                          placeholder="Write a comment..."
                          value={modalCommentText}
                          onChange={(e) => setModalCommentText(e.target.value)}
                          className="min-h-[80px] resize-none"
                          disabled={isSubmittingModalComment}
                        />
                        <div className="flex justify-end">
                          <Button
                            onClick={handleModalCommentSubmit}
                            disabled={!modalCommentText.trim() || isSubmittingModalComment}
                            className="bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white"
                          >
                            {isSubmittingModalComment ? (
                              <>
                                <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent mr-2" />
                                Posting...
                              </>
                            ) : (
                              <>
                                <Send className="h-4 w-4 mr-2" />
                                Post Comment
                              </>
                            )}
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Comments List */}
                <div className="space-y-4">
                  {isLoadingComments ? (
                    <div className="flex items-center justify-center py-8">
                      <div className="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
                    </div>
                  ) : comments.length > 0 ? (
                    comments.map((comment) => (
                      <CommentItem 
                        key={comment.id} 
                        comment={comment} 
                        postId={post.id}
                        onDeleteComment={handleDeleteComment}
                        onCommentReaction={handleCommentReaction}
                        onRefreshComments={fetchComments}
                        username={username}
                        role={role}
                        token={token}
                        isAuthenticated={isAuthenticated}
                      />
                    ))
                  ) : (
                    <div className="text-center py-8 text-muted-foreground">
                      <MessageSquare className="h-12 w-12 mx-auto mb-4 opacity-50" />
                      <p>No comments yet. Be the first to comment!</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </div>
  )
}

function CommentItem({ 
  comment, 
  postId, 
  onDeleteComment, 
  onCommentReaction, 
  onRefreshComments,
  username, 
  role, 
  token, 
  isAuthenticated,
  depth = 0 
}: { 
  comment: Comment
  postId: string
  onDeleteComment: (commentId: number) => void
  onCommentReaction: (commentId: number, reactionType: 'LIKE' | 'DISLIKE') => void
  onRefreshComments: () => void
  username: string | null
  role: string | null
  token: string | null
  isAuthenticated: boolean
  depth?: number
}) {
  const [showReplies, setShowReplies] = useState(false)
  const [showReplyForm, setShowReplyForm] = useState(false)
  const [replyText, setReplyText] = useState("")
  const [isSubmittingReply, setIsSubmittingReply] = useState(false)
  const { toast } = useToast()

  const handleReply = async () => {
    if (!replyText.trim() || !isAuthenticated || !token) return

    setIsSubmittingReply(true)
    try {
      const response = await fetch(`/api/posts/${postId}/comments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ 
          content: replyText.trim(),
          parentCommentId: comment.id
        })
      })

      if (!response.ok) {
        throw new Error('Failed to post reply')
      }

      toast({
        title: "Reply posted",
        description: "Your reply has been added successfully."
      })

      setReplyText("")
      setShowReplyForm(false)
      setShowReplies(true) // Show replies after posting
      onRefreshComments() // Refresh to get the new reply
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to post reply. Please try again.",
        variant: "destructive"
      })
    } finally {
      setIsSubmittingReply(false)
    }
  }

  const maxDepth = 3 // Limit nesting depth to prevent infinite nesting

  return (
    <div className={`border rounded-lg p-4 space-y-3 ${depth > 0 ? 'ml-6 border-l-2 border-l-violet-200' : ''}`}>
      <div className="flex items-start gap-3">
        <Avatar>
          <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white">
            {comment.owner ? comment.owner.substring(0, 1).toUpperCase() : 'A'}
          </AvatarFallback>
        </Avatar>
        <div className="flex-1">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <Link href={`/profile/${comment.owner}`} className="font-medium hover:text-violet-600 hover:underline">
                {comment.owner || 'Anonymous'}
              </Link>
              <p className="text-sm text-muted-foreground">
                {comment.createdDate ? new Date(comment.createdDate).toLocaleDateString() : 'Unknown date'}
              </p>
            </div>
            {(username === comment.owner || role === 'ADMIN' || role === 'MODERATOR') && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-6 w-6">
                    <MoreHorizontal className="h-3 w-3" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem 
                    onClick={() => onDeleteComment(comment.id)}
                    className="text-destructive focus:text-destructive"
                  >
                    <Trash2 className="h-3 w-3 mr-2" />
                    Delete comment
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
          <p className="text-sm leading-relaxed mb-3">{comment.content || 'No content'}</p>
          
          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <button 
              onClick={() => onCommentReaction(comment.id, 'LIKE')}
              className={`flex items-center gap-1 transition-colors ${
                comment.userReaction === 'LIKE' 
                  ? 'text-rose-500' 
                  : 'hover:text-rose-500'
              }`}
            >
              <Heart 
                className="h-3 w-3" 
                fill={comment.userReaction === 'LIKE' ? 'currentColor' : 'none'}
              />
              {comment.likeCount || 0}
            </button>
            
            <button 
              onClick={() => onCommentReaction(comment.id, 'DISLIKE')}
              className={`flex items-center gap-1 transition-colors ${
                comment.userReaction === 'DISLIKE' 
                  ? 'text-orange-500' 
                  : 'hover:text-orange-500'
              }`}
            >
              <ThumbsDown 
                className="h-3 w-3" 
                fill={comment.userReaction === 'DISLIKE' ? 'currentColor' : 'none'}
              />
              {comment.dislikeCount || 0}
            </button>

            {depth < maxDepth && (
              <button 
                onClick={() => setShowReplyForm(!showReplyForm)}
                className="flex items-center gap-1 hover:text-blue-500 transition-colors"
              >
                <Reply className="h-3 w-3" />
                Reply
              </button>
            )}

            {comment.replies && comment.replies.length > 0 && (
              <button 
                onClick={() => setShowReplies(!showReplies)}
                className="flex items-center gap-1 hover:text-blue-500 transition-colors"
              >
                {showReplies ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
                {comment.replies.length} {comment.replies.length === 1 ? 'reply' : 'replies'}
              </button>
            )}
          </div>

          {/* Reply Form */}
          {showReplyForm && isAuthenticated && (
            <div className="mt-4 space-y-3">
              <div className="flex items-start gap-3">
                <Avatar className="h-6 w-6">
                  <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white text-xs">
                    {username ? username.substring(0, 1).toUpperCase() : 'U'}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 space-y-2">
                  <Textarea
                    placeholder="Write a reply..."
                    value={replyText}
                    onChange={(e) => setReplyText(e.target.value)}
                    className="min-h-[60px] resize-none text-sm"
                    disabled={isSubmittingReply}
                  />
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setShowReplyForm(false)
                        setReplyText("")
                      }}
                      disabled={isSubmittingReply}
                    >
                      Cancel
                    </Button>
                    <Button
                      onClick={handleReply}
                      disabled={!replyText.trim() || isSubmittingReply}
                      size="sm"
                      className="bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white"
                    >
                      {isSubmittingReply ? (
                        <>
                          <div className="h-3 w-3 animate-spin rounded-full border border-white border-t-transparent mr-1" />
                          Posting...
                        </>
                      ) : (
                        <>
                          <Send className="h-3 w-3 mr-1" />
                          Reply
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Nested Replies */}
          {showReplies && comment.replies && comment.replies.length > 0 && (
            <div className="mt-4 space-y-3">
              {comment.replies.map((reply) => (
                <CommentItem
                  key={reply.id}
                  comment={reply}
                  postId={postId}
                  onDeleteComment={onDeleteComment}
                  onCommentReaction={onCommentReaction}
                  onRefreshComments={onRefreshComments}
                  username={username}
                  role={role}
                  token={token}
                  isAuthenticated={isAuthenticated}
                  depth={depth + 1}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}