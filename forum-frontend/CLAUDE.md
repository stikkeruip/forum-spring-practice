# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Frontend Development Guidelines

### Component Development Protocol

**MANDATORY**: When creating new components or features:

1. **Check Existing UI Components First**:
   - Always check `/components/ui/` for existing Radix UI components
   - Use existing components rather than creating new ones
   - Follow the established patterns in existing components

2. **API Integration Pattern**:
   - **NEVER USE MOCK DATA** - Always use real API endpoints
   - Handle API failures gracefully with proper error states
   - All API calls should go through the `/api` proxy
   - Use loading states while fetching data
   - Show appropriate error messages when API calls fail

3. **Type Safety**:
   - Define TypeScript interfaces in `/lib/types.ts`
   - Never use `any` type
   - Ensure all props and API responses are properly typed

### Development Commands

```bash
# Start development server (port 3000)
npm run dev

# Build for production
npm run build

# Run production build locally
npm run start

# Type checking (if configured)
npm run type-check

# Linting
npm run lint

# Format code (if configured)
npm run format
```

### Component Guidelines

**UI Component Usage**:
```tsx
// Always import from the ui folder
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Dialog } from "@/components/ui/dialog"

// NOT like this
import { Button } from "@radix-ui/react-button" // Wrong!
```

**Component Structure**:
```tsx
// Follow this pattern for new components
export function ComponentName({ prop1, prop2 }: ComponentNameProps) {
  // Hooks first
  const [state, setState] = useState()
  
  // Effects second
  useEffect(() => {}, [])
  
  // Handlers third
  const handleClick = () => {}
  
  // Render last
  return <div>...</div>
}
```

### API Integration Examples

**Correct Pattern** (no mock data):
```tsx
async function fetchPosts() {
  try {
    const response = await fetch('/api/posts')
    if (!response.ok) throw new Error(`API error: ${response.status}`)
    return await response.json()
  } catch (error) {
    console.error('Failed to fetch posts:', error)
    throw error // Let the component handle the error state
  }
}

// In component:
const [posts, setPosts] = useState([])
const [loading, setLoading] = useState(false)
const [error, setError] = useState(null)

useEffect(() => {
  const loadPosts = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await fetchPosts()
      setPosts(data)
    } catch (err) {
      setError('Failed to load posts. Please try again.')
    } finally {
      setLoading(false)
    }
  }
  loadPosts()
}, [])
```

**Authentication Headers**:
```tsx
const token = localStorage.getItem('token')
const response = await fetch('/api/posts', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
```

### Form Handling

Always use React Hook Form with Zod validation:
```tsx
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"

const formSchema = z.object({
  title: z.string().min(1, "Title is required"),
  content: z.string().min(1, "Content is required")
})

const form = useForm({
  resolver: zodResolver(formSchema),
  defaultValues: { title: "", content: "" }
})
```

### Theme and Styling

1. **Dark Mode**: Already configured with next-themes
2. **Styling Priority**:
   - Use Tailwind CSS classes
   - Use CSS variables from globals.css
   - Never use inline styles unless absolutely necessary
   - Follow existing color scheme and spacing

### State Management

1. **Local State**: Use React hooks (useState, useReducer)
2. **Global State**: Use React Context (see theme-provider.tsx pattern)
3. **Server State**: Consider React Query or SWR for caching (if needed)

### Common Patterns

**Error Handling**:
```tsx
const [error, setError] = useState<string | null>(null)
const [loading, setLoading] = useState(false)

try {
  setLoading(true)
  // ... async operation
} catch (err) {
  setError(err.message)
} finally {
  setLoading(false)
}
```

**Toast Notifications**:
```tsx
import { useToast } from "@/hooks/use-toast"

const { toast } = useToast()
toast({
  title: "Success",
  description: "Post created successfully"
})
```

### File Organization

```
/app              - Next.js 13+ app directory
/components       - React components
  /ui            - Reusable UI components (Radix UI)
  *.tsx          - Feature components
/lib             - Utilities and types
  /data.ts       - Mock data
  /types.ts      - TypeScript interfaces
  /utils.ts      - Helper functions
/hooks           - Custom React hooks
/public          - Static assets
```

### Important Considerations

1. **API Proxy**: All `/api/*` requests are proxied to `http://localhost:8080`
2. **Image Optimization**: Currently disabled in next.config.mjs
3. **Build Errors**: ESLint and TypeScript errors are ignored during build
4. **NO MOCK DATA**: Never use mock data - always use real API endpoints with proper error handling

### Progressive Enhancement

When adding new features:
1. Start with real API integration from the beginning
2. Add loading states
3. Add comprehensive error handling
4. Test with backend running
5. Add optimistic updates (if applicable)

### Security Best Practices

1. **Never store sensitive data in localStorage** (except auth tokens)
2. **Validate all user inputs** on the frontend
3. **Sanitize any user-generated content** before rendering
4. **Use HTTPS in production** for all API calls

### Testing Approach

While no testing framework is currently configured:
1. Manually test all user flows
2. Test with different user roles
3. Test error scenarios
4. Test on different screen sizes
5. Test dark/light mode compatibility

### Current Features Status

- **Forum Layout**: Base layout with navigation
- **Post List**: Displays posts from real API
- **Progressive Auth**: Authentication flow with JWT tokens
- **Notifications**: Real-time notification system via WebSocket
- **Theme Support**: Dark/light mode switching
- **Responsive Design**: Mobile-first approach

### When Adding New Features

1. Check if UI components exist in `/components/ui/`
2. Add TypeScript types to `/lib/types.ts`
3. **NEVER add mock data** - integrate with real API from start
4. Follow existing component patterns
5. Ensure mobile responsiveness
6. Test with backend running and proper error handling