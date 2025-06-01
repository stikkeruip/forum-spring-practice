# Forum API Documentation

## Overview
This document provides comprehensive documentation for the Forum API, which is a RESTful API for a forum application with role-based authorization. The API allows users to register, login, create posts, comment on posts, and react to posts and comments.

## Authentication
The API uses JWT (JSON Web Token) for authentication. To access protected endpoints, you need to include the JWT token in the Authorization header of your requests.

### Authentication Flow
1. Register a new user account
2. Login with your credentials to receive a JWT token
3. Include the token in the Authorization header for subsequent requests:
   ```
   Authorization: Bearer <your_jwt_token>
   ```

### Role Hierarchy
The API implements a role hierarchy:
- ADMIN > MODERATOR > USER
- Admins can perform any action in the system
- Moderators can moderate content but cannot access admin-only features
- Regular users can only manage their own content

## Endpoints

### Authentication

#### Register User
- **URL**: `/register`
- **Method**: `POST`
- **Authentication**: Not required
- **Request Body**:
  ```json
  {
    "name": "username",
    "password": "password123"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "name": "username"
  }
  ```
- **Description**: Register a new user account.

#### Login User
- **URL**: `/login`
- **Method**: `POST`
- **Authentication**: Not required
- **Request Body**:
  ```json
  {
    "name": "username",
    "password": "password123"
  }
  ```
- **Response**: `200 OK`
  ```
  eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  ```
- **Description**: Login with existing user credentials and receive a JWT token.

#### Get User Profile
- **URL**: `/profile`
- **Method**: `GET`
- **Authentication**: Required
- **Response**: `200 OK`
  ```json
  {
    "username": "username",
    "createdDate": "2023-01-01T00:00:00",
    "posts": [
      {
        "postId": 1,
        "owner": "username",
        "title": "Post Title",
        "content": "Post content",
        "commentCount": 2,
        "likeCount": 5,
        "dislikeCount": 1,
        "createdDate": "2023-05-15T10:30:00",
        "updatedDate": "2023-05-15T10:30:00",
        "deletedDate": null
      }
    ]
  }
  ```
- **Description**: Get the current user's profile information including their posts.

### Posts

#### Get All Posts
- **URL**: `/posts`
- **Method**: `GET`
- **Authentication**: Optional
- **Response**: `200 OK`
  ```json
  [
    {
      "postId": 1,
      "owner": "username",
      "title": "Post Title",
      "content": "Post content",
      "commentCount": 2,
      "likeCount": 5,
      "dislikeCount": 1,
      "createdDate": "2023-05-15T10:30:00",
      "updatedDate": "2023-05-15T10:30:00",
      "deletedDate": null
    }
  ]
  ```
- **Description**: Get all posts. Results vary based on user role:
  - Admins and moderators see all posts including deleted ones
  - Regular users see all non-deleted posts plus their own deleted posts
  - Unauthenticated users see only non-deleted posts

#### Get Post by ID
- **URL**: `/posts/{id}`
- **Method**: `GET`
- **Authentication**: Optional
- **Path Parameters**:
  - `id`: The ID of the post
- **Response**: `200 OK`
  ```json
  {
    "postId": 1,
    "owner": "username",
    "title": "Post Title",
    "content": "Post content",
    "commentCount": 2,
    "likeCount": 5,
    "dislikeCount": 1,
    "createdDate": "2023-05-15T10:30:00",
    "updatedDate": "2023-05-15T10:30:00",
    "deletedDate": null
  }
  ```
- **Description**: Get a specific post by ID. For deleted posts, only the post owner, admins, and moderators can access them. Other users will receive a 404 Not Found response for deleted posts.

