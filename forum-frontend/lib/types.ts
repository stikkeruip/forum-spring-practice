export interface Author {
  id: string
  name: string
  avatar: string
}

export interface Post {
  id: string
  title: string
  content: string
  author: Author
  date: string
  likes: number
  dislikes: number
  comments: number
  userReaction?: 'LIKE' | 'DISLIKE' | null
  image?: string
  deletedDate?: string | null
  deletedBy?: string | null
}

export interface ProfilePost {
  postId: number
  owner: string
  title: string
  content: string
  commentCount: number
  likeCount: number
  dislikeCount: number
  userReaction?: 'LIKE' | 'DISLIKE' | null
  createdDate: string
  updatedDate: string
  deletedDate: string | null
  deletedBy: string | null
}

export interface Comment {
  id: number
  content: string
  owner: string
  createdDate: string
  likeCount: number
  dislikeCount: number
  replyCount: number
  parentCommentId?: number | null
  updatedDate?: string
  userReaction?: 'LIKE' | 'DISLIKE' | null
  replies?: Comment[]
}

export interface UserProfile {
  username: string
  createdDate: string
  posts: ProfilePost[]
  deletedPosts: ProfilePost[]
}

export interface OnlineUser {
  username: string
  createdDate: string
  isOnline: boolean
  lastSeen?: string
}

export interface UserStatus {
  username: string
  isOnline: boolean
}

export interface OnlineCount {
  count: number
}

export interface Notification {
  id: number
  actorUsername: string
  type: 'POST_LIKED' | 'COMMENT_LIKED' | 'POST_COMMENTED' | 'COMMENT_REPLIED' | 'POST_DELETED_BY_MODERATOR' | 'COMMENT_DELETED_BY_MODERATOR' | 'POST_RESTORED_BY_MODERATOR' | 'FRIEND_REQUEST_SENT' | 'FRIEND_REQUEST_ACCEPTED' | 'FRIEND_REQUEST_DECLINED'
  targetPostId?: number
  targetCommentId?: number
  message: string
  read: boolean
  createdDate: string
}

export interface NotificationCount {
  count: number
}

// Friend System Types
export interface Friend {
  username: string
  createdDate: string
  isOnline: boolean
  lastSeen: string
  friendshipDate: string
}

export interface PendingFriendRequest {
  friendshipId: number
  requesterUsername: string
  requestDate: string
  isOnline: boolean
}

export interface FriendshipStatus {
  status: 'NONE' | 'PENDING_SENT' | 'PENDING_RECEIVED' | 'ACCEPTED' | 'DECLINED' | 'BLOCKED' | 'SELF'
  friendshipId?: number | null
  canSendRequest: boolean
}
