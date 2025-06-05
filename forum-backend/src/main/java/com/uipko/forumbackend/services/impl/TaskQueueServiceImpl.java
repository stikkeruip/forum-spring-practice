package com.uipko.forumbackend.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipko.forumbackend.services.TaskQueueService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskQueueServiceImpl implements TaskQueueService {
    
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final Map<String, AtomicBoolean> processingFlags = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Redis key patterns
    private static final String QUEUE_PREFIX = "forum:queue:";
    private static final String DELAYED_QUEUE_PREFIX = "forum:delayed:";
    private static final String TASK_STATUS_PREFIX = "forum:task:status:";
    private static final String QUEUE_STATS_PREFIX = "forum:queue:stats:";
    private static final String FAILED_TASKS_PREFIX = "forum:queue:failed:";
    
    public TaskQueueServiceImpl(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public <T> String submitTask(String queueName, Task<T> task) {
        return submitTask(queueName, task, 0);
    }
    
    @Override
    public <T> String submitTask(String queueName, Task<T> task, int priority) {
        try {
            String taskId = generateTaskId();
            TaskWrapper<T> wrapper = new TaskWrapper<>(taskId, queueName, task, priority);
            
            // Store task status
            storeTaskStatus(taskId, queueName, "PENDING");
            
            // Add to priority queue
            if (priority > 0) {
                RPriorityQueue<String> priorityQueue = redissonClient.getPriorityQueue(QUEUE_PREFIX + queueName);
                String taskJson = objectMapper.writeValueAsString(wrapper);
                priorityQueue.add(taskJson);
            } else {
                // Add to regular queue
                RQueue<String> queue = redissonClient.getQueue(QUEUE_PREFIX + queueName);
                String taskJson = objectMapper.writeValueAsString(wrapper);
                queue.offer(taskJson);
            }
            
            // Update queue statistics
            updateQueueStats(queueName, "submitted");
            
            log.debug("Submitted task {} to queue {} with priority {}", taskId, queueName, priority);
            return taskId;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task for queue {}", queueName, e);
            throw new RuntimeException("Failed to submit task", e);
        }
    }
    
    @Override
    public <T> String submitDelayedTask(String queueName, Task<T> task, Duration delay) {
        try {
            String taskId = generateTaskId();
            TaskWrapper<T> wrapper = new TaskWrapper<>(taskId, queueName, task, 0);
            wrapper.setScheduledTime(LocalDateTime.now().plus(delay));
            
            // Store in delayed queue
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(
                redissonClient.getQueue(DELAYED_QUEUE_PREFIX + queueName)
            );
            
            String taskJson = objectMapper.writeValueAsString(wrapper);
            delayedQueue.offer(taskJson, delay.toMillis(), TimeUnit.MILLISECONDS);
            
            // Store task status
            storeTaskStatus(taskId, queueName, "SCHEDULED");
            
            log.debug("Submitted delayed task {} to queue {} with delay {}", taskId, queueName, delay);
            return taskId;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize delayed task for queue {}", queueName, e);
            throw new RuntimeException("Failed to submit delayed task", e);
        }
    }
    
    @Override
    public <T> String submitRecurringTask(String queueName, Task<T> task, Duration interval) {
        try {
            String taskId = generateTaskId();
            TaskWrapper<T> wrapper = new TaskWrapper<>(taskId, queueName, task, 0);
            wrapper.setRecurring(true);
            wrapper.setRecurringInterval(interval);
            
            // Schedule recurring task
            RScheduledExecutorService scheduler = redissonClient.getExecutorService("forum:scheduler");
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    submitTask(queueName, task);
                } catch (Exception e) {
                    log.error("Failed to submit recurring task {}", taskId, e);
                }
            }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
            
            log.debug("Submitted recurring task {} to queue {} with interval {}", taskId, queueName, interval);
            return taskId;
            
        } catch (Exception e) {
            log.error("Failed to submit recurring task for queue {}", queueName, e);
            throw new RuntimeException("Failed to submit recurring task", e);
        }
    }
    
    @Override
    public <T> List<String> submitBatch(String queueName, List<Task<T>> tasks) {
        List<String> taskIds = new ArrayList<>();
        
        RBatch batch = redissonClient.createBatch();
        RQueueAsync<String> queueAsync = batch.getQueue(QUEUE_PREFIX + queueName);
        
        for (Task<T> task : tasks) {
            try {
                String taskId = generateTaskId();
                TaskWrapper<T> wrapper = new TaskWrapper<>(taskId, queueName, task, 0);
                String taskJson = objectMapper.writeValueAsString(wrapper);
                
                queueAsync.offerAsync(taskJson);
                taskIds.add(taskId);
                
                // Store task status
                storeTaskStatus(taskId, queueName, "PENDING");
                
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize task in batch for queue {}", queueName, e);
            }
        }
        
        batch.execute();
        updateQueueStats(queueName, "batch_submitted", tasks.size());
        
        log.debug("Submitted batch of {} tasks to queue {}", tasks.size(), queueName);
        return taskIds;
    }
    
    @Override
    @Async
    public <T> void processQueue(String queueName, TaskProcessor<T> processor) {
        processQueue(queueName, processor, 1);
    }
    
    @Override
    @Async
    public <T> void processQueue(String queueName, TaskProcessor<T> processor, int concurrency) {
        AtomicBoolean processing = processingFlags.computeIfAbsent(queueName, k -> new AtomicBoolean(false));
        
        if (!processing.compareAndSet(false, true)) {
            log.warn("Queue {} is already being processed", queueName);
            return;
        }
        
        log.info("Starting processing queue {} with concurrency {}", queueName, concurrency);
        
        ExecutorService queueExecutor = Executors.newFixedThreadPool(concurrency);
        
        try {
            RQueue<String> queue = redissonClient.getQueue(QUEUE_PREFIX + queueName);
            
            while (processing.get()) {
                String taskJson = queue.poll();
                
                if (taskJson == null) {
                    // No tasks available, wait briefly
                    Thread.sleep(100);
                    continue;
                }
                
                queueExecutor.submit(() -> processTask(taskJson, processor));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Queue processing interrupted for {}", queueName, e);
        } finally {
            processing.set(false);
            queueExecutor.shutdown();
            log.info("Stopped processing queue {}", queueName);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> void processTask(String taskJson, TaskProcessor<T> processor) {
        TaskWrapper<T> wrapper = null;
        
        try {
            wrapper = objectMapper.readValue(taskJson, TaskWrapper.class);
            Task<T> task = wrapper.getTask();
            
            // Update task status
            storeTaskStatus(wrapper.getTaskId(), wrapper.getQueueName(), "PROCESSING");
            updateQueueStats(wrapper.getQueueName(), "processing");
            
            long startTime = System.currentTimeMillis();
            
            // Process the task
            processor.process(task);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Task completed successfully
            storeTaskStatus(wrapper.getTaskId(), wrapper.getQueueName(), "COMPLETED");
            updateQueueStats(wrapper.getQueueName(), "completed", processingTime);
            
            processor.onSuccess(task, null);
            
            log.debug("Successfully processed task {} in {} ms", wrapper.getTaskId(), processingTime);
            
        } catch (Exception e) {
            log.error("Failed to process task", e);
            
            if (wrapper != null) {
                handleTaskFailure(wrapper, processor, e);
            }
        }
    }
    
    private <T> void handleTaskFailure(TaskWrapper<T> wrapper, TaskProcessor<T> processor, Exception error) {
        try {
            Task<T> task = wrapper.getTask();
            wrapper.incrementAttemptCount();
            
            boolean shouldRetry = processor.shouldRetry(task, error) && 
                                wrapper.getAttemptCount() < task.getMaxRetries();
            
            if (shouldRetry) {
                // Requeue the task
                String taskJson = objectMapper.writeValueAsString(wrapper);
                RQueue<String> queue = redissonClient.getQueue(QUEUE_PREFIX + wrapper.getQueueName());
                queue.offer(taskJson);
                
                storeTaskStatus(wrapper.getTaskId(), wrapper.getQueueName(), "RETRYING");
                log.debug("Retrying task {} (attempt {})", wrapper.getTaskId(), wrapper.getAttemptCount());
                
            } else {
                // Task failed permanently
                storeTaskStatus(wrapper.getTaskId(), wrapper.getQueueName(), "FAILED", error.getMessage());
                storeFailedTask(wrapper, error);
                updateQueueStats(wrapper.getQueueName(), "failed");
                
                processor.onFailure(task, error);
                log.error("Task {} failed permanently after {} attempts", 
                    wrapper.getTaskId(), wrapper.getAttemptCount());
            }
            
        } catch (JsonProcessingException e) {
            log.error("Failed to handle task failure", e);
        }
    }
    
    @Override
    public TaskStatus getTaskStatus(String taskId) {
        RMap<String, Object> statusMap = redissonClient.getMap(TASK_STATUS_PREFIX + taskId);
        
        if (statusMap.isEmpty()) {
            return null;
        }
        
        return new TaskStatus(
            taskId,
            (String) statusMap.get("queueName"),
            (String) statusMap.get("status"),
            (String) statusMap.get("submittedAt"),
            (String) statusMap.get("startedAt"),
            (String) statusMap.get("completedAt"),
            (Integer) statusMap.getOrDefault("attemptCount", 0),
            (String) statusMap.get("error"),
            (Map<String, Object>) statusMap.get("result")
        );
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        TaskStatus status = getTaskStatus(taskId);
        
        if (status == null || !"PENDING".equals(status.status())) {
            return false;
        }
        
        storeTaskStatus(taskId, status.queueName(), "CANCELLED");
        updateQueueStats(status.queueName(), "cancelled");
        
        log.debug("Cancelled task {}", taskId);
        return true;
    }
    
    @Override
    public boolean retryTask(String taskId) {
        TaskStatus status = getTaskStatus(taskId);
        
        if (status == null || !"FAILED".equals(status.status())) {
            return false;
        }
        
        // Get failed task and requeue
        RMap<String, Object> failedTaskMap = redissonClient.getMap(FAILED_TASKS_PREFIX + status.queueName() + ":" + taskId);
        
        if (!failedTaskMap.isEmpty()) {
            String taskJson = (String) failedTaskMap.get("taskJson");
            if (taskJson != null) {
                RQueue<String> queue = redissonClient.getQueue(QUEUE_PREFIX + status.queueName());
                queue.offer(taskJson);
                
                storeTaskStatus(taskId, status.queueName(), "PENDING");
                failedTaskMap.delete();
                
                log.debug("Retried task {}", taskId);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public QueueStatistics getQueueStatistics(String queueName) {
        RMap<String, Object> statsMap = redissonClient.getMap(QUEUE_STATS_PREFIX + queueName);
        
        long pending = ((Number) statsMap.getOrDefault("pending", 0L)).longValue();
        long processing = ((Number) statsMap.getOrDefault("processing", 0L)).longValue();
        long completed = ((Number) statsMap.getOrDefault("completed", 0L)).longValue();
        long failed = ((Number) statsMap.getOrDefault("failed", 0L)).longValue();
        long cancelled = ((Number) statsMap.getOrDefault("cancelled", 0L)).longValue();
        
        double avgProcessingTime = ((Number) statsMap.getOrDefault("avgProcessingTime", 0.0)).doubleValue();
        double successRate = (completed + failed) > 0 ? 
            (double) completed / (completed + failed) : 0.0;
        
        return new QueueStatistics(
            queueName,
            pending,
            processing,
            completed,
            failed,
            cancelled,
            avgProcessingTime,
            successRate,
            0L, // Would need additional tracking
            new HashMap<>() // Would need additional tracking
        );
    }
    
    @Override
    public List<String> getAllQueues() {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(QUEUE_PREFIX + "*");
        List<String> queues = new ArrayList<>();
        
        for (String key : keys) {
            queues.add(key.replace(QUEUE_PREFIX, ""));
        }
        
        return queues;
    }
    
    @Override
    public void clearQueue(String queueName) {
        RQueue<String> queue = redissonClient.getQueue(QUEUE_PREFIX + queueName);
        queue.clear();
        
        log.info("Cleared queue {}", queueName);
    }
    
    @Override
    public List<FailedTask> getFailedTasks(String queueName) {
        List<FailedTask> failedTasks = new ArrayList<>();
        
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(FAILED_TASKS_PREFIX + queueName + ":*");
        
        for (String key : keys) {
            RMap<String, Object> failedTaskMap = redissonClient.getMap(key);
            
            failedTasks.add(new FailedTask(
                (String) failedTaskMap.get("taskId"),
                (String) failedTaskMap.get("taskType"),
                failedTaskMap.get("payload"),
                (String) failedTaskMap.get("failedAt"),
                ((Number) failedTaskMap.getOrDefault("attemptCount", 0)).intValue(),
                (String) failedTaskMap.get("lastError"),
                (Map<String, Object>) failedTaskMap.get("metadata")
            ));
        }
        
        return failedTasks;
    }
    
    @Override
    public void reprocessFailedTasks(String queueName) {
        List<FailedTask> failedTasks = getFailedTasks(queueName);
        
        for (FailedTask failedTask : failedTasks) {
            retryTask(failedTask.taskId());
        }
        
        log.info("Reprocessed {} failed tasks for queue {}", failedTasks.size(), queueName);
    }
    
    // Forum-specific task operations
    
    @Override
    public String submitEmailNotification(String recipient, String subject, String content) {
        Map<String, Object> payload = Map.of(
            "recipient", recipient,
            "subject", subject,
            "content", content
        );
        
        Task<Map<String, Object>> task = new SimpleTask<>(
            "EMAIL_NOTIFICATION",
            payload,
            3,
            Duration.ofMinutes(5)
        );
        
        return submitTask("email", task);
    }
    
    @Override
    public String submitPostIndexing(Long postId) {
        Task<Long> task = new SimpleTask<>(
            "POST_INDEXING",
            postId,
            3,
            Duration.ofMinutes(2)
        );
        
        return submitTask("indexing", task);
    }
    
    @Override
    public String submitUserActivityAggregation(String username) {
        Task<String> task = new SimpleTask<>(
            "USER_ACTIVITY_AGGREGATION",
            username,
            2,
            Duration.ofMinutes(10)
        );
        
        return submitTask("analytics", task);
    }
    
    @Override
    public String submitCacheWarmingTask(String cacheType) {
        Task<String> task = new SimpleTask<>(
            "CACHE_WARMING",
            cacheType,
            1,
            Duration.ofMinutes(15)
        );
        
        return submitTask("maintenance", task, 1); // Low priority
    }
    
    @Override
    public String submitCleanupTask(String cleanupType, Map<String, Object> parameters) {
        Map<String, Object> payload = new HashMap<>(parameters);
        payload.put("cleanupType", cleanupType);
        
        Task<Map<String, Object>> task = new SimpleTask<>(
            "CLEANUP",
            payload,
            1,
            Duration.ofMinutes(30)
        );
        
        return submitDelayedTask("maintenance", task, Duration.ofHours(1));
    }
    
    @Override
    public String submitAnalyticsTask(String eventType, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>(data);
        payload.put("eventType", eventType);
        payload.put("timestamp", LocalDateTime.now().toString());
        
        Task<Map<String, Object>> task = new SimpleTask<>(
            "ANALYTICS_EVENT",
            payload,
            5,
            Duration.ofMinutes(5)
        );
        
        return submitTask("analytics", task);
    }
    
    // Helper methods
    
    private String generateTaskId() {
        return "task_" + UUID.randomUUID().toString();
    }
    
    private void storeTaskStatus(String taskId, String queueName, String status) {
        storeTaskStatus(taskId, queueName, status, null);
    }
    
    private void storeTaskStatus(String taskId, String queueName, String status, String error) {
        RMap<String, Object> statusMap = redissonClient.getMap(TASK_STATUS_PREFIX + taskId);
        
        statusMap.put("taskId", taskId);
        statusMap.put("queueName", queueName);
        statusMap.put("status", status);
        
        if ("PENDING".equals(status)) {
            statusMap.put("submittedAt", LocalDateTime.now().toString());
        } else if ("PROCESSING".equals(status)) {
            statusMap.put("startedAt", LocalDateTime.now().toString());
        } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            statusMap.put("completedAt", LocalDateTime.now().toString());
        }
        
        if (error != null) {
            statusMap.put("error", error);
        }
        
        statusMap.expire(Duration.ofDays(7)); // Keep status for 7 days
    }
    
    private void updateQueueStats(String queueName, String operation) {
        updateQueueStats(queueName, operation, 1);
    }
    
    private void updateQueueStats(String queueName, String operation, long value) {
        RMap<String, Object> statsMap = redissonClient.getMap(QUEUE_STATS_PREFIX + queueName);
        
        switch (operation) {
            case "submitted":
                statsMap.merge("pending", 1L, (a, b) -> ((Long) a) + ((Long) b));
                break;
            case "processing":
                statsMap.merge("pending", -1L, (a, b) -> Math.max(0, ((Long) a) + ((Long) b)));
                statsMap.merge("processing", 1L, (a, b) -> ((Long) a) + ((Long) b));
                break;
            case "completed":
                statsMap.merge("processing", -1L, (a, b) -> Math.max(0, ((Long) a) + ((Long) b)));
                statsMap.merge("completed", 1L, (a, b) -> ((Long) a) + ((Long) b));
                // Update average processing time
                if (value > 0) {
                    updateAverageProcessingTime(statsMap, value);
                }
                break;
            case "failed":
                statsMap.merge("processing", -1L, (a, b) -> Math.max(0, ((Long) a) + ((Long) b)));
                statsMap.merge("failed", 1L, (a, b) -> ((Long) a) + ((Long) b));
                break;
            case "cancelled":
                statsMap.merge("pending", -1L, (a, b) -> Math.max(0, ((Long) a) + ((Long) b)));
                statsMap.merge("cancelled", 1L, (a, b) -> ((Long) a) + ((Long) b));
                break;
            case "batch_submitted":
                statsMap.merge("pending", value, (a, b) -> ((Long) a) + ((Long) b));
                break;
        }
    }
    
    private void updateAverageProcessingTime(RMap<String, Object> statsMap, long processingTime) {
        double currentAvg = ((Number) statsMap.getOrDefault("avgProcessingTime", 0.0)).doubleValue();
        long totalProcessed = ((Number) statsMap.getOrDefault("completed", 0L)).longValue();
        
        double newAvg = (currentAvg * (totalProcessed - 1) + processingTime) / totalProcessed;
        statsMap.put("avgProcessingTime", newAvg);
    }
    
    private <T> void storeFailedTask(TaskWrapper<T> wrapper, Exception error) {
        RMap<String, Object> failedTaskMap = redissonClient.getMap(
            FAILED_TASKS_PREFIX + wrapper.getQueueName() + ":" + wrapper.getTaskId()
        );
        
        failedTaskMap.put("taskId", wrapper.getTaskId());
        failedTaskMap.put("taskType", wrapper.getTask().getType());
        failedTaskMap.put("payload", wrapper.getTask().getPayload());
        failedTaskMap.put("failedAt", LocalDateTime.now().toString());
        failedTaskMap.put("attemptCount", wrapper.getAttemptCount());
        failedTaskMap.put("lastError", error.getMessage());
        failedTaskMap.put("metadata", wrapper.getTask().getMetadata());
        
        try {
            failedTaskMap.put("taskJson", objectMapper.writeValueAsString(wrapper));
        } catch (JsonProcessingException e) {
            log.error("Failed to store failed task JSON", e);
        }
        
        failedTaskMap.expire(Duration.ofDays(30)); // Keep failed tasks for 30 days
    }
    
    // Helper classes
    
    @Data
    private static class TaskWrapper<T> {
        private final String taskId;
        private final String queueName;
        private final Task<T> task;
        private final int priority;
        private int attemptCount = 0;
        private LocalDateTime scheduledTime;
        private boolean recurring = false;
        private Duration recurringInterval;
        
        public TaskWrapper(String taskId, String queueName, Task<T> task, int priority) {
            this.taskId = taskId;
            this.queueName = queueName;
            this.task = task;
            this.priority = priority;
        }
        
        public void incrementAttemptCount() {
            this.attemptCount++;
        }
    }
    
    @Data
    private static class SimpleTask<T> implements Task<T> {
        private final String id = UUID.randomUUID().toString();
        private final String type;
        private final T payload;
        private final int maxRetries;
        private final Duration timeout;
        private final Map<String, Object> metadata = new HashMap<>();
        
        public SimpleTask(String type, T payload, int maxRetries, Duration timeout) {
            this.type = type;
            this.payload = payload;
            this.maxRetries = maxRetries;
            this.timeout = timeout;
        }
    }
}