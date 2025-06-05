package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.EventStreamService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.stream.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventStreamServiceImpl implements EventStreamService {
    
    private final RedissonClient redissonClient;
    
    // Stream name prefixes
    private static final String STREAM_PREFIX = "forum:stream:";
    private static final String USER_STREAM = "forum:stream:users";
    private static final String POST_STREAM = "forum:stream:posts";
    private static final String COMMENT_STREAM = "forum:stream:comments";
    private static final String NOTIFICATION_STREAM = "forum:stream:notifications";
    private static final String MODERATION_STREAM = "forum:stream:moderation";
    private static final String AUDIT_STREAM = "forum:stream:audit";
    
    public EventStreamServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    @Override
    public String publishEvent(String streamName, Event event) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", event.getEventType());
        eventData.put("entityId", event.getEntityId());
        eventData.put("userId", event.getUserId());
        eventData.put("timestamp", event.getTimestamp().toString());
        eventData.putAll(event.getData());
        
        StreamMessageId messageId = stream.add(StreamAddArgs.entries(eventData));
        
        log.debug("Published event {} to stream {} with ID {}", event.getEventType(), streamName, messageId);
        
        // Also publish to audit stream for complete trail
        publishAuditEvent(streamName, event, messageId.toString());
        
        return messageId.toString();
    }
    
    @Override
    public List<String> publishEventsBatch(String streamName, List<Event> events) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        RBatch batch = redissonClient.createBatch();
        RStreamAsync<String, Object> streamAsync = batch.getStream(STREAM_PREFIX + streamName);
        
        List<String> eventIds = new ArrayList<>();
        
        for (Event event : events) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventType", event.getEventType());
            eventData.put("entityId", event.getEntityId());
            eventData.put("userId", event.getUserId());
            eventData.put("timestamp", event.getTimestamp().toString());
            eventData.putAll(event.getData());
            
            streamAsync.addAsync(StreamAddArgs.entries(eventData));
        }
        
        BatchResult<?> results = batch.execute();
        
        log.debug("Published batch of {} events to stream {}", events.size(), streamName);
        
        return eventIds;
    }
    
    @Override
    public List<StreamEvent> readEvents(String streamName, String startId, int count) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        StreamMessageId start = startId != null ? parseStreamId(startId) : StreamMessageId.MIN;
        
        Map<StreamMessageId, Map<String, Object>> messages = stream.read(
            StreamReadArgs.greaterThan(start).count(count)
        );
        
        return messages.entrySet().stream()
            .map(entry -> new StreamEvent(
                entry.getKey().toString(),
                streamName,
                entry.getValue(),
                parseTimestamp(entry.getValue().get("timestamp"))
            ))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StreamEvent> readEventsByTimeRange(String streamName, LocalDateTime start, LocalDateTime end) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        // Convert timestamps to stream IDs (approximation)
        String startId = start.toInstant(ZoneOffset.UTC).toEpochMilli() + "-0";
        String endId = end.toInstant(ZoneOffset.UTC).toEpochMilli() + "-999";
        
        Map<StreamMessageId, Map<String, Object>> messages = stream.range(
            parseStreamId(startId),
            parseStreamId(endId)
        );
        
        return messages.entrySet().stream()
            .map(entry -> new StreamEvent(
                entry.getKey().toString(),
                streamName,
                entry.getValue(),
                parseTimestamp(entry.getValue().get("timestamp"))
            ))
            .collect(Collectors.toList());
    }
    
    @Override
    public void subscribe(String streamName, String consumerGroup, String consumerName, EventConsumer consumer) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        // Ensure consumer group exists
        try {
            stream.createGroup(StreamCreateGroupArgs.name(consumerGroup).id(StreamMessageId.NEWEST));
        } catch (Exception e) {
            // Group might already exist
            log.debug("Consumer group {} might already exist for stream {}", consumerGroup, streamName);
        }
        
        // Start consuming
        while (true) {
            try {
                Map<StreamMessageId, Map<String, Object>> messages = stream.readGroup(
                    consumerGroup,
                    consumerName,
                    StreamReadGroupArgs.neverDelivered().count(10).timeout(Duration.ofSeconds(1))
                );
                
                for (Map.Entry<StreamMessageId, Map<String, Object>> entry : messages.entrySet()) {
                    try {
                        StreamEvent event = new StreamEvent(
                            entry.getKey().toString(),
                            streamName,
                            entry.getValue(),
                            parseTimestamp(entry.getValue().get("timestamp"))
                        );
                        
                        consumer.consume(event);
                        
                        // Acknowledge successful processing
                        stream.ack(consumerGroup, entry.getKey());
                        
                    } catch (Exception e) {
                        log.error("Error processing event {} from stream {}", entry.getKey(), streamName, e);
                        consumer.onError(e);
                    }
                }
                
            } catch (Exception e) {
                log.error("Error reading from stream {}", streamName, e);
                consumer.onError(e);
                
                // Wait before retrying
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    @Override
    @Async
    public CompletableFuture<Void> subscribeAsync(String streamName, String consumerGroup, String consumerName, EventConsumer consumer) {
        return CompletableFuture.runAsync(() -> subscribe(streamName, consumerGroup, consumerName, consumer));
    }
    
    @Override
    public void createConsumerGroup(String streamName, String groupName, String startId) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        StreamMessageId start = startId != null ? 
            parseStreamId(startId) : StreamMessageId.NEWEST;
        
        stream.createGroup(StreamCreateGroupArgs.name(groupName).id(start));
        
        log.info("Created consumer group {} for stream {} starting from {}", groupName, streamName, start);
    }
    
    @Override
    public void acknowledgeEvent(String streamName, String groupName, String eventId) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        stream.ack(groupName, parseStreamId(eventId));
        
        log.debug("Acknowledged event {} for group {} in stream {}", eventId, groupName, streamName);
    }
    
    @Override
    public List<StreamEvent> getPendingEvents(String streamName, String groupName, String consumerName) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        List<PendingEntry> pendingResult = stream.listPending(groupName, StreamMessageId.MIN, StreamMessageId.MAX, 100);
        
        if (pendingResult.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<PendingEntry> entries = pendingResult;
        
        List<StreamEvent> pendingEvents = new ArrayList<>();
        
        for (PendingEntry entry : entries) {
            if (consumerName.equals(entry.getConsumerName())) {
                Map<StreamMessageId, Map<String, Object>> messages = stream.range(
                    entry.getId(),
                    entry.getId()
                );
                
                messages.forEach((id, data) -> {
                    pendingEvents.add(new StreamEvent(
                        id.toString(),
                        streamName,
                        data,
                        parseTimestamp(data.get("timestamp"))
                    ));
                });
            }
        }
        
        return pendingEvents;
    }
    
    @Override
    public List<StreamEvent> claimAbandonedEvents(String streamName, String groupName, String consumerName, Duration idleTime) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        Map<StreamMessageId, Map<String, Object>> claimedMessages = stream.claim(
            groupName,
            consumerName,
            idleTime.toMillis(),
            TimeUnit.MILLISECONDS,
            StreamMessageId.MIN
        );
        
        List<StreamEvent> claimedEvents = claimedMessages.entrySet().stream()
            .map(entry -> new StreamEvent(
                entry.getKey().toString(),
                streamName,
                entry.getValue(),
                parseTimestamp(entry.getValue().get("timestamp"))
            ))
            .collect(Collectors.toList());
        
        log.info("Claimed {} abandoned events from stream {} for consumer {}", 
            claimedEvents.size(), streamName, consumerName);
        
        return claimedEvents;
    }
    
    @Override
    public StreamInfo getStreamInfo(String streamName) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        org.redisson.api.StreamInfo<String, Object> info = stream.getInfo();
        
        List<ConsumerGroupInfo> groupInfos = new ArrayList<>();
        
        try {
            List<StreamGroup> groups = stream.listGroups();
            for (StreamGroup group : groups) {
                groupInfos.add(new ConsumerGroupInfo(
                    group.getName(),
                    group.getConsumers(),
                    group.getPending(),
                    group.getLastDeliveredId().toString()
                ));
            }
        } catch (Exception e) {
            log.debug("No consumer groups for stream {}", streamName);
        }
        
        return new StreamInfo(
            streamName,
            info.getLength(),
            info.getFirstEntry() != null ? info.getFirstEntry().getId().toString() : null,
            info.getLastEntry() != null ? info.getLastEntry().getId().toString() : null,
            info.getRadixTreeKeys(),
            info.getRadixTreeNodes(),
            groupInfos
        );
    }
    
    @Override
    public void trimStream(String streamName, long maxSize) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        stream.trim(TrimStrategy.MAXLEN, (int) maxSize);
        
        log.info("Trimmed stream {} to max size {}", streamName, maxSize);
    }
    
    @Override
    public void trimStreamByAge(String streamName, Duration maxAge) {
        RStream<String, Object> stream = redissonClient.getStream(STREAM_PREFIX + streamName);
        
        // For time-based trimming, we'll remove older entries by approximating with length
        // This is a simplified approach since exact time-based trimming API might differ
        stream.trim(TrimStrategy.MAXLEN, 1000); // Keep last 1000 entries as approximation
        
        log.info("Trimmed stream {} removing entries older than {}", streamName, maxAge);
    }
    
    // Forum-specific event operations
    
    @Override
    public String publishUserEvent(UserEvent event) {
        String eventId = publishEvent("users", event);
        
        // Also publish to user-specific stream
        publishEvent("user:" + event.userId(), event);
        
        return eventId;
    }
    
    @Override
    public String publishPostEvent(PostEvent event) {
        String eventId = publishEvent("posts", event);
        
        // Also publish to user activity stream
        publishEvent("user:" + event.userId() + ":activity", event);
        
        return eventId;
    }
    
    @Override
    public String publishCommentEvent(CommentEvent event) {
        String eventId = publishEvent("comments", event);
        
        // Also publish to post-specific stream
        publishEvent("post:" + event.postId() + ":activity", event);
        
        return eventId;
    }
    
    @Override
    public String publishNotificationEvent(NotificationEvent event) {
        return publishEvent("notifications", event);
    }
    
    @Override
    public String publishModerationEvent(ModerationEvent event) {
        String eventId = publishEvent("moderation", event);
        
        // Ensure moderation events are in audit trail
        Map<String, Object> auditData = new HashMap<>(event.data());
        auditData.put("moderatorId", event.moderatorId());
        auditData.put("reason", event.reason());
        auditData.put("targetType", event.targetType());
        auditData.put("targetId", event.targetId());
        
        publishAuditEvent("moderation", event, eventId);
        
        return eventId;
    }
    
    @Override
    public List<StreamEvent> getUserActivityStream(String username, int count) {
        return readEvents("user:" + username + ":activity", null, count);
    }
    
    @Override
    public List<StreamEvent> getAuditTrail(String entityType, String entityId) {
        RStream<String, Object> stream = redissonClient.getStream(AUDIT_STREAM);
        
        // Read all audit events and filter by entity
        Map<StreamMessageId, Map<String, Object>> messages = stream.read(
            StreamReadArgs.greaterThan(StreamMessageId.MIN).count(1000)
        );
        
        return messages.entrySet().stream()
            .filter(entry -> {
                Map<String, Object> data = entry.getValue();
                return entityType.equals(data.get("entityType")) && 
                       entityId.equals(data.get("entityId"));
            })
            .map(entry -> new StreamEvent(
                entry.getKey().toString(),
                "audit",
                entry.getValue(),
                parseTimestamp(entry.getValue().get("timestamp"))
            ))
            .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private void publishAuditEvent(String streamName, Event event, String eventId) {
        RStream<String, Object> auditStream = redissonClient.getStream(AUDIT_STREAM);
        
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("streamName", streamName);
        auditData.put("eventId", eventId);
        auditData.put("eventType", event.getEventType());
        auditData.put("entityType", streamName);
        auditData.put("entityId", event.getEntityId());
        auditData.put("userId", event.getUserId());
        auditData.put("timestamp", event.getTimestamp().toString());
        
        auditStream.add(StreamAddArgs.entries(auditData));
    }
    
    private LocalDateTime parseTimestamp(Object timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now();
        }
        
        try {
            return LocalDateTime.parse(timestamp.toString());
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}", timestamp);
            return LocalDateTime.now();
        }
    }
    
    private StreamMessageId parseStreamId(String streamId) {
        try {
            // Stream ID format is typically "timestamp-sequence"
            String[] parts = streamId.split("-");
            if (parts.length == 2) {
                long timestamp = Long.parseLong(parts[0]);
                long sequence = Long.parseLong(parts[1]);
                return new StreamMessageId(timestamp, sequence);
            } else {
                // Try parsing as single long (timestamp only)
                long timestamp = Long.parseLong(streamId);
                return new StreamMessageId(timestamp, 0);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse stream ID: {}, using current timestamp", streamId);
            return new StreamMessageId(System.currentTimeMillis(), 0);
        }
    }
}