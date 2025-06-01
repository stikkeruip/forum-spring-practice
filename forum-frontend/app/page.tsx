import ForumLayout from "@/components/forum-layout"
import PostList from "@/components/post-list"
import { posts } from "@/lib/data"

export default function Home() {
  return (
    <ForumLayout>
      <PostList posts={posts} />
    </ForumLayout>
  )
}
