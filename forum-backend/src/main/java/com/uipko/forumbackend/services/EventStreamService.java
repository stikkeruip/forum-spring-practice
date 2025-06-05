package com.uipko.forumbackend.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for event sourcing using Redis Streams
 * Provides immutable event log for audit trails and event-driven architecture
 */
public interface EventStreamService {
    
    /**
     * Publish an event to a stream
     */
    String publishEvent(String streamName, Event event);
    
    /**
     * Publish multiple events in a transaction
     */
    List<String> publishEventsBatch(String streamName, List<Event> events);
    
    /**
     * Read events from stream
     */
    List<StreamEvent> readEvents(String streamName, String startId, int count);
    
    /**
     * Read events from stream with time range
     */
    List<StreamEvent> readEventsByTimeRange(String streamName, LocalDateTime start, LocalDateTime end);
    
    /**
     * Subscribe to a stream (blocking)
     */
    void subscribe(String streamName, String consumerGroup, String consumerName, EventConsumer consumer);
    
    /**
     * Subscribe to a stream (non-blocking)
     */
    CompletableFuture<Void> subscribeAsync(String streamName, String consumerGroup, String consumerName, EventConsumer consumer);
    
    /**
     * Create consumer group
     */
    void createConsumerGroup(String streamName, String groupName, String startId);
    
    /**
     * Acknowledge event processing
     */
    void acknowledgeEvent(String streamName, String groupName, String eventId);
    
    /**
     * Get pending events for consumer
     */
    List<StreamEvent> getPendingEvents(String streamName, String groupName, String consumerName);
    
    /**
     * Claim abandoned events
     */
    List<StreamEvent> claimAbandonedEvents(String streamName, String groupName, String consumerName, Duration idleTime);
    
    /**
     * Get stream info
     */
    StreamInfo getStreamInfo(String streamName);
    
    /**
     * Trim stream to size
     */
    void trimStream(String streamName, long maxSize);
    
    /**
     * Trim stream by age
     */
    void trimStreamByAge(String streamName, Duration maxAge);
    
    // Forum-specific event operations
    
    /**
     * Publish user event
     */
    String publishUserEvent(UserEvent event);
    
    /**
     * Publish post event
     */
    String publishPostEvent(PostEvent event);
    
    /**
     * Publish comment event
     */
    String publishCommentEvent(CommentEvent event);
    
    /**
     * Publish notification event
     */
    String publishNotificationEvent(NotificationEvent event);
    
    /**
     * Publish moderation event
     */
    String publishModerationEvent(ModerationEvent event);
    
    /**
     * Get user activity stream
     */
    List<StreamEvent> getUserActivityStream(String username, int count);
    
    /**
     * Get audit trail for entity
     */
    List<StreamEvent> getAuditTrail(String entityType, String entityId);
    
    /**
     * Base event interface
     */
    interface Event {
        String getEventType();
        String getEntityId();
        String getUserId();
        Map<String, Object> getData();
        LocalDateTime getTimestamp();
    }
    
    /**
     * Event consumer interface
     */
    interface EventConsumer {
        void consume(StreamEvent event);
        void onError(Exception error);
    }
    
    /**
     * Stream event wrapper
     */
    record StreamEvent(
        String id,
        String streamName,
        Map<String, Object> data,
        LocalDateTime timestamp
    ) {}
    
    /**
     * Stream information
     */
    record StreamInfo(
        String streamName,
        long length,
        String firstEntry,
        String lastEntry,
        long radixTreeKeys,
        long radixTreeNodes,
        List<ConsumerGroupInfo> consumerGroups
    ) {}
    
    /**
     * Consumer group information
     */
    record ConsumerGroupInfo(
        String name,
        long consumers,
        long pending,
        String lastDeliveredId
    ) {}
    
    // Event types
    
    /**
     * User-related event
     */
    record UserEvent(
        String eventType, // REGISTERED, LOGIN, LOGOUT, PROFILE_UPDATED, etc.
        String userId,
        Map<String, Object> data,
        LocalDateTime timestamp
    ) implements Event {
        @Override
        public String getEntityId() { return userId; }
        @Override
        public String getUserId() { return userId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Map<String, Object> getData() { return data; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Post-related event
     */
    record PostEvent(
        String eventType, // CREATED, UPDATED, DELETED, LIKED, etc.
        String postId,
        String userId,
        Map<String, Object> data,
        LocalDateTime timestamp
    ) implements Event {
        @Override
        public String getEntityId() { return postId; }
        @Override
        public String getUserId() { return userId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Map<String, Object> getData() { return data; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Comment-related event
     */
    record CommentEvent(
        String eventType, // ADDED, UPDATED, DELETED, LIKED, etc.
        String commentId,
        String postId,
        String userId,
        Map<String, Object> data,
        LocalDateTime timestamp
    ) implements Event {
        @Override
        public String getEntityId() { return commentId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public String getUserId() { return userId; }
        @Override
        public Map<String, Object> getData() { return data; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Notification-related event
     */
    record NotificationEvent(
        String eventType, // SENT, READ, DELETED
        String notificationId,
        String recipientId,
        String actorId,
        Map<String, Object> data,
        LocalDateTime timestamp
    ) implements Event {
        @Override
        public String getEntityId() { return notificationId; }
        @Override
        public String getUserId() { return recipientId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Map<String, Object> getData() { return data; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Moderation-related event
     */
    record ModerationEvent(
        String eventType, // POST_REMOVED, COMMENT_REMOVED, USER_BANNED, etc.
        String targetType,
        String targetId,
        String moderatorId,
        String reason,
        Map<String, Object> data,
        LocalDateTime timestamp
    ) implements Event {
        @Override
        public String getEntityId() { return targetId; }
        @Override
        public String getUserId() { return moderatorId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Map<String, Object> getData() { return data; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}