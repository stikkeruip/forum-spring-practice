# Role-Based Authorization in Forum Backend

This document explains the role-based authorization system implemented in the Forum Backend application.

## Role Hierarchy

The application uses a role hierarchy where higher roles inherit permissions from lower roles:

```
ROLE_ADMIN > ROLE_MODERATOR > ROLE_USER
```

This means:
- Users with the `ADMIN` role can do everything that `MODERATOR` and `USER` roles can do
- Users with the `MODERATOR` role can do everything that `USER` role can do
- Users with the `USER` role have basic permissions

## Available Roles

1. **USER** - Basic role assigned to all registered users
2. **MODERATOR** - Can moderate content (e.g., delete any comment)
3. **ADMIN** - Has full access to all features

## Implementation Details

### Role Storage

Roles are stored in the `role` column of the `users` table. The column has a default value of `USER`.

### Role Loading

When a user logs in, their role is loaded by the `CustomUserDetailsService` and converted to a Spring Security `GrantedAuthority` with the `ROLE_` prefix.

### Authorization Methods

The application uses several methods for authorization:

1. **URL-based authorization** in `SecurityConfig.securityFilterChain`
   - Public endpoints: `/register`, `/login`, `/posts`, etc.
   - Admin-only endpoints: `/admin/**`

2. **Method-level security** using annotations:
   ```java
   @PreAuthorize("hasRole('ADMIN') or @postOwnershipEvaluator.isOwner(authentication, #id)")
   public void deletePost(Long id) {
       // Method implementation
   }
   ```

3. **Custom permission evaluators** for ownership checks:
   - `PostOwnershipEvaluator` - Checks if a user owns a post
   - `CommentOwnershipEvaluator` - Checks if a user owns a comment

## How to Use

### Checking Roles in Controllers/Services

Use method security annotations:

```java
// Only admins can access this method
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() {
    // Method implementation
}

// Admins or moderators can access this method
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public void moderationMethod() {
    // Method implementation
}

// Only the owner of the resource or an admin can access this method
@PreAuthorize("hasRole('ADMIN') or @postOwnershipEvaluator.isOwner(authentication, #id)")
public void ownerOrAdminMethod(Long id) {
    // Method implementation
}
```

### Adding New Roles

To add a new role:

1. Update the role hierarchy in `SecurityConfig.roleHierarchy`
2. Add helper methods to the `User` entity
3. Update any relevant authorization rules

## Testing

When testing, you can use the following predefined users:

- `admin` - Has the `ADMIN` role
- `moderator` - Has the `MODERATOR` role
- Any other user - Has the `USER` role by default