#### Create Post
- **URL**: `/posts`
- **Method**: `POST`
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "title": "Post Title",
    "content": "Post content"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "title": "Post Title",
    "content": "Post content"
  }
  ```
- **Description**: Create a new post.

#### Delete Post
- **URL**: `/posts/{id}`
- **Method**: `DELETE`
- **Authentication**: Required
- **Path Parameters**:
  - `id`: The ID of the post to delete
- **Response**: `204 No Content`
- **Description**: Delete a post. Regular users can only delete their own posts, while moderators and admins can delete any post. If the post is already deleted, the endpoint will return an error.

#### React to Post
- **URL**: `/posts/{id}/reactions`
- **Method**: `POST`
- **Authentication**: Required
- **Path Parameters**:
  - `id`: The ID of the post
- **Request Body**:
  ```json
  {
    "reactionType": "LIKE"
  }
  ```
  Note: `reactionType` can be either `LIKE` or `DISLIKE`
- **Response**: `200 OK`
  ```json
  {
    "postId": 1,
    "owner": "username",
    "title": "Post Title",
    "content": "Post content",
    "commentCount": 2,
    "likeCount": 6,
    "dislikeCount": 1,
    "createdDate": "2023-05-15T10:30:00",
    "updatedDate": "2023-05-15T10:30:00",
    "deletedDate": null
  }
  ```
- **Description**: React to a post with a like or dislike. This endpoint only works with non-deleted posts.

#### Get Deleted Posts
- **URL**: `/posts/deleted`
- **Method**: `GET`
- **Authentication**: Required
- **Response**: `200 OK`
  ```json
  [
    {
      "postId": 1,
      "owner": "username",
      "title": "Deleted Post",
      "content": "This is a deleted post content",
      "commentCount": 0,
      "likeCount": 0,
      "dislikeCount": 0,
      "createdDate": "2023-05-15T10:30:00",
      "updatedDate": "2023-05-15T10:30:00",
      "deletedDate": "2023-05-20T15:45:00"
    }
  ]
  ```
- **Description**: Get all deleted posts. Access is role-based:
  - Admins and moderators can see all deleted posts
  - Regular users can only see their own deleted posts
  - Anonymous users will receive an empty list

#### Get Deleted Post by ID
- **URL**: `/posts/deleted/{id}`
- **Method**: `GET`
- **Authentication**: Required
- **Path Parameters**:
  - `id`: The ID of the deleted post
- **Response**: `200 OK`
  ```json
  {
    "postId": 1,
    "owner": "username",
    "title": "Deleted Post",
    "content": "This is a deleted post content",
    "commentCount": 0,
    "likeCount": 0,
    "dislikeCount": 0,
    "createdDate": "2023-05-15T10:30:00",
    "updatedDate": "2023-05-15T10:30:00",
    "deletedDate": "2023-05-20T15:45:00"
  }
  ```
- **Description**: Get a specific deleted post by ID. Access is role-based:
  - Admins and moderators can see any deleted post
  - Regular users can only see their own deleted posts
  - Anonymous users cannot access deleted posts

### Comments

#### Get Comments by Post
- **URL**: `/posts/{postId}/comments`
- **Method**: `GET`
- **Authentication**: Not required
- **Path Parameters**:
  - `postId`: The ID of the post
- **Response**: `200 OK`
  ```json
  [
    {
      "id": 1,
      "owner": "username",
      "content": "This is a comment on the post",
      "replyCount": 0,
      "likeCount": 2,
      "dislikeCount": 0,
      "createdDate": "2023-05-17T09:45:00",
      "updatedDate": "2023-05-17T09:45:00"
    }
  ]
  ```
- **Description**: Get all comments for a specific post.

#### Get Comment
- **URL**: `/posts/{postId}/comments/{commentId}`
- **Method**: `GET`
- **Authentication**: Not required
- **Path Parameters**:
  - `postId`: The ID of the post
  - `commentId`: The ID of the comment
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "owner": "username",
    "content": "This is a comment on the post",
    "replyCount": 0,
    "likeCount": 2,
    "dislikeCount": 0,
    "createdDate": "2023-05-17T09:45:00",
    "updatedDate": "2023-05-17T09:45:00"
  }
  ```
- **Description**: Get a specific comment for a post.

#### Create Comment
- **URL**: `/posts/{postId}/comments`
- **Method**: `POST`
- **Authentication**: Required
- **Path Parameters**:
  - `postId`: The ID of the post
- **Request Body**:
  ```json
  {
    "content": "This is a comment on the post"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "content": "This is a comment on the post"
  }
  ```
- **Description**: Create a new comment on a post.

#### React to Comment
- **URL**: `/posts/{postId}/comments/{commentId}/reactions`
- **Method**: `POST`
- **Authentication**: Required
- **Path Parameters**:
  - `postId`: The ID of the post
  - `commentId`: The ID of the comment
- **Request Body**:
  ```json
  {
    "reactionType": "LIKE"
  }
  ```
  Note: `reactionType` can be either `LIKE` or `DISLIKE`
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "owner": "username",
    "content": "This is a comment on the post",
    "replyCount": 0,
    "likeCount": 3,
    "dislikeCount": 0,
    "createdDate": "2023-05-17T09:45:00",
    "updatedDate": "2023-05-17T09:45:00"
  }
  ```
- **Description**: React to a comment with a like or dislike.

#### Delete Comment
- **URL**: `/posts/{postId}/comments/{commentId}`
- **Method**: `DELETE`
- **Authentication**: Required
- **Path Parameters**:
  - `postId`: The ID of the post
  - `commentId`: The ID of the comment
- **Response**: `204 No Content`
- **Description**: Delete a comment. Regular users can only delete their own comments, while moderators and admins can delete any comment. This follows the role hierarchy: ADMIN > MODERATOR > USER.
- **Note**: This endpoint is documented but needs to be implemented in the CommentController class.

## Error Handling
The API returns appropriate HTTP status codes for different error scenarios:
- `400 Bad Request`: Invalid request parameters or body
- `401 Unauthorized`: Authentication required but not provided
- `403 Forbidden`: Authentication provided but insufficient permissions
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server-side error

## Data Models

### User
- `name`: String - Username
- `password`: String - User password (only used in request DTOs)
- `createdDate`: LocalDateTime - When the user account was created

### Post
- `postId`: Long - Unique identifier
- `owner`: String - Username of the post creator
- `title`: String - Post title
- `content`: String - Post content
- `commentCount`: Long - Number of comments on the post
- `likeCount`: Long - Number of likes on the post
- `dislikeCount`: Long - Number of dislikes on the post
- `createdDate`: LocalDateTime - When the post was created
- `updatedDate`: LocalDateTime - When the post was last updated
- `deletedDate`: LocalDateTime - When the post was deleted (null if not deleted)

### Comment
- `id`: Long - Unique identifier
- `owner`: String - Username of the comment creator
- `content`: String - Comment content
- `replyCount`: Long - Number of replies to the comment
- `likeCount`: Long - Number of likes on the comment
- `dislikeCount`: Long - Number of dislikes on the comment
- `createdDate`: LocalDateTime - When the comment was created
- `updatedDate`: LocalDateTime - When the comment was last updated

### Reaction
- `reactionType`: String - Type of reaction, either "LIKE" or "DISLIKE"

## Notes
- The API implements soft deletion for posts. Deleted posts are only accessible to the post owner, admins, and moderators.
- The DELETE endpoint for comments is documented but needs to be implemented in the CommentController class.
- The API includes permission checks to ensure proper access control based on the role hierarchy.