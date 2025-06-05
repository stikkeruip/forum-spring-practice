package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {
    
    private final RedissonClient redissonClient;
    
    // Redis key patterns
    private static final String VISITORS_KEY = "forum:analytics:visitors";
    private static final String DAILY_VISITORS_KEY = "forum:analytics:visitors:daily:";
    private static final String POST_VIEWS_KEY = "forum:analytics:post_views:";
    private static final String ACTIVITY_KEY = "forum:analytics:activity:";
    
    public AnalyticsServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    @Override
    public void trackUniqueVisitor(String visitorId) {
        RHyperLogLog<String> visitors = redissonClient.getHyperLogLog(VISITORS_KEY);
        visitors.add(visitorId);
        
        // Also track for today
        trackUniqueVisitor(visitorId, LocalDate.now());
        
        log.debug("Tracked unique visitor: {}", visitorId);
    }
    
    @Override
    public void trackUniqueVisitor(String visitorId, LocalDate date) {
        String dateKey = DAILY_VISITORS_KEY + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        RHyperLogLog<String> dailyVisitors = redissonClient.getHyperLogLog(dateKey);
        dailyVisitors.add(visitorId);
        
        log.debug("Tracked unique visitor {} for date {}", visitorId, date);
    }
    
    @Override
    public void trackUniquePostView(Long postId, String visitorId) {
        String postKey = POST_VIEWS_KEY + postId;
        RHyperLogLog<String> postViews = redissonClient.getHyperLogLog(postKey);
        postViews.add(visitorId);
        
        log.debug("Tracked unique view for post {} by visitor {}", postId, visitorId);
    }
    
    @Override
    public void trackUniqueUserActivity(String username, String activityType) {
        String activityKey = ACTIVITY_KEY + activityType;
        RHyperLogLog<String> activityLog = redissonClient.getHyperLogLog(activityKey);
        activityLog.add(username);
        
        log.debug("Tracked unique activity {} for user {}", activityType, username);
    }
    
    @Override
    public long getUniqueVisitorCount() {
        RHyperLogLog<String> visitors = redissonClient.getHyperLogLog(VISITORS_KEY);
        return visitors.count();
    }
    
    @Override
    public long getUniqueVisitorCount(LocalDate date) {
        String dateKey = DAILY_VISITORS_KEY + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        RHyperLogLog<String> dailyVisitors = redissonClient.getHyperLogLog(dateKey);
        return dailyVisitors.count();
    }
    
    @Override
    public long getUniquePostViews(Long postId) {
        String postKey = POST_VIEWS_KEY + postId;
        RHyperLogLog<String> postViews = redissonClient.getHyperLogLog(postKey);
        return postViews.count();
    }
    
    @Override
    public long getUniqueActivityCount(String activityType) {
        String activityKey = ACTIVITY_KEY + activityType;
        RHyperLogLog<String> activityLog = redissonClient.getHyperLogLog(activityKey);
        return activityLog.count();
    }
    
    @Override
    public AnalyticsSummary getAnalyticsSummary() {
        long totalUniqueVisitors = getUniqueVisitorCount();
        long todayUniqueVisitors = getUniqueVisitorCount(LocalDate.now());
        
        // Calculate week and month visitors
        long thisWeekUniqueVisitors = mergeUniqueVisitors(getLastNDays(7));
        long thisMonthUniqueVisitors = mergeUniqueVisitors(getLastNDays(30));
        
        // Get popular posts (this would need additional tracking)
        Map<Long, Long> popularPosts = new HashMap<>();
        
        // Get activity counts
        Map<String, Long> activityCounts = Map.of(
            "posts", getUniqueActivityCount("post_creation"),
            "comments", getUniqueActivityCount("comment_creation"),
            "likes", getUniqueActivityCount("like_action")
        );
        
        return new AnalyticsSummary(
            totalUniqueVisitors,
            todayUniqueVisitors,
            thisWeekUniqueVisitors,
            thisMonthUniqueVisitors,
            popularPosts,
            activityCounts
        );
    }
    
    @Override
    public long mergeUniqueVisitors(Set<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        
        // Create temporary HyperLogLog for merging
        String tempKey = "forum:analytics:temp:" + UUID.randomUUID();
        RHyperLogLog<String> merged = redissonClient.getHyperLogLog(tempKey);
        
        try {
            // Merge all HyperLogLogs for the given dates
            List<String> hllKeys = new ArrayList<>();
            for (LocalDate date : dates) {
                String dateKey = DAILY_VISITORS_KEY + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                hllKeys.add(dateKey);
            }
            
            // Merge all HLLs by their key names
            if (!hllKeys.isEmpty()) {
                String[] keyArray = hllKeys.toArray(new String[0]);
                merged.mergeWith(keyArray);
            }
            
            return merged.count();
            
        } finally {
            // Clean up temporary key
            merged.delete();
        }
    }
    
    private Set<LocalDate> getLastNDays(int days) {
        Set<LocalDate> dates = new HashSet<>();
        LocalDate current = LocalDate.now();
        
        for (int i = 0; i < days; i++) {
            dates.add(current.minusDays(i));
        }
        
        return dates;
    }
}