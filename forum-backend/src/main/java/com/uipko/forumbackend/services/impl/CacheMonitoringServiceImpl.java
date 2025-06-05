package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CacheMonitoringServiceImpl implements CacheMonitoringService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthenticationCacheService authCacheService;
    private final RealTimeDataCacheService realTimeCacheService;
    private final SessionManagementService sessionManagementService;
    private final CacheInvalidationService cacheInvalidationService;
    private final CacheWarmingService cacheWarmingService;
    private final MultiLevelCacheService multiLevelCacheService;
    
    public CacheMonitoringServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                    AuthenticationCacheService authCacheService,
                                    RealTimeDataCacheService realTimeCacheService,
                                    SessionManagementService sessionManagementService,
                                    CacheInvalidationService cacheInvalidationService,
                                    CacheWarmingService cacheWarmingService,
                                    MultiLevelCacheService multiLevelCacheService) {
        this.redisTemplate = redisTemplate;
        this.authCacheService = authCacheService;
        this.realTimeCacheService = realTimeCacheService;
        this.sessionManagementService = sessionManagementService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.cacheWarmingService = cacheWarmingService;
        this.multiLevelCacheService = multiLevelCacheService;
    }
    
    @Override
    public CacheHealthStatus getCacheHealthStatus() {
        log.debug("Generating cache health status");
        
        List<String> issues = new ArrayList<>();
        double overallScore = 100.0;
        
        // Check Redis health
        RedisHealthStatus redisHealth = getRedisHealth();
        if (!redisHealth.connected()) {
            issues.add("Redis connection failed");
            overallScore -= 40;
        }
        if (redisHealth.memoryUsagePercent() > 90) {
            issues.add("Redis memory usage is high: " + redisHealth.memoryUsagePercent() + "%");
            overallScore -= 20;
        }
        if (redisHealth.hitRate() < 0.7) {
            issues.add("Redis hit rate is low: " + (redisHealth.hitRate() * 100) + "%");
            overallScore -= 15;
        }
        
        // Check local cache health
        LocalCacheHealthStatus localCacheHealth = getLocalCacheHealth();
        if (localCacheHealth.usagePercent() > 95) {
            issues.add("Local cache usage is high: " + localCacheHealth.usagePercent() + "%");
            overallScore -= 10;
        }
        if (localCacheHealth.hitRate() < 0.8) {
            issues.add("Local cache hit rate is low: " + (localCacheHealth.hitRate() * 100) + "%");
            overallScore -= 10;
        }
        
        // Check session health
        SessionCacheHealthStatus sessionHealth = getSessionHealth();
        if (sessionHealth.sessionTimeouts() > 100) {
            issues.add("High number of session timeouts: " + sessionHealth.sessionTimeouts());
            overallScore -= 5;
        }
        
        String status = overallScore >= 90 ? "HEALTHY" : 
                       overallScore >= 70 ? "DEGRADED" : "CRITICAL";
        
        boolean healthy = overallScore >= 70;
        
        return new CacheHealthStatus(
            healthy, status, issues, overallScore,
            redisHealth, localCacheHealth, sessionHealth
        );
    }
    
    private RedisHealthStatus getRedisHealth() {
        try {
            // Get Redis info
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            
            boolean connected = true;
            String version = info.getProperty("redis_version", "unknown");
            long memoryUsed = Long.parseLong(info.getProperty("used_memory", "0"));
            long memoryMax = Long.parseLong(info.getProperty("maxmemory", "0"));
            double memoryUsagePercent = memoryMax > 0 ? (double) memoryUsed / memoryMax * 100 : 0;
            long connectedClients = Long.parseLong(info.getProperty("connected_clients", "0"));
            
            // Calculate hit rate
            long keyspaceHits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long keyspaceMisses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            double hitRate = (keyspaceHits + keyspaceMisses) > 0 ? 
                (double) keyspaceHits / (keyspaceHits + keyspaceMisses) : 0;
            
            long totalCommands = Long.parseLong(info.getProperty("total_commands_processed", "0"));
            String role = info.getProperty("role", "unknown");
            
            return new RedisHealthStatus(
                connected, version, memoryUsed, memoryMax, memoryUsagePercent,
                connectedClients, hitRate, totalCommands, role
            );
            
        } catch (Exception e) {
            log.error("Failed to get Redis health status", e);
            return new RedisHealthStatus(
                false, "unknown", 0, 0, 0, 0, 0, 0, "unknown"
            );
        }
    }
    
    private LocalCacheHealthStatus getLocalCacheHealth() {
        try {
            MultiLevelCacheService.MultiLevelCacheStats stats = multiLevelCacheService.getStats();
            
            // Estimate max size (would need configuration access)
            long maxSize = 10000; // Default value
            double usagePercent = maxSize > 0 ? (double) stats.l1Size() / maxSize * 100 : 0;
            
            return new LocalCacheHealthStatus(
                stats.l1Size(),
                maxSize,
                usagePercent,
                stats.l1HitRate(),
                stats.evictions(),
                0.0 // Average load time would need additional tracking
            );
            
        } catch (Exception e) {
            log.error("Failed to get local cache health status", e);
            return new LocalCacheHealthStatus(0, 0, 0, 0, 0, 0);
        }
    }
    
    private SessionCacheHealthStatus getSessionHealth() {
        try {
            // This would need implementation in SessionManagementService
            return new SessionCacheHealthStatus(
                0, 0, 0.0, "30min", 0, 0
            );
        } catch (Exception e) {
            log.error("Failed to get session health status", e);
            return new SessionCacheHealthStatus(0, 0, 0, "0min", 0, 0);
        }
    }
    
    @Override
    public CachePerformanceMetrics getPerformanceMetrics() {
        // This would need implementation with actual performance tracking
        return new CachePerformanceMetrics(
            5.0, 10.0, 25.0, 1000, 0.01,
            Map.of("GET", 2.0, "PUT", 3.0, "EVICT", 1.0)
        );
    }
    
    @Override
    public CacheUsageStats getUsageStats() {
        try {
            MultiLevelCacheService.MultiLevelCacheStats multiStats = multiLevelCacheService.getStats();
            AuthenticationCacheService.AuthCacheStats authStats = authCacheService.getCacheStats();
            RealTimeDataCacheService.RealTimeCacheStats realTimeStats = realTimeCacheService.getCacheStats();
            
            long totalRequests = multiStats.totalRequests();
            long totalHits = multiStats.l1Hits() + multiStats.l2Hits();
            long totalMisses = multiStats.l1Misses() + multiStats.l2Misses();
            double overallHitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0;
            
            Map<String, Long> cacheTypeUsage = Map.of(
                "authentication", authStats.totalCacheRequests(),
                "realtime", realTimeStats.totalActivitiesCached(),
                "multilevel", totalRequests
            );
            
            Map<String, Double> cacheTypeHitRates = Map.of(
                "authentication", authStats.cacheHitRate(),
                "realtime", 85.0, // Would need actual implementation
                "multilevel", overallHitRate
            );
            
            long totalCacheSize = multiStats.l1Size() + authStats.userDetailsCacheSize() + 
                                realTimeStats.onlineUsersCount();
            
            Map<String, Long> cacheSizeByType = Map.of(
                "l1", multiStats.l1Size(),
                "auth", authStats.userDetailsCacheSize(),
                "realtime", realTimeStats.onlineUsersCount()
            );
            
            return new CacheUsageStats(
                totalRequests, totalHits, totalMisses, overallHitRate,
                cacheTypeUsage, cacheTypeHitRates, totalCacheSize, cacheSizeByType
            );
            
        } catch (Exception e) {
            log.error("Failed to get cache usage stats", e);
            return new CacheUsageStats(0, 0, 0, 0, Map.of(), Map.of(), 0, Map.of());
        }
    }
    
    @Override
    public CacheEfficiencyMetrics getEfficiencyMetrics() {
        // This would need more sophisticated analysis
        return new CacheEfficiencyMetrics(
            85.0, 90.0, 80.0, 50, 1000,
            List.of("user:admin", "post:recent", "notifications:user1"),
            List.of("cache:unused1", "cache:unused2")
        );
    }
    
    @Override
    public RedisMonitoringData getRedisMonitoringData() {
        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            
            Map<String, String> infoMap = info.entrySet().stream()
                .collect(Collectors.toMap(
                    e -> e.getKey().toString(),
                    e -> e.getValue().toString()
                ));
            
            Map<String, Long> stats = Map.of(
                "total_connections_received", Long.parseLong(info.getProperty("total_connections_received", "0")),
                "total_commands_processed", Long.parseLong(info.getProperty("total_commands_processed", "0")),
                "keyspace_hits", Long.parseLong(info.getProperty("keyspace_hits", "0")),
                "keyspace_misses", Long.parseLong(info.getProperty("keyspace_misses", "0"))
            );
            
            return new RedisMonitoringData(
                infoMap, stats, List.of(), Map.of(),
                stats.get("total_connections_received"),
                stats.get("total_commands_processed"),
                Double.parseDouble(info.getProperty("instantaneous_ops_per_sec", "0"))
            );
            
        } catch (Exception e) {
            log.error("Failed to get Redis monitoring data", e);
            return new RedisMonitoringData(Map.of(), Map.of(), List.of(), Map.of(), 0, 0, 0);
        }
    }
    
    @Override
    public SessionManagementStats getSessionStats() {
        // This would need implementation in SessionManagementService
        return new SessionManagementStats(0, 0, 0, 0, 0, Map.of(), 0);
    }
    
    @Override
    public RealTimeCacheMetrics getRealTimeMetrics() {
        try {
            RealTimeDataCacheService.RealTimeCacheStats stats = realTimeCacheService.getCacheStats();
            
            return new RealTimeCacheMetrics(
                stats.onlineUsersCount(),
                stats.totalNotificationsCached(),
                stats.recentPostsCount(),
                stats.totalReactionsCached(),
                0.5, // Cache refresh rate
                System.currentTimeMillis(),
                Map.of(
                    "online_users", stats.onlineUsersCount(),
                    "notifications", stats.totalNotificationsCached(),
                    "posts", stats.recentPostsCount(),
                    "reactions", stats.totalReactionsCached()
                )
            );
            
        } catch (Exception e) {
            log.error("Failed to get real-time cache metrics", e);
            return new RealTimeCacheMetrics(0, 0, 0, 0, 0, 0, Map.of());
        }
    }
    
    @Override
    public List<CacheOptimizationRecommendation> getOptimizationRecommendations() {
        List<CacheOptimizationRecommendation> recommendations = new ArrayList<>();
        
        try {
            CacheHealthStatus health = getCacheHealthStatus();
            
            if (health.redisHealth().memoryUsagePercent() > 80) {
                recommendations.add(new CacheOptimizationRecommendation(
                    "MEMORY", "HIGH",
                    "High Redis Memory Usage",
                    "Redis memory usage is above 80%. Consider increasing TTL for less critical data or adding more memory.",
                    "Adjust cache TTL configuration or scale Redis instance",
                    15.0
                ));
            }
            
            if (health.redisHealth().hitRate() < 0.8) {
                recommendations.add(new CacheOptimizationRecommendation(
                    "PERFORMANCE", "MEDIUM",
                    "Low Cache Hit Rate",
                    "Cache hit rate is below 80%. Review caching strategy and warm-up procedures.",
                    "Implement better cache warming and review cache key strategies",
                    20.0
                ));
            }
            
            if (health.localCacheHealth().usagePercent() > 90) {
                recommendations.add(new CacheOptimizationRecommendation(
                    "CONFIGURATION", "MEDIUM",
                    "Local Cache Near Capacity",
                    "Local cache is near maximum capacity. Consider increasing size or improving eviction strategy.",
                    "Increase local cache size or optimize cache usage patterns",
                    10.0
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to generate optimization recommendations", e);
        }
        
        return recommendations;
    }
    
    @Override
    public Map<String, Object> exportMetricsForPrometheus() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            CacheHealthStatus health = getCacheHealthStatus();
            CacheUsageStats usage = getUsageStats();
            
            metrics.put("cache_health_score", health.overallScore());
            metrics.put("cache_redis_connected", health.redisHealth().connected() ? 1 : 0);
            metrics.put("cache_redis_memory_usage_percent", health.redisHealth().memoryUsagePercent());
            metrics.put("cache_redis_hit_rate", health.redisHealth().hitRate());
            metrics.put("cache_total_requests", usage.totalRequests());
            metrics.put("cache_total_hits", usage.totalHits());
            metrics.put("cache_total_misses", usage.totalMisses());
            metrics.put("cache_overall_hit_rate", usage.overallHitRate());
            metrics.put("cache_total_size", usage.totalCacheSize());
            
        } catch (Exception e) {
            log.error("Failed to export metrics for Prometheus", e);
        }
        
        return metrics;
    }
    
    @Override
    public List<CacheAlert> getCacheAlerts() {
        List<CacheAlert> alerts = new ArrayList<>();
        
        try {
            CacheHealthStatus health = getCacheHealthStatus();
            
            if (!health.redisHealth().connected()) {
                alerts.add(new CacheAlert(
                    "ERROR", "REDIS",
                    "Redis connection failed",
                    LocalDateTime.now().toString(),
                    Map.of("component", "redis", "action", "check_connection")
                ));
            }
            
            if (health.redisHealth().memoryUsagePercent() > 95) {
                alerts.add(new CacheAlert(
                    "ERROR", "REDIS",
                    "Redis memory usage critical: " + health.redisHealth().memoryUsagePercent() + "%",
                    LocalDateTime.now().toString(),
                    Map.of("usage_percent", health.redisHealth().memoryUsagePercent())
                ));
            } else if (health.redisHealth().memoryUsagePercent() > 85) {
                alerts.add(new CacheAlert(
                    "WARNING", "REDIS",
                    "Redis memory usage high: " + health.redisHealth().memoryUsagePercent() + "%",
                    LocalDateTime.now().toString(),
                    Map.of("usage_percent", health.redisHealth().memoryUsagePercent())
                ));
            }
            
            if (health.redisHealth().hitRate() < 0.5) {
                alerts.add(new CacheAlert(
                    "WARNING", "REDIS",
                    "Redis hit rate low: " + (health.redisHealth().hitRate() * 100) + "%",
                    LocalDateTime.now().toString(),
                    Map.of("hit_rate", health.redisHealth().hitRate())
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to get cache alerts", e);
        }
        
        return alerts;
    }
}