import type { Post } from "./types"

// API function to restore a deleted post
export async function restorePost(postId: string, token: string): Promise<void> {
  const response = await fetch(`/api/posts/${postId}/restore`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Failed to restore post')
  }
}

// API function to react to a comment
export async function reactToComment(
  postId: string, 
  commentId: number, 
  reactionType: 'LIKE' | 'DISLIKE',
  token: string
): Promise<{
  id: number;
  owner: string;
  content: string;
  replyCount: number;
  likeCount: number;
  dislikeCount: number;
  userReaction: 'LIKE' | 'DISLIKE' | null;
  createdDate: string;
  updatedDate: string;
}> {
  const response = await fetch(`/api/posts/${postId}/comments/${commentId}/reactions`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ reactionType })
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Failed to react to comment')
  }

  return await response.json()
}

export const posts: Post[] = [
  {
    id: "1",
    title: "Introducing our new forum platform",
    content:
      "We're excited to launch our new forum platform! This is a space where our community can connect, share ideas, and help each other. Feel free to explore and start discussions.",
    author: {
      id: "a1",
      name: "Alex Johnson",
      avatar: "/placeholder.svg?height=40&width=40",
    },
    date: "Just now",
    likes: 24,
    dislikes: 2,
    comments: 5,
  },
  {
    id: "2",
    title: "Tips for minimalist design in 2025",
    content:
      "Minimalism continues to evolve in 2025. The key trends this year focus on sustainable materials, functional spaces, and digital decluttering. What are your thoughts on the direction of minimalist design?",
    author: {
      id: "a2",
      name: "Sam Taylor",
      avatar: "/placeholder.svg?height=40&width=40",
    },
    date: "2 hours ago",
    likes: 42,
    dislikes: 3,
    comments: 13,
    image: "/placeholder.svg?height=400&width=600",
  },
  {
    id: "3",
    title: "Weekly discussion: What are you working on?",
    content:
      "Share your current projects, challenges, and wins! This is a great place to get feedback from the community or just share what you've been up to this week.",
    author: {
      id: "a3",
      name: "Jamie Rivera",
      avatar: "/placeholder.svg?height=40&width=40",
    },
    date: "Yesterday",
    likes: 18,
    dislikes: 1,
    comments: 27,
  },
  {
    id: "4",
    title: "Resources for learning web development in 2025",
    content:
      "I've compiled a list of the most helpful resources I've found for learning modern web development. From interactive courses to documentation and communities, these have been invaluable on my journey.",
    author: {
      id: "a4",
      name: "Morgan Chen",
      avatar: "/placeholder.svg?height=40&width=40",
    },
    date: "3 days ago",
    likes: 86,
    dislikes: 5,
    comments: 31,
  },
]
