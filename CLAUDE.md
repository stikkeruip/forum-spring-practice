# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Versions
- **Backend**: Spring Boot v3.4.2
- **Java**: v17
- **Database**: PostgreSQL (latest via Docker)
- **Frontend**: Next.js v15.3.3
- **React**: v18
- **TypeScript**: v5
- **Additional Tools**: 
  - Flyway v11.3.1 (database migrations)
  - JWT v0.12.6 (authentication)
  - Tailwind CSS v3.4.17
  - Radix UI components
  - Maven (backend build)
  - Docker Compose (database)

*Last updated: 6/4/2025*

## 🚨 CRITICAL POLICY: NO MOCK DATA

**NEVER USE MOCK DATA IN ANY PART OF THE APPLICATION**

- Always use real API endpoints from the backend
- Handle API failures gracefully with proper error states and user feedback
- Test all features with the backend running
- If the backend is not available, show appropriate loading/error states
- This policy ensures production-like behavior during development

## Common Development Commands

### Backend (Spring Boot)
```bash
# Start PostgreSQL database
cd forum-backend
docker-compose up -d

# Run the application
mvn spring-boot:run

# Build the project
mvn clean package

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=YourTestClassName

# Run tests with specific profile
mvn test -Dspring.profiles.active=test

# Clean build artifacts
mvn clean
```

### Frontend (Next.js)
```bash
cd forum-frontend

# Install dependencies
npm install
# or
pnpm install

# Run development server
npm run dev

# Build production version
npm run build

# Start production server
npm start

# Run linting
npm run lint
```

## Architecture Overview

### Backend Architecture
The Spring Boot backend follows a layered architecture:

1. **Controllers** (`/controllers`): REST API endpoints with role-based security
   - `UserController`: Authentication and user profile management
   - `PostController`: CRUD operations for posts with soft deletion
   - `CommentController`: Comment management and reactions

2. **Services** (`/services`): Business logic layer
   - Each service has an interface and implementation
   - Handles authorization checks and business rules
   - Integrates with JWT for authentication

3. **Security** (`/security`):
   - `JwtAuthenticationFilter`: Validates JWT tokens on each request
   - `SecurityConfig`: Configures Spring Security with JWT integration
   - Ownership evaluators for fine-grained access control
   - Role hierarchy: ADMIN > MODERATOR > USER

4. **Domain** (`/domain`):
   - **Entities**: JPA entities with relationships (User, Post, Comment, Reactions)
   - **DTOs**: Request/Response objects for API communication
   - **Mappers**: Manual mappers for entity-DTO conversion

5. **Database**:
   - PostgreSQL with Flyway migrations (10 migration files)
   - Soft deletion for posts with visibility rules
   - Reaction tables for likes/dislikes

### Frontend Architecture
The Next.js frontend uses modern React patterns:

1. **API Integration**: 
   - Configured to proxy `/api/*` requests to `http://localhost:8080`
   - Supports both mock data and real backend integration

2. **Component Structure**:
   - Extensive Radix UI component library in `/components/ui`
   - Feature components for forum functionality
   - Theme provider for dark mode support

3. **State Management**:
   - React hooks and context for state
   - Form handling with React Hook Form and Zod validation

## Security Considerations

1. **Authentication**: JWT-based with secure token generation
2. **Authorization**: Role-based (USER, MODERATOR, ADMIN) with ownership checks
3. **Password**: BCrypt encoding for password storage
4. **CORS**: Configured for frontend integration
5. **Soft Deletion**: Posts can be soft-deleted with role-based visibility

## API Testing
- Postman collection available: `forum-backend/forum-api-postman-collection.json`
- Includes examples for all endpoints with different user roles
- Test users are created via Flyway migrations

## Database Setup
The project uses PostgreSQL with the following configuration:
- Database name: `springforum`
- Username: `uipko`
- Password: `uipko`
- Port: `5432`

Flyway migrations automatically create and update the schema on application startup.

## Current Development Status
Based on git status, active development includes:
- User controller modifications
- Forum layout component updates  
- Progressive authentication component (newly added)

## Key Implementation Details

1. **Soft Deletion Logic**: 
   - Deleted posts remain in database with `deletedDate` timestamp
   - Visibility rules: Admins/Moderators see all, users see own deleted posts
   - Deleted posts cannot be reacted to or commented on

2. **Reaction System**:
   - Separate tables for post and comment reactions
   - One reaction per user per content item
   - Supports LIKE and DISLIKE types

3. **Comment Nesting**:
   - Comments support parent-child relationships
   - Reply count tracking for UI display

4. **API Integration Policy**:
   - **NEVER USE MOCK DATA** - Always use real API endpoints
   - Handle API failures gracefully with proper error states
   - All development should be done with backend running