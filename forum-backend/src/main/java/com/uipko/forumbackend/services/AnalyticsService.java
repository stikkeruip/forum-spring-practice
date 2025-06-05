package com.uipko.forumbackend.services;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Service for analytics using Redis HyperLogLog for efficient unique counting
 * Provides memory-efficient cardinality estimation for large datasets
 */
public interface AnalyticsService {
    
    /**
     * Track unique visitor
     */
    void trackUniqueVisitor(String visitorId);
    
    /**
     * Track unique visitor for specific date
     */
    void trackUniqueVisitor(String visitorId, LocalDate date);
    
    /**
     * Track unique post view
     */
    void trackUniquePostView(Long postId, String visitorId);
    
    /**
     * Track unique user activity
     */
    void trackUniqueUserActivity(String username, String activityType);
    
    /**
     * Get unique visitor count
     */
    long getUniqueVisitorCount();
    
    /**
     * Get unique visitor count for date
     */
    long getUniqueVisitorCount(LocalDate date);
    
    /**
     * Get unique post views
     */
    long getUniquePostViews(Long postId);
    
    /**
     * Get unique activity count
     */
    long getUniqueActivityCount(String activityType);
    
    /**
     * Get analytics summary
     */
    AnalyticsSummary getAnalyticsSummary();
    
    /**
     * Merge analytics from multiple time periods
     */
    long mergeUniqueVisitors(Set<LocalDate> dates);
    
    /**
     * Analytics summary
     */
    record AnalyticsSummary(
        long totalUniqueVisitors,
        long todayUniqueVisitors,
        long thisWeekUniqueVisitors,
        long thisMonthUniqueVisitors,
        Map<Long, Long> popularPosts,
        Map<String, Long> activityCounts
    ) {}
}