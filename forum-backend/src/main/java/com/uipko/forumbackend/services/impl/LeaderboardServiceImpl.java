package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {
    
    private final RedissonClient redissonClient;
    
    // Redis key patterns
    private static final String LEADERBOARD_PREFIX = "forum:leaderboard:";
    private static final String TIME_BASED_PREFIX = "forum:leaderboard:time:";
    private static final String POST_METADATA_PREFIX = "forum:post:meta:";
    
    // Forum-specific leaderboards
    private static final String KARMA_LEADERBOARD = "karma";
    private static final String ACTIVITY_LEADERBOARD = "activity";
    private static final String COMMENTS_LEADERBOARD = "comments";
    private static final String POSTS_LEADERBOARD = "posts";
    private static final String WEEKLY_LEADERBOARD = "weekly";
    private static final String TRENDING_LEADERBOARD = "trending";
    
    public LeaderboardServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    @Override
    public void updateScore(String leaderboardName, String member, double score) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        leaderboard.add(score, member);
        
        log.debug("Updated score for {} in leaderboard {}: {}", member, leaderboardName, score);
    }
    
    @Override
    public double incrementScore(String leaderboardName, String member, double increment) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        Double newScore = leaderboard.addScore(member, increment);
        
        log.debug("Incremented score for {} in leaderboard {} by {}: new score = {}", 
            member, leaderboardName, increment, newScore);
        
        return newScore != null ? newScore : increment;
    }
    
    @Override
    public Double getScore(String leaderboardName, String member) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        return leaderboard.getScore(member);
    }
    
    @Override
    public Long getRank(String leaderboardName, String member) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        // Redisson returns rank in descending order (highest score = rank 0)
        Integer rank = leaderboard.revRank(member);
        return rank != null ? rank.longValue() : null;
    }
    
    @Override
    public Long getReverseRank(String leaderboardName, String member) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        // Ascending order (lowest score = rank 0)
        Integer rank = leaderboard.rank(member);
        return rank != null ? rank.longValue() : null;
    }
    
    @Override
    public List<LeaderboardEntry> getTopMembers(String leaderboardName, int count) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        
        Collection<ScoredEntry<String>> entries = leaderboard.entryRangeReversed(0, count - 1);
        
        List<LeaderboardEntry> result = new ArrayList<>();
        long rank = 0;
        
        for (ScoredEntry<String> entry : entries) {
            result.add(new LeaderboardEntry(
                entry.getValue(),
                entry.getScore(),
                rank++
            ));
        }
        
        return result;
    }
    
    @Override
    public List<LeaderboardEntry> getBottomMembers(String leaderboardName, int count) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        
        Collection<ScoredEntry<String>> entries = leaderboard.entryRange(0, count - 1);
        
        List<LeaderboardEntry> result = new ArrayList<>();
        long totalSize = leaderboard.size();
        long rank = totalSize - count;
        
        for (ScoredEntry<String> entry : entries) {
            result.add(new LeaderboardEntry(
                entry.getValue(),
                entry.getScore(),
                rank++
            ));
        }
        
        return result;
    }
    
    @Override
    public List<LeaderboardEntry> getMembersByRank(String leaderboardName, long startRank, long endRank) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        
        Collection<ScoredEntry<String>> entries = leaderboard.entryRangeReversed((int) startRank, (int) endRank);
        
        List<LeaderboardEntry> result = new ArrayList<>();
        long rank = startRank;
        
        for (ScoredEntry<String> entry : entries) {
            result.add(new LeaderboardEntry(
                entry.getValue(),
                entry.getScore(),
                rank++
            ));
        }
        
        return result;
    }
    
    @Override
    public List<LeaderboardEntry> getMembersByScore(String leaderboardName, double minScore, double maxScore) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        
        Collection<ScoredEntry<String>> entries = leaderboard.entryRangeReversed(minScore, true, maxScore, true);
        
        List<LeaderboardEntry> result = new ArrayList<>();
        
        for (ScoredEntry<String> entry : entries) {
            Long rank = getRank(leaderboardName, entry.getValue());
            result.add(new LeaderboardEntry(
                entry.getValue(),
                entry.getScore(),
                rank != null ? rank : -1
            ));
        }
        
        return result;
    }
    
    @Override
    public List<LeaderboardEntry> getMembersAroundMember(String leaderboardName, String member, int radius) {
        Long memberRank = getRank(leaderboardName, member);
        
        if (memberRank == null) {
            return Collections.emptyList();
        }
        
        long startRank = Math.max(0, memberRank - radius);
        long endRank = memberRank + radius;
        
        return getMembersByRank(leaderboardName, startRank, endRank);
    }
    
    @Override
    public boolean removeMember(String leaderboardName, String member) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        boolean removed = leaderboard.remove(member);
        
        if (removed) {
            log.debug("Removed {} from leaderboard {}", member, leaderboardName);
        }
        
        return removed;
    }
    
    @Override
    public long removeMembersByRank(String leaderboardName, long startRank, long endRank) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        int removed = leaderboard.removeRangeByRank((int) startRank, (int) endRank);
        
        log.debug("Removed {} members from leaderboard {} by rank range {}-{}", 
            removed, leaderboardName, startRank, endRank);
        
        return removed;
    }
    
    @Override
    public long removeMembersByScore(String leaderboardName, double minScore, double maxScore) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        int removed = leaderboard.removeRangeByScore(minScore, true, maxScore, true);
        
        log.debug("Removed {} members from leaderboard {} by score range {}-{}", 
            removed, leaderboardName, minScore, maxScore);
        
        return removed;
    }
    
    @Override
    public long getLeaderboardSize(String leaderboardName) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        return leaderboard.size();
    }
    
    @Override
    public void clearLeaderboard(String leaderboardName) {
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        leaderboard.clear();
        
        log.info("Cleared leaderboard {}", leaderboardName);
    }
    
    @Override
    public Set<String> getAllLeaderboards() {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(LEADERBOARD_PREFIX + "*");
        Set<String> leaderboards = new HashSet<>();
        
        for (String key : keys) {
            leaderboards.add(key.replace(LEADERBOARD_PREFIX, ""));
        }
        
        return leaderboards;
    }
    
    @Override
    public void updateTimeBasedScore(String baseLeaderboardName, String member, double score, TimePeriod period) {
        String timeKey = generateTimeKey(period);
        String leaderboardName = baseLeaderboardName + ":" + timeKey;
        
        updateScore(leaderboardName, member, score);
        
        // Set expiration for time-based leaderboards
        RScoredSortedSet<String> leaderboard = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + leaderboardName);
        Duration expiration = getExpirationForPeriod(period);
        leaderboard.expire(expiration);
        
        log.debug("Updated time-based score for {} in {} leaderboard: {}", member, leaderboardName, score);
    }
    
    @Override
    public List<LeaderboardEntry> getTimeBasedLeaderboard(String baseLeaderboardName, TimePeriod period, int count) {
        String timeKey = generateTimeKey(period);
        String leaderboardName = baseLeaderboardName + ":" + timeKey;
        
        return getTopMembers(leaderboardName, count);
    }
    
    @Override
    public void mergeLeaderboards(String targetLeaderboard, List<String> sourceLeaderboards, MergeOperation operation) {
        Map<String, Double> mergedScores = new HashMap<>();
        
        for (String sourceLeaderboard : sourceLeaderboards) {
            RScoredSortedSet<String> source = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + sourceLeaderboard);
            
            Collection<ScoredEntry<String>> entries = source.entryRange(0, -1);
            
            for (ScoredEntry<String> entry : entries) {
                String member = entry.getValue();
                double score = entry.getScore();
                
                mergedScores.merge(member, score, (existing, newScore) -> {
                    switch (operation) {
                        case SUM: return existing + newScore;
                        case MAX: return Math.max(existing, newScore);
                        case MIN: return Math.min(existing, newScore);
                        case AVERAGE: return (existing + newScore) / 2.0;
                        default: return newScore;
                    }
                });
            }
        }
        
        // Update target leaderboard
        RScoredSortedSet<String> target = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + targetLeaderboard);
        target.clear(); // Clear existing data
        
        for (Map.Entry<String, Double> entry : mergedScores.entrySet()) {
            target.add(entry.getValue(), entry.getKey());
        }
        
        log.info("Merged {} source leaderboards into {} using {} operation", 
            sourceLeaderboards.size(), targetLeaderboard, operation);
    }
    
    @Override
    public List<LeaderboardEntry> getFilteredLeaderboard(String leaderboardName, LeaderboardFilter filter, int count) {
        List<LeaderboardEntry> allMembers = getTopMembers(leaderboardName, Integer.MAX_VALUE);
        
        return allMembers.stream()
            .filter(entry -> filter.shouldInclude(entry.member(), entry.score()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    // Forum-specific leaderboard operations
    
    @Override
    public void updateUserKarma(String username, long karmaChange) {
        incrementScore(KARMA_LEADERBOARD, username, karmaChange);
        
        // Also update time-based karma
        updateTimeBasedScore(KARMA_LEADERBOARD, username, karmaChange, TimePeriod.WEEKLY);
        updateTimeBasedScore(KARMA_LEADERBOARD, username, karmaChange, TimePeriod.MONTHLY);
    }
    
    @Override
    public List<LeaderboardEntry> getKarmaLeaderboard(int count) {
        return getTopMembers(KARMA_LEADERBOARD, count);
    }
    
    @Override
    public void updatePostPopularity(Long postId, String title, double popularityScore) {
        String postKey = "post:" + postId;
        updateScore(POSTS_LEADERBOARD, postKey, popularityScore);
        
        // Store post metadata
        RMap<String, Object> postMeta = redissonClient.getMap(POST_METADATA_PREFIX + postId);
        postMeta.put("title", title);
        postMeta.put("lastUpdated", LocalDateTime.now().toString());
        postMeta.expire(Duration.ofDays(30)); // Keep metadata for 30 days
        
        // Update trending posts (with time decay)
        updateTrendingScore(postId, title, popularityScore);
        
        log.debug("Updated popularity score for post {}: {}", postId, popularityScore);
    }
    
    @Override
    public List<PostLeaderboardEntry> getPopularPosts(int count) {
        List<LeaderboardEntry> entries = getTopMembers(POSTS_LEADERBOARD, count);
        
        return entries.stream()
            .map(this::convertToPostEntry)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    public void updateUserActivity(String username, long activityScore) {
        incrementScore(ACTIVITY_LEADERBOARD, username, activityScore);
        
        // Update time-based activity
        updateTimeBasedScore(ACTIVITY_LEADERBOARD, username, activityScore, TimePeriod.DAILY);
        updateTimeBasedScore(ACTIVITY_LEADERBOARD, username, activityScore, TimePeriod.WEEKLY);
    }
    
    @Override
    public List<LeaderboardEntry> getMostActiveUsers(int count) {
        return getTopMembers(ACTIVITY_LEADERBOARD, count);
    }
    
    @Override
    public void updateCommentContribution(String username, long commentScore) {
        incrementScore(COMMENTS_LEADERBOARD, username, commentScore);
        
        // Update weekly comments
        updateTimeBasedScore(COMMENTS_LEADERBOARD, username, commentScore, TimePeriod.WEEKLY);
    }
    
    @Override
    public List<LeaderboardEntry> getTopCommenters(int count) {
        return getTopMembers(COMMENTS_LEADERBOARD, count);
    }
    
    @Override
    public void updateWeeklyContribution(String username, long contributionScore) {
        updateTimeBasedScore(WEEKLY_LEADERBOARD, username, contributionScore, TimePeriod.WEEKLY);
    }
    
    @Override
    public List<LeaderboardEntry> getWeeklyContributors(int count) {
        return getTimeBasedLeaderboard(WEEKLY_LEADERBOARD, TimePeriod.WEEKLY, count);
    }
    
    @Override
    public UserRankingSummary getUserRankingSummary(String username) {
        LeaderboardEntry karmaRanking = findUserInLeaderboard(KARMA_LEADERBOARD, username);
        LeaderboardEntry activityRanking = findUserInLeaderboard(ACTIVITY_LEADERBOARD, username);
        LeaderboardEntry commentRanking = findUserInLeaderboard(COMMENTS_LEADERBOARD, username);
        LeaderboardEntry weeklyRanking = findUserInTimeBasedLeaderboard(WEEKLY_LEADERBOARD, username, TimePeriod.WEEKLY);
        
        Map<String, LeaderboardEntry> customRankings = new HashMap<>();
        // Add any custom leaderboards here
        
        return new UserRankingSummary(
            username,
            karmaRanking,
            activityRanking,
            commentRanking,
            weeklyRanking,
            customRankings
        );
    }
    
    @Override
    public List<PostLeaderboardEntry> getTrendingPosts(Duration timeWindow, int count) {
        // Calculate trending posts based on recent activity
        String trendingKey = TRENDING_LEADERBOARD + ":" + generateTimeKey(TimePeriod.DAILY);
        
        List<LeaderboardEntry> entries = getTopMembers(trendingKey, count);
        
        return entries.stream()
            .map(this::convertToPostEntry)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private String generateTimeKey(TimePeriod period) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (period) {
            case DAILY:
                return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case WEEKLY:
                // Get week of year
                return now.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
            case MONTHLY:
                return now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case YEARLY:
                return now.format(DateTimeFormatter.ofPattern("yyyy"));
            default:
                return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }
    
    private Duration getExpirationForPeriod(TimePeriod period) {
        switch (period) {
            case DAILY: return Duration.ofDays(2);
            case WEEKLY: return Duration.ofDays(14);
            case MONTHLY: return Duration.ofDays(62);
            case YEARLY: return Duration.ofDays(400);
            default: return Duration.ofDays(2);
        }
    }
    
    private void updateTrendingScore(Long postId, String title, double popularityScore) {
        // Apply time decay to trending score
        LocalDateTime now = LocalDateTime.now();
        double timeDecay = calculateTimeDecay(now);
        double trendingScore = popularityScore * timeDecay;
        
        String trendingKey = TRENDING_LEADERBOARD + ":" + generateTimeKey(TimePeriod.DAILY);
        String postKey = "post:" + postId;
        
        updateScore(trendingKey, postKey, trendingScore);
        
        // Set expiration for trending leaderboard
        RScoredSortedSet<String> trending = redissonClient.getScoredSortedSet(LEADERBOARD_PREFIX + trendingKey);
        trending.expire(Duration.ofDays(2));
    }
    
    private double calculateTimeDecay(LocalDateTime timestamp) {
        // Simple time decay: newer content gets higher multiplier
        LocalDateTime now = LocalDateTime.now();
        long hoursSince = java.time.Duration.between(timestamp, now).toHours();
        
        // Decay factor: 1.0 for current hour, decreasing over time
        return Math.max(0.1, 1.0 - (hoursSince * 0.05));
    }
    
    private PostLeaderboardEntry convertToPostEntry(LeaderboardEntry entry) {
        try {
            String postKey = entry.member();
            if (!postKey.startsWith("post:")) {
                return null;
            }
            
            Long postId = Long.parseLong(postKey.substring(5));
            
            RMap<String, Object> postMeta = redissonClient.getMap(POST_METADATA_PREFIX + postId);
            String title = (String) postMeta.getOrDefault("title", "Unknown Title");
            String author = (String) postMeta.getOrDefault("author", "Unknown Author");
            String createdAtStr = (String) postMeta.get("createdAt");
            
            LocalDateTime createdAt = createdAtStr != null ? 
                LocalDateTime.parse(createdAtStr) : LocalDateTime.now();
            
            return new PostLeaderboardEntry(
                postId,
                title,
                author,
                entry.score(),
                entry.rank(),
                createdAt
            );
            
        } catch (Exception e) {
            log.warn("Failed to convert leaderboard entry to post entry: {}", entry, e);
            return null;
        }
    }
    
    private LeaderboardEntry findUserInLeaderboard(String leaderboardName, String username) {
        Double score = getScore(leaderboardName, username);
        Long rank = getRank(leaderboardName, username);
        
        if (score != null && rank != null) {
            return new LeaderboardEntry(username, score, rank);
        }
        
        return null;
    }
    
    private LeaderboardEntry findUserInTimeBasedLeaderboard(String baseLeaderboardName, String username, TimePeriod period) {
        String timeKey = generateTimeKey(period);
        String leaderboardName = baseLeaderboardName + ":" + timeKey;
        
        return findUserInLeaderboard(leaderboardName, username);
    }
}