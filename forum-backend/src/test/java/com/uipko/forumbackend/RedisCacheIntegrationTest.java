package com.uipko.forumbackend;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.services.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // This will use Caffeine cache instead of Redis for testing
class RedisCacheIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testPostCaching() {
        // First call - should hit database
        Instant start1 = Instant.now();
        List<Post> posts1 = postService.getAllPosts();
        Duration duration1 = Duration.between(start1, Instant.now());

        // Second call - should hit cache
        Instant start2 = Instant.now();
        List<Post> posts2 = postService.getAllPosts();
        Duration duration2 = Duration.between(start2, Instant.now());

        // Verify results are the same
        assertEquals(posts1.size(), posts2.size());
        
        // Cache should be significantly faster (although with Caffeine for testing)
        assertTrue(duration2.toMillis() <= duration1.toMillis(), 
            "Cache hit should be faster than database call");
        
        // Verify cache exists
        assertNotNull(cacheManager.getCache("posts"));
        
        System.out.println("Database call took: " + duration1.toMillis() + "ms");
        System.out.println("Cache call took: " + duration2.toMillis() + "ms");
    }

    @Test
    void testCacheNames() {
        // Verify all expected caches are configured
        String[] expectedCaches = {"posts", "users", "friends", "notifications", "online-users", "reactions"};
        
        for (String cacheName : expectedCaches) {
            assertNotNull(cacheManager.getCache(cacheName), 
                "Cache '" + cacheName + "' should be configured");
        }
    }
}