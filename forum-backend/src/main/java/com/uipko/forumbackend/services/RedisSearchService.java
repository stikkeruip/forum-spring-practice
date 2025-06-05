package com.uipko.forumbackend.services;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for Redis-based search capabilities
 * Provides full-text search using Redis data structures
 */
public interface RedisSearchService {
    
    /**
     * Index a document for search
     */
    void indexDocument(String indexName, String documentId, Map<String, Object> fields);
    
    /**
     * Remove document from index
     */
    void removeDocument(String indexName, String documentId);
    
    /**
     * Search documents by text
     */
    List<SearchResult> searchText(String indexName, String query, int limit);
    
    /**
     * Search with filters
     */
    List<SearchResult> searchWithFilters(String indexName, String query, Map<String, Object> filters, int limit);
    
    /**
     * Auto-complete suggestions
     */
    List<String> getSuggestions(String indexName, String prefix, int limit);
    
    /**
     * Get search statistics
     */
    SearchStatistics getSearchStatistics(String indexName);
    
    // Forum-specific search operations
    
    /**
     * Index a post for search
     */
    void indexPost(Long postId, String title, String content, String author, List<String> tags);
    
    /**
     * Search posts
     */
    List<PostSearchResult> searchPosts(String query, int limit);
    
    /**
     * Index a user for search
     */
    void indexUser(String username, String displayName, String bio);
    
    /**
     * Search users
     */
    List<UserSearchResult> searchUsers(String query, int limit);
    
    /**
     * Search result interface
     */
    record SearchResult(
        String documentId,
        Map<String, Object> fields,
        double score
    ) {}
    
    /**
     * Post search result
     */
    record PostSearchResult(
        Long postId,
        String title,
        String excerpt,
        String author,
        List<String> tags,
        double score
    ) {}
    
    /**
     * User search result
     */
    record UserSearchResult(
        String username,
        String displayName,
        String bio,
        double score
    ) {}
    
    /**
     * Search statistics
     */
    record SearchStatistics(
        String indexName,
        long totalDocuments,
        long totalSearches,
        Map<String, Long> popularTerms
    ) {}
}