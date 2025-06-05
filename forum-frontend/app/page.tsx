"use client"

import { useState, useEffect } from "react"
import ForumLayout from "@/components/forum-layout"
import PostList from "@/components/post-list"
import { useAuth } from "@/components/auth-provider"
import { Post } from "@/lib/types"

// Disable static generation for this page since it uses authentication
export const dynamic = 'force-dynamic'

async function getPosts(token?: string | null): Promise<Post[]> {
  try {
    // Create an AbortController with a timeout
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5 second timeout

    // Include authentication headers if token is available
    const headers: HeadersInit = {
      'Cache-Control': 'no-store'
    };
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch('/api/posts', { 
      cache: 'no-store',
      signal: controller.signal,
      headers
    });

    // Clear the timeout
    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`Failed to fetch posts: ${response.status} ${response.statusText}`);
    }
    const data = await response.json();

    // Map API response to match the Post type
    return data.map((post: any) => ({
      id: post.postId.toString(),
      title: post.title,
      content: post.content,
      author: {
        id: post.owner,
        name: post.owner,
        avatar: "/placeholder.svg?height=40&width=40",
      },
      date: new Date(post.createdDate).toLocaleDateString(),
      likes: post.likeCount,
      dislikes: post.dislikeCount,
      comments: post.commentCount,
      userReaction: post.userReaction, // Map the user's reaction state
      deletedDate: post.deletedDate,
      deletedBy: post.deletedBy,
    }));
  } catch (error) {
    console.error('Error fetching posts:', error);

    // Provide specific error messages based on the error type
    if (error.name === 'AbortError') {
      console.error('Request timed out: The server took too long to respond');
    } else if (error instanceof TypeError && error.message.includes('fetch failed')) {
      console.error('Network error: Make sure the backend server is running at http://localhost:8080');
    }

    // Return empty array instead of mock data
    return [];
  }
}

export default function Home() {
  const { token, isLoading: authLoading } = useAuth()
  const [isCreating, setIsCreating] = useState(false)
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Load posts after auth finishes loading
  useEffect(() => {
    // Don't fetch posts until auth has finished loading
    if (authLoading) return
    
    setLoading(true)
    getPosts(token).then(result => {
      // Sort posts by date (newest first)
      const sortedPosts = result.sort((a, b) => {
        const dateA = new Date(a.date)
        const dateB = new Date(b.date)
        return dateB.getTime() - dateA.getTime()
      })
      
      setPosts(sortedPosts)
      setError(null)
    }).catch(err => {
      setError('Failed to load posts. Please check your connection and try again.')
      console.error('Error loading posts:', err)
    }).finally(() => {
      setLoading(false)
    })
  }, [token, authLoading])

  // Handle new post creation
  const handlePostCreated = (newPost: Post) => {
    setPosts(prevPosts => [newPost, ...prevPosts])
  }

  // Handle post deletion
  const handlePostDeleted = (postId: string) => {
    setPosts(prevPosts => prevPosts.filter(post => post.id !== postId))
  }


  return (
    <ForumLayout isCreating={isCreating} setIsCreating={setIsCreating}>
      {loading && (
        <div className="flex justify-center items-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-violet-600"></div>
          <span className="ml-2 text-muted-foreground">Loading posts...</span>
        </div>
      )}
      
      {error && (
        <div className="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-4" role="alert">
          <p className="font-bold">Error</p>
          <p>{error}</p>
        </div>
      )}
      
      {!loading && !error && (
        <PostList posts={posts} isCreating={isCreating} setIsCreating={setIsCreating} onPostCreated={handlePostCreated} onPostDeleted={handlePostDeleted} />
      )}
    </ForumLayout>
  )
}
