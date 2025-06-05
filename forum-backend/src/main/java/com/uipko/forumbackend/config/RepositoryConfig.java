package com.uipko.forumbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Repository configuration to separate JPA and Redis repository scanning
 * This prevents Spring Data Redis from trying to scan JPA repositories
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.uipko.forumbackend.repositories")
@EnableRedisRepositories(basePackages = "com.uipko.forumbackend.redis.repositories")
public class RepositoryConfig {
    // Configuration class to explicitly separate JPA and Redis repository scanning
}