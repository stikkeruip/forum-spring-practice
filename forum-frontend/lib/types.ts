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
  comments: number
  image?: string
}
