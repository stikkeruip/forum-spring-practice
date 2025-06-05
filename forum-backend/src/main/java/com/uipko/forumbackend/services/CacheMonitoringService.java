package com.uipko.forumbackend.services;

import java.util.List;
import java.util.Map;

/**
 * Service for monitoring cache performance and health across all cache layers
 * Provides comprehensive insights into cache usage, efficiency, and system health
 */
public interface CacheMonitoringService {
    
    /**
     * Get comprehensive cache health status
     */
    CacheHealthStatus getCacheHealthStatus();
    
    /**
     * Get detailed performance metrics for all cache layers
     */
    CachePerformanceMetrics getPerformanceMetrics();
    
    /**
     * Get cache usage statistics
     */
    CacheUsageStats getUsageStats();
    
    /**
     * Get cache efficiency metrics
     */
    CacheEfficiencyMetrics getEfficiencyMetrics();
    
    /**
     * Get Redis-specific monitoring data
     */
    RedisMonitoringData getRedisMonitoringData();
    
    /**
     * Get session management statistics
     */
    SessionManagementStats getSessionStats();
    
    /**
     * Get real-time cache operations metrics
     */
    RealTimeCacheMetrics getRealTimeMetrics();
    
    /**
     * Generate cache optimization recommendations
     */
    List<CacheOptimizationRecommendation> getOptimizationRecommendations();
    
    /**
     * Export cache metrics for external monitoring systems
     */
    Map<String, Object> exportMetricsForPrometheus();
    
    /**
     * Get cache alert conditions
     */
    List<CacheAlert> getCacheAlerts();
    
    /**
     * Cache health status
     */
    record CacheHealthStatus(
        boolean healthy,
        String status, // HEALTHY, DEGRADED, CRITICAL
        List<String> issues,
        double overallScore,
        RedisHealthStatus redisHealth,
        LocalCacheHealthStatus localCacheHealth,
        SessionCacheHealthStatus sessionHealth
    ) {}
    
    /**
     * Redis health status
     */
    record RedisHealthStatus(
        boolean connected,
        String version,
        long memoryUsed,
        long memoryMax,
        double memoryUsagePercent,
        long connectedClients,
        double hitRate,
        long totalCommands,
        String role
    ) {}
    
    /**
     * Local cache health status
     */
    record LocalCacheHealthStatus(
        long totalSize,
        long maxSize,
        double usagePercent,
        double hitRate,
        long evictionCount,
        double averageLoadTime
    ) {}
    
    /**
     * Session cache health status
     */
    record SessionCacheHealthStatus(
        long activeSessions,
        long expiredSessions,
        double sessionHitRate,
        String averageSessionDuration,
        long totalLogins,
        long sessionTimeouts
    ) {}
    
    /**
     * Performance metrics
     */
    record CachePerformanceMetrics(
        double averageResponseTime,
        double p95ResponseTime,
        double p99ResponseTime,
        long throughputPerSecond,
        double errorRate,
        Map<String, Double> operationLatency
    ) {}
    
    /**
     * Usage statistics
     */
    record CacheUsageStats(
        long totalRequests,
        long totalHits,
        long totalMisses,
        double overallHitRate,
        Map<String, Long> cacheTypeUsage,
        Map<String, Double> cacheTypeHitRates,
        long totalCacheSize,
        Map<String, Long> cacheSizeByType
    ) {}
    
    /**
     * Efficiency metrics
     */
    record CacheEfficiencyMetrics(
        double memoryEfficiency,
        double timeEfficiency,
        double costEfficiency,
        long redundantCacheEntries,
        long underutilizedCacheSpace,
        List<String> mostAccessedKeys,
        List<String> leastAccessedKeys
    ) {}
    
    /**
     * Redis monitoring data
     */
    record RedisMonitoringData(
        Map<String, String> info,
        Map<String, Long> stats,
        List<String> slowlog,
        Map<String, Long> keyspaceStats,
        long totalConnectionsReceived,
        long totalCommandsProcessed,
        double instantaneousOpsPerSec
    ) {}
    
    /**
     * Session management statistics
     */
    record SessionManagementStats(
        long totalActiveSessions,
        long averageSessionDuration,
        long totalSessionCreations,
        long totalSessionInvalidations,
        double sessionCreationRate,
        Map<String, Long> sessionsByUser,
        long concurrentSessionsMax
    ) {}
    
    /**
     * Real-time cache metrics
     */
    record RealTimeCacheMetrics(
        long onlineUsersCount,
        long notificationsCached,
        long postsCached,
        long reactionsCached,
        double cacheRefreshRate,
        long lastRefreshTime,
        Map<String, Long> realTimeCacheSizes
    ) {}
    
    /**
     * Cache optimization recommendation
     */
    record CacheOptimizationRecommendation(
        String type, // MEMORY, PERFORMANCE, CONFIGURATION
        String priority, // HIGH, MEDIUM, LOW
        String title,
        String description,
        String action,
        double potentialImprovement
    ) {}
    
    /**
     * Cache alert
     */
    record CacheAlert(
        String level, // ERROR, WARNING, INFO
        String component, // REDIS, LOCAL_CACHE, SESSION
        String message,
        String timestamp,
        Map<String, Object> details
    ) {}
}