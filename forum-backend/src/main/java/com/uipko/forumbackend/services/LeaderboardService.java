package com.uipko.forumbackend.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing leaderboards and rankings using Redis Sorted Sets
 * Provides real-time rankings for various forum metrics
 */
public interface LeaderboardService {
    
    /**
     * Add or update a score in the leaderboard
     */
    void updateScore(String leaderboardName, String member, double score);
    
    /**
     * Increment a member's score
     */
    double incrementScore(String leaderboardName, String member, double increment);
    
    /**
     * Get member's score
     */
    Double getScore(String leaderboardName, String member);
    
    /**
     * Get member's rank (0-based, where 0 is the highest score)
     */
    Long getRank(String leaderboardName, String member);
    
    /**
     * Get reverse rank (0-based, where 0 is the lowest score)
     */
    Long getReverseRank(String leaderboardName, String member);
    
    /**
     * Get top N members
     */
    List<LeaderboardEntry> getTopMembers(String leaderboardName, int count);
    
    /**
     * Get bottom N members
     */
    List<LeaderboardEntry> getBottomMembers(String leaderboardName, int count);
    
    /**
     * Get members in rank range
     */
    List<LeaderboardEntry> getMembersByRank(String leaderboardName, long startRank, long endRank);
    
    /**
     * Get members in score range
     */
    List<LeaderboardEntry> getMembersByScore(String leaderboardName, double minScore, double maxScore);
    
    /**
     * Get members around a specific member
     */
    List<LeaderboardEntry> getMembersAroundMember(String leaderboardName, String member, int radius);
    
    /**
     * Remove member from leaderboard
     */
    boolean removeMember(String leaderboardName, String member);
    
    /**
     * Remove members by rank range
     */
    long removeMembersByRank(String leaderboardName, long startRank, long endRank);
    
    /**
     * Remove members by score range
     */
    long removeMembersByScore(String leaderboardName, double minScore, double maxScore);
    
    /**
     * Get leaderboard size
     */
    long getLeaderboardSize(String leaderboardName);
    
    /**
     * Clear entire leaderboard
     */
    void clearLeaderboard(String leaderboardName);
    
    /**
     * Get all leaderboard names
     */
    Set<String> getAllLeaderboards();
    
    /**
     * Create time-based leaderboard (daily, weekly, monthly)
     */
    void updateTimeBasedScore(String baseLeaderboardName, String member, double score, TimePeriod period);
    
    /**
     * Get time-based leaderboard
     */
    List<LeaderboardEntry> getTimeBasedLeaderboard(String baseLeaderboardName, TimePeriod period, int count);
    
    /**
     * Merge multiple leaderboards
     */
    void mergeLeaderboards(String targetLeaderboard, List<String> sourceLeaderboards, MergeOperation operation);
    
    /**
     * Create filtered leaderboard
     */
    List<LeaderboardEntry> getFilteredLeaderboard(String leaderboardName, LeaderboardFilter filter, int count);
    
    // Forum-specific leaderboard operations
    
    /**
     * Update user karma score
     */
    void updateUserKarma(String username, long karmaChange);
    
    /**
     * Get karma leaderboard
     */
    List<LeaderboardEntry> getKarmaLeaderboard(int count);
    
    /**
     * Update post popularity score
     */
    void updatePostPopularity(Long postId, String title, double popularityScore);
    
    /**
     * Get popular posts leaderboard
     */
    List<PostLeaderboardEntry> getPopularPosts(int count);
    
    /**
     * Update active users ranking
     */
    void updateUserActivity(String username, long activityScore);
    
    /**
     * Get most active users
     */
    List<LeaderboardEntry> getMostActiveUsers(int count);
    
    /**
     * Update comment contribution score
     */
    void updateCommentContribution(String username, long commentScore);
    
    /**
     * Get top commenters
     */
    List<LeaderboardEntry> getTopCommenters(int count);
    
    /**
     * Update weekly contribution
     */
    void updateWeeklyContribution(String username, long contributionScore);
    
    /**
     * Get weekly contributors
     */
    List<LeaderboardEntry> getWeeklyContributors(int count);
    
    /**
     * Get user's overall ranking summary
     */
    UserRankingSummary getUserRankingSummary(String username);
    
    /**
     * Get trending content (based on recent activity)
     */
    List<PostLeaderboardEntry> getTrendingPosts(Duration timeWindow, int count);
    
    /**
     * Time period enumeration
     */
    enum TimePeriod {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
    
    /**
     * Merge operation enumeration
     */
    enum MergeOperation {
        SUM, MAX, MIN, AVERAGE
    }
    
    /**
     * Leaderboard entry
     */
    record LeaderboardEntry(
        String member,
        double score,
        long rank
    ) {}
    
    /**
     * Post leaderboard entry
     */
    record PostLeaderboardEntry(
        Long postId,
        String title,
        String author,
        double score,
        long rank,
        LocalDateTime createdAt
    ) {}
    
    /**
     * User ranking summary
     */
    record UserRankingSummary(
        String username,
        LeaderboardEntry karmaRanking,
        LeaderboardEntry activityRanking,
        LeaderboardEntry commentRanking,
        LeaderboardEntry weeklyRanking,
        Map<String, LeaderboardEntry> customRankings
    ) {}
    
    /**
     * Leaderboard filter interface
     */
    interface LeaderboardFilter {
        boolean shouldInclude(String member, double score);
    }
}