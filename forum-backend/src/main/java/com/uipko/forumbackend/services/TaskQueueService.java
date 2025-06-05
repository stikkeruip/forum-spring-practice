package com.uipko.forumbackend.services;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing distributed task queues using Redis
 * Enables asynchronous processing of background jobs across multiple instances
 */
public interface TaskQueueService {
    
    /**
     * Submit a task to the queue
     */
    <T> String submitTask(String queueName, Task<T> task);
    
    /**
     * Submit a task with priority
     */
    <T> String submitTask(String queueName, Task<T> task, int priority);
    
    /**
     * Submit a delayed task
     */
    <T> String submitDelayedTask(String queueName, Task<T> task, Duration delay);
    
    /**
     * Submit a recurring task
     */
    <T> String submitRecurringTask(String queueName, Task<T> task, Duration interval);
    
    /**
     * Submit multiple tasks as a batch
     */
    <T> List<String> submitBatch(String queueName, List<Task<T>> tasks);
    
    /**
     * Process tasks from a queue
     */
    <T> void processQueue(String queueName, TaskProcessor<T> processor);
    
    /**
     * Process tasks with concurrency control
     */
    <T> void processQueue(String queueName, TaskProcessor<T> processor, int concurrency);
    
    /**
     * Get task status
     */
    TaskStatus getTaskStatus(String taskId);
    
    /**
     * Cancel a task
     */
    boolean cancelTask(String taskId);
    
    /**
     * Retry a failed task
     */
    boolean retryTask(String taskId);
    
    /**
     * Get queue statistics
     */
    QueueStatistics getQueueStatistics(String queueName);
    
    /**
     * Get all queues
     */
    List<String> getAllQueues();
    
    /**
     * Clear a queue
     */
    void clearQueue(String queueName);
    
    /**
     * Get failed tasks
     */
    List<FailedTask> getFailedTasks(String queueName);
    
    /**
     * Reprocess failed tasks
     */
    void reprocessFailedTasks(String queueName);
    
    // Forum-specific task operations
    
    /**
     * Submit email notification task
     */
    String submitEmailNotification(String recipient, String subject, String content);
    
    /**
     * Submit post indexing task
     */
    String submitPostIndexing(Long postId);
    
    /**
     * Submit user activity aggregation task
     */
    String submitUserActivityAggregation(String username);
    
    /**
     * Submit cache warming task
     */
    String submitCacheWarmingTask(String cacheType);
    
    /**
     * Submit cleanup task
     */
    String submitCleanupTask(String cleanupType, Map<String, Object> parameters);
    
    /**
     * Submit analytics task
     */
    String submitAnalyticsTask(String eventType, Map<String, Object> data);
    
    /**
     * Task definition
     */
    interface Task<T> {
        String getId();
        String getType();
        T getPayload();
        int getMaxRetries();
        Duration getTimeout();
        Map<String, Object> getMetadata();
    }
    
    /**
     * Task processor interface
     */
    interface TaskProcessor<T> {
        void process(Task<T> task) throws Exception;
        void onSuccess(Task<T> task, Object result);
        void onFailure(Task<T> task, Exception error);
        boolean shouldRetry(Task<T> task, Exception error);
    }
    
    /**
     * Task status
     */
    record TaskStatus(
        String taskId,
        String queueName,
        String status, // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
        String submittedAt,
        String startedAt,
        String completedAt,
        int attemptCount,
        String error,
        Map<String, Object> result
    ) {}
    
    /**
     * Queue statistics
     */
    record QueueStatistics(
        String queueName,
        long pendingTasks,
        long processingTasks,
        long completedTasks,
        long failedTasks,
        long cancelledTasks,
        double averageProcessingTime,
        double successRate,
        long oldestTaskAge,
        Map<String, Long> tasksByType
    ) {}
    
    /**
     * Failed task information
     */
    record FailedTask(
        String taskId,
        String taskType,
        Object payload,
        String failedAt,
        int attemptCount,
        String lastError,
        Map<String, Object> metadata
    ) {}
}