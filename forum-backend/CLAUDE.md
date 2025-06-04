# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Backend Development Guidelines

### Spring Boot Best Practices Protocol
**MANDATORY for all new features, modifications, and architectural decisions:**
- **ALWAYS** consult the Spring Boot best practices documentation using the MCP tool before implementing new features
- Reference `mcp__springboot-best-practices_Docs__fetch_springboot_docs` and `mcp__springboot-best-practices_Docs__search_repo_docs` for guidance
- Ensure all new code follows Spring Boot conventions and industry standards
- Apply security, performance, and maintainability best practices consistently
- Use proper Spring annotations, dependency injection patterns, and configuration management
- Follow repository layer patterns, service layer design, and proper exception handling

### Endpoint Development Protocol

**MANDATORY**: When creating or modifying any API endpoint, follow this strict process:

1. **Test First**: 
   - Write and run tests for the new endpoint BEFORE considering it complete
   - Ensure all edge cases are covered (authentication, authorization, validation)
   - Test with different user roles (USER, MODERATOR, ADMIN)
   - Verify error handling and response codes

2. **Update Postman Collection**:
   - After tests pass, update `forum-api-postman-collection.json`
   - Include examples for each role type if applicable
   - Add both success and error scenarios
   - Document request/response bodies

3. **Update API Documentation**:
   - Update `api-documentation.md` with the new endpoint details
   - Include all parameters, authentication requirements, and response formats
   - Document role-based access rules
   - Add error responses and status codes

### Testing Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserControllerTest
mvn test -Dtest=PostServiceTest

# Run with specific test method
mvn test -Dtest=UserControllerTest#testRegisterUser

# Run tests with debug output
mvn test -X

# Skip tests during build (NOT recommended)
mvn clean package -DskipTests
```

### Security Checklist for New Endpoints

1. **Authentication**: Is `@PreAuthorize` or security config properly set?
2. **Authorization**: Are role-based permissions correctly implemented?
3. **Ownership**: For user-specific resources, verify ownership checks
4. **Validation**: Are all inputs validated with `@Valid` and proper constraints?
5. **Soft Deletion**: If dealing with posts, respect soft deletion visibility rules

### Common Security Annotations

```java
// Require authentication
@PreAuthorize("isAuthenticated()")

// Require specific role
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")

// Custom ownership check
@PreAuthorize("@postOwnershipEvaluator.isOwner(#id, authentication.name) or hasAnyRole('ADMIN', 'MODERATOR')")
```

### Database Migration Protocol

When database changes are needed:
1. Create new Flyway migration in `/src/main/resources/sql/`
2. Follow naming convention: `V{number}__description.sql`
3. Test migration locally before committing
4. Never modify existing migration files

### Exception Handling

All custom exceptions should:
1. Extend appropriate base exception
2. Be handled in `GlobalExceptionHandler`
3. Return consistent error response format
4. Include meaningful error messages

### DTO Guidelines

- Create separate DTOs for requests and responses
- Never expose entity internals directly
- Use validation annotations on request DTOs
- Keep response DTOs focused on client needs

### Service Layer Rules

1. Business logic belongs in services, not controllers
2. Services should handle authorization checks
3. Use interfaces for all services
4. Transaction boundaries should be at service level

### Current Architecture Patterns

- **Authentication**: JWT tokens with Spring Security
- **Authorization**: Role hierarchy (ADMIN > MODERATOR > USER)
- **Soft Deletion**: Posts use `deletedDate` field
- **Reactions**: Separate tables for post/comment reactions
- **Mappers**: Manual mapping between entities and DTOs

### Running the Application

```bash
# Ensure PostgreSQL is running
docker-compose up -d

# Run with default profile
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Debug mode (port 5005)
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Important Files to Update

When adding new features:
- Controllers: `/controllers/`
- Services: `/services/` and `/services/impl/`
- DTOs: `/domain/dto/`
- Entities: `/domain/entities/`
- Exceptions: `/exceptions/`
- Security: `/security/` (if auth changes needed)
- Migrations: `/resources/sql/`
- **ALWAYS**: `api-documentation.md` and `forum-api-postman-collection.json`