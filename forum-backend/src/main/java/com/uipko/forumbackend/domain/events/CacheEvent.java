package com.uipko.forumbackend.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheEvent {
    private String cacheName;
    private String key;
    private String operation; // HIT, MISS, EVICT, PUT
    private LocalDateTime timestamp;
    private Long executionTime; // in milliseconds
    private String source; // service class name
    
    public static CacheEvent hit(String cacheName, String key, String source) {
        return CacheEvent.builder()
                .cacheName(cacheName)
                .key(key)
                .operation("HIT")
                .timestamp(LocalDateTime.now())
                .source(source)
                .build();
    }
    
    public static CacheEvent miss(String cacheName, String key, String source) {
        return CacheEvent.builder()
                .cacheName(cacheName)
                .key(key)
                .operation("MISS")
                .timestamp(LocalDateTime.now())
                .source(source)
                .build();
    }
    
    public static CacheEvent evict(String cacheName, String key, String source) {
        return CacheEvent.builder()
                .cacheName(cacheName)
                .key(key)
                .operation("EVICT")
                .timestamp(LocalDateTime.now())
                .source(source)
                .build();
    }
}