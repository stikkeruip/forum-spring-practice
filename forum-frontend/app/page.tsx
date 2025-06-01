import ForumLayout from "@/components/forum-layout"
import PostList from "@/components/post-list"
import { Post } from "@/lib/types"
import { posts as mockPosts } from "@/lib/data"

async function getPosts() {
  try {
    // Create an AbortController with a timeout
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5 second timeout

    const response = await fetch('http://localhost:8080/posts', { 
      cache: 'no-store',
      signal: controller.signal
    });

    // Clear the timeout
    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error('Failed to fetch posts');
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
      comments: post.commentCount,
    }));
  } catch (error) {
    console.error('Error fetching posts:', error);

    // Provide specific error messages based on the error type
    if (error.name === 'AbortError') {
      console.error('Request timed out: The server took too long to respond');
    } else if (error instanceof TypeError && error.message.includes('fetch failed')) {
      console.error('Network error: Make sure the backend server is running at http://localhost:8080');
    }

    console.log('Falling back to mock data...');
    // Return mock data as fallback
    return mockPosts;
  }
}

export default async function Home() {
  // Track if we're using mock data
  let usingMockData = false;

  // Get posts with fallback to mock data
  const posts: Post[] = await getPosts().then(result => {
    // If the result exactly matches mockPosts, we're using mock data
    usingMockData = result === mockPosts;
    return result;
  });

  return (
    <ForumLayout>
      {usingMockData && (
        <div className="bg-yellow-100 border-l-4 border-yellow-500 text-yellow-700 p-4 mb-4" role="alert">
          <p className="font-bold">Note</p>
          <p>Unable to connect to the backend server. Showing mock data instead.</p>
        </div>
      )}
      <PostList posts={posts} />
    </ForumLayout>
  )
}
