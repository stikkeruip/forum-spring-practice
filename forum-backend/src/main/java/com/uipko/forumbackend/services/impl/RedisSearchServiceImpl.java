package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.RedisSearchService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RedisSearchServiceImpl implements RedisSearchService {
    
    private final RedissonClient redissonClient;
    
    // Redis key patterns
    private static final String INDEX_PREFIX = "forum:search:index:";
    private static final String DOCUMENT_PREFIX = "forum:search:doc:";
    private static final String TERMS_PREFIX = "forum:search:terms:";
    private static final String STATS_PREFIX = "forum:search:stats:";
    
    public RedisSearchServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    @Override
    public void indexDocument(String indexName, String documentId, Map<String, Object> fields) {
        // Store document data
        RMap<String, Object> document = redissonClient.getMap(DOCUMENT_PREFIX + indexName + ":" + documentId);
        document.putAll(fields);
        
        // Index terms for search
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (field.getValue() instanceof String) {
                String text = (String) field.getValue();
                Set<String> terms = extractTerms(text);
                
                for (String term : terms) {
                    RSet<String> termIndex = redissonClient.getSet(TERMS_PREFIX + indexName + ":" + field.getKey() + ":" + term.toLowerCase());
                    termIndex.add(documentId);
                }
            }
        }
        
        // Update statistics
        updateSearchStats(indexName, "indexed");
        
        log.debug("Indexed document {} in index {}", documentId, indexName);
    }
    
    @Override
    public void removeDocument(String indexName, String documentId) {
        // Remove document
        RMap<String, Object> document = redissonClient.getMap(DOCUMENT_PREFIX + indexName + ":" + documentId);
        Map<String, Object> fields = document.readAllMap();
        document.delete();
        
        // Remove from term indexes
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (field.getValue() instanceof String) {
                String text = (String) field.getValue();
                Set<String> terms = extractTerms(text);
                
                for (String term : terms) {
                    RSet<String> termIndex = redissonClient.getSet(TERMS_PREFIX + indexName + ":" + field.getKey() + ":" + term.toLowerCase());
                    termIndex.remove(documentId);
                }
            }
        }
        
        log.debug("Removed document {} from index {}", documentId, indexName);
    }
    
    @Override
    public List<SearchResult> searchText(String indexName, String query, int limit) {
        return searchWithFilters(indexName, query, Collections.emptyMap(), limit);
    }
    
    @Override
    public List<SearchResult> searchWithFilters(String indexName, String query, Map<String, Object> filters, int limit) {
        Set<String> terms = extractTerms(query);
        Map<String, Double> documentScores = new HashMap<>();
        
        // Search across all text fields
        for (String term : terms) {
            String termLower = term.toLowerCase();
            
            // Search in title field (higher weight)
            RSet<String> titleMatches = redissonClient.getSet(TERMS_PREFIX + indexName + ":title:" + termLower);
            for (String docId : titleMatches) {
                documentScores.merge(docId, 2.0, Double::sum);
            }
            
            // Search in content field
            RSet<String> contentMatches = redissonClient.getSet(TERMS_PREFIX + indexName + ":content:" + termLower);
            for (String docId : contentMatches) {
                documentScores.merge(docId, 1.0, Double::sum);
            }
            
            // Search in other text fields
            RSet<String> otherMatches = redissonClient.getSet(TERMS_PREFIX + indexName + ":*:" + termLower);
            for (String docId : otherMatches) {
                documentScores.merge(docId, 0.5, Double::sum);
            }
        }
        
        // Apply filters
        if (!filters.isEmpty()) {
            documentScores = applyFilters(indexName, documentScores, filters);
        }
        
        // Sort by score and limit results
        List<SearchResult> results = documentScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                String docId = entry.getKey();
                double score = entry.getValue();
                
                RMap<String, Object> document = redissonClient.getMap(DOCUMENT_PREFIX + indexName + ":" + docId);
                Map<String, Object> fields = document.readAllMap();
                
                return new SearchResult(docId, fields, score);
            })
            .collect(Collectors.toList());
        
        // Update search statistics
        updateSearchStats(indexName, "searched");
        updatePopularTerms(indexName, terms);
        
        log.debug("Found {} results for query '{}' in index {}", results.size(), query, indexName);
        
        return results;
    }
    
    @Override
    public List<String> getSuggestions(String indexName, String prefix, int limit) {
        String prefixLower = prefix.toLowerCase();
        Set<String> suggestions = new HashSet<>();
        
        // Get all term keys that start with prefix
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(TERMS_PREFIX + indexName + ":*:" + prefixLower + "*");
        
        for (String key : keys) {
            // Extract term from key
            String[] parts = key.split(":");
            if (parts.length >= 4) {
                String term = parts[parts.length - 1];
                if (term.startsWith(prefixLower)) {
                    suggestions.add(term);
                }
            }
            
            if (suggestions.size() >= limit) {
                break;
            }
        }
        
        return new ArrayList<>(suggestions);
    }
    
    @Override
    public SearchStatistics getSearchStatistics(String indexName) {
        RMap<String, Object> stats = redissonClient.getMap(STATS_PREFIX + indexName);
        
        long totalDocuments = ((Number) stats.getOrDefault("totalDocuments", 0L)).longValue();
        long totalSearches = ((Number) stats.getOrDefault("totalSearches", 0L)).longValue();
        
        @SuppressWarnings("unchecked")
        Map<String, Long> popularTerms = (Map<String, Long>) stats.getOrDefault("popularTerms", new HashMap<>());
        
        return new SearchStatistics(indexName, totalDocuments, totalSearches, popularTerms);
    }
    
    // Forum-specific search operations
    
    @Override
    public void indexPost(Long postId, String title, String content, String author, List<String> tags) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", title);
        fields.put("content", content);
        fields.put("author", author);
        fields.put("tags", String.join(" ", tags));
        fields.put("type", "post");
        
        indexDocument("posts", postId.toString(), fields);
    }
    
    @Override
    public List<PostSearchResult> searchPosts(String query, int limit) {
        List<SearchResult> results = searchText("posts", query, limit);
        
        return results.stream()
            .map(result -> {
                Map<String, Object> fields = result.fields();
                Long postId = Long.parseLong(result.documentId());
                String title = (String) fields.get("title");
                String content = (String) fields.get("content");
                String author = (String) fields.get("author");
                String tagsStr = (String) fields.getOrDefault("tags", "");
                List<String> tags = Arrays.asList(tagsStr.split(" "));
                
                // Create excerpt from content
                String excerpt = createExcerpt(content, query, 200);
                
                return new PostSearchResult(postId, title, excerpt, author, tags, result.score());
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public void indexUser(String username, String displayName, String bio) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("username", username);
        fields.put("displayName", displayName);
        fields.put("bio", bio);
        fields.put("type", "user");
        
        indexDocument("users", username, fields);
    }
    
    @Override
    public List<UserSearchResult> searchUsers(String query, int limit) {
        List<SearchResult> results = searchText("users", query, limit);
        
        return results.stream()
            .map(result -> {
                Map<String, Object> fields = result.fields();
                String username = result.documentId();
                String displayName = (String) fields.get("displayName");
                String bio = (String) fields.get("bio");
                
                return new UserSearchResult(username, displayName, bio, result.score());
            })
            .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private Set<String> extractTerms(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        // Simple tokenization - split by non-alphanumeric characters
        return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9]+"))
            .filter(term -> term.length() > 2) // Filter out short terms
            .collect(Collectors.toSet());
    }
    
    private Map<String, Double> applyFilters(String indexName, Map<String, Double> documentScores, Map<String, Object> filters) {
        Map<String, Double> filtered = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : documentScores.entrySet()) {
            String docId = entry.getKey();
            RMap<String, Object> document = redissonClient.getMap(DOCUMENT_PREFIX + indexName + ":" + docId);
            
            boolean matches = true;
            for (Map.Entry<String, Object> filter : filters.entrySet()) {
                Object docValue = document.get(filter.getKey());
                if (!Objects.equals(docValue, filter.getValue())) {
                    matches = false;
                    break;
                }
            }
            
            if (matches) {
                filtered.put(docId, entry.getValue());
            }
        }
        
        return filtered;
    }
    
    private void updateSearchStats(String indexName, String operation) {
        RMap<String, Object> stats = redissonClient.getMap(STATS_PREFIX + indexName);
        
        if ("indexed".equals(operation)) {
            stats.merge("totalDocuments", 1L, (a, b) -> ((Long) a) + ((Long) b));
        } else if ("searched".equals(operation)) {
            stats.merge("totalSearches", 1L, (a, b) -> ((Long) a) + ((Long) b));
        }
    }
    
    private void updatePopularTerms(String indexName, Set<String> terms) {
        RMap<String, Object> stats = redissonClient.getMap(STATS_PREFIX + indexName);
        
        @SuppressWarnings("unchecked")
        Map<String, Long> popularTerms = (Map<String, Long>) stats.getOrDefault("popularTerms", new HashMap<>());
        
        for (String term : terms) {
            popularTerms.merge(term, 1L, Long::sum);
        }
        
        // Keep only top 100 popular terms
        if (popularTerms.size() > 100) {
            popularTerms = popularTerms.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(100)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
        
        stats.put("popularTerms", popularTerms);
    }
    
    private String createExcerpt(String content, String query, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        if (content.length() <= maxLength) {
            return content;
        }
        
        // Try to find query terms in content and create excerpt around them
        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();
        
        int index = contentLower.indexOf(queryLower);
        if (index >= 0) {
            int start = Math.max(0, index - maxLength / 2);
            int end = Math.min(content.length(), start + maxLength);
            
            String excerpt = content.substring(start, end);
            
            if (start > 0) {
                excerpt = "..." + excerpt;
            }
            if (end < content.length()) {
                excerpt = excerpt + "...";
            }
            
            return excerpt;
        }
        
        // If query not found, return beginning of content
        return content.substring(0, Math.min(maxLength, content.length())) + "...";
    }
}