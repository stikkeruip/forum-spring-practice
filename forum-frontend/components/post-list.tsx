"use client"

import { useState, useEffect, useRef } from "react"
import { MessageSquare, Heart, Share2, MoreHorizontal, Eye } from "lucide-react"

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Separator } from "@/components/ui/separator"
import type { Post } from "@/lib/types"

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

export default function PostList({ posts }: { posts: Post[] }) {
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
          // Create animations
          gsap.fromTo(
              ".post-card",
              {
                y: 100,
                opacity: 0,
                scale: 0.8,
                rotationX: -15,
              },
              {
                y: 0,
                opacity: 1,
                scale: 1,
                rotationX: 0,
                duration: 0.8,
                stagger: 0.15,
                ease: "back.out(1.7)",
                scrollTrigger: {
                  trigger: containerRef.current,
                  start: "top 85%",
                  toggleActions: "play none none reverse",
                },
              }
          )

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
        {posts.map((post, index) => (
            <PostCard
                key={post.id}
                post={post}
                index={index}
                animationsReady={animationsReady}
            />
        ))}
      </div>
  )
}

function PostCard({ post, index, animationsReady }: { post: Post; index: number; animationsReady: boolean }) {
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(post.likes)
  const [isExpanded, setIsExpanded] = useState(false)
  const cardRef = useRef<HTMLDivElement>(null)
  const contentRef = useRef<HTMLDivElement>(null)
  const imageRef = useRef<HTMLImageElement>(null)

  const handleLike = async () => {
    const newLiked = !liked
    setLiked(newLiked)
    setLikeCount(newLiked ? likeCount + 1 : likeCount - 1)

    // Heart explosion animation
    if (newLiked && cardRef.current && animationsReady) {
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
                  <AvatarImage src={post.author.avatar || "/placeholder.svg"} alt={post.author.name} />
                  <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white">
                    {post.author.name.substring(0, 2).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div className="absolute -bottom-1 -right-1 h-4 w-4 rounded-full bg-green-500 ring-2 ring-background" />
              </div>
              <div>
                <p className="font-medium transition-colors group-hover:text-violet-600">{post.author.name}</p>
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
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <MoreHorizontal className="h-4 w-4" />
                    <span className="sr-only">Open menu</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="animate-in slide-in-from-top-2">
                  <DropdownMenuItem>Save post</DropdownMenuItem>
                  <DropdownMenuItem>Report</DropdownMenuItem>
                  <DropdownMenuItem>Hide</DropdownMenuItem>
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
              <MessageSquare className="h-3 w-3" />
                {post.comments} comments
            </span>
            </div>

            <Separator className="mb-4" />

            <div className="flex items-center justify-between">
              <Button
                  variant="ghost"
                  size="sm"
                  className={`gap-2 transition-all duration-300 hover:text-rose-500 ${
                      liked ? "text-rose-500 bg-rose-50 hover:bg-rose-100" : "text-muted-foreground hover:text-rose-500"
                  }`}
                  onClick={handleLike}
              >
                <Heart
                    className="heart-icon h-4 w-4 transition-all duration-200"
                    fill={liked ? "currentColor" : "none"}
                />
                <span>Like</span>
              </Button>

              <Button
                  variant="ghost"
                  size="sm"
                  className="gap-2 text-muted-foreground transition-all duration-300 hover:text-blue-500"
              >
                <MessageSquare className="h-4 w-4" />
                <span>Comment</span>
              </Button>

              <Button
                  variant="ghost"
                  size="sm"
                  className="gap-2 text-muted-foreground transition-all duration-300 hover:text-green-500"
              >
                <Share2 className="h-4 w-4" />
                <span>Share</span>
              </Button>
            </div>
          </div>
        </div>
      </div>
  )
}