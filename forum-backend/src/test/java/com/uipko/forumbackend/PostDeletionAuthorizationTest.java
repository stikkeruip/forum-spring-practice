package com.uipko.forumbackend;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PostDeletionAuthorizationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User regularUser;
    private User postOwner;
    private Post testPost;

    @BeforeEach
    public void setUp() {
        // Clear security context
        SecurityContextHolder.clearContext();

        // Create users
        adminUser = new User();
        adminUser.setName("testadmin");
        adminUser.setPassword("password");
        adminUser.setCreatedDate(LocalDateTime.now());
        adminUser.setRole("ADMIN");
        userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setName("testuser");
        regularUser.setPassword("password");
        regularUser.setCreatedDate(LocalDateTime.now());
        regularUser.setRole("USER");
        userRepository.save(regularUser);

        postOwner = new User();
        postOwner.setName("postowner");
        postOwner.setPassword("password");
        postOwner.setCreatedDate(LocalDateTime.now());
        postOwner.setRole("USER");
        userRepository.save(postOwner);

        // Create a post owned by postOwner
        testPost = new Post();
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setUser(postOwner);
        testPost.setCreatedDate(LocalDateTime.now());
        testPost = postRepository.save(testPost);
    }

    @Test
    public void testAdminCanDeleteAnyPost() {
        // Authenticate as admin
        authenticateAs(adminUser);

        // Admin should be able to delete the post
        assertDoesNotThrow(() -> postService.deletePost(testPost.getId()));

        // Verify post is soft deleted
        Post deletedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assertNotNull(deletedPost.getDeletedDate());
    }

    @Test
    public void testOwnerCanDeleteOwnPost() {
        // Authenticate as post owner
        authenticateAs(postOwner);

        // Owner should be able to delete their own post
        assertDoesNotThrow(() -> postService.deletePost(testPost.getId()));

        // Verify post is soft deleted
        Post deletedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assertNotNull(deletedPost.getDeletedDate());
    }

    @Test
    public void testRegularUserCannotDeleteOthersPost() {
        // Authenticate as regular user (not the owner)
        authenticateAs(regularUser);

        // Regular user should not be able to delete someone else's post
        assertThrows(AccessDeniedException.class, () -> postService.deletePost(testPost.getId()));

        // Verify post is not deleted
        Post notDeletedPost = postRepository.findById(testPost.getId()).orElseThrow();
        assertNull(notDeletedPost.getDeletedDate());
    }

    private void authenticateAs(User user) {
        // Create authentication token with proper authorities
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user.getName(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}