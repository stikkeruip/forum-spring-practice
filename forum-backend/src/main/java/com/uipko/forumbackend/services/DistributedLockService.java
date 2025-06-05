package com.uipko.forumbackend.services;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service for distributed locking across multiple application instances
 * Ensures thread-safe operations in a distributed environment
 */
public interface DistributedLockService {
    
    /**
     * Acquire a lock and execute the provided operation
     * 
     * @param lockKey The unique key for the lock
     * @param waitTime Maximum time to wait for lock acquisition
     * @param leaseTime Time after which lock is automatically released
     * @param operation The operation to execute while holding the lock
     * @return Result of the operation
     */
    <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation);
    
    /**
     * Acquire a lock and execute the provided operation with default timeouts
     */
    <T> T executeWithLock(String lockKey, Callable<T> operation);
    
    /**
     * Acquire a lock and execute asynchronously
     */
    <T> CompletableFuture<T> executeWithLockAsync(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation);
    
    /**
     * Try to acquire a lock without waiting
     */
    boolean tryLock(String lockKey, Duration leaseTime);
    
    /**
     * Try to acquire a lock with wait time
     */
    boolean tryLock(String lockKey, Duration waitTime, Duration leaseTime);
    
    /**
     * Release a lock manually
     */
    void unlock(String lockKey);
    
    /**
     * Check if a lock is currently held
     */
    boolean isLocked(String lockKey);
    
    /**
     * Force unlock a lock (use with caution)
     */
    void forceUnlock(String lockKey);
    
    /**
     * Execute operation with optimistic locking pattern
     */
    <T> T executeWithOptimisticLock(String lockKey, Supplier<T> operation, int maxRetries);
    
    /**
     * Create a fair lock that guarantees order
     */
    <T> T executeWithFairLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation);
    
    /**
     * Execute with read lock (multiple readers allowed)
     */
    <T> T executeWithReadLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation);
    
    /**
     * Execute with write lock (exclusive access)
     */
    <T> T executeWithWriteLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation);
    
    /**
     * Get lock statistics
     */
    LockStatistics getLockStatistics();
    
    /**
     * Lock-specific operations for common forum actions
     */
    
    /**
     * Lock for post creation to prevent duplicates
     */
    <T> T executePostCreation(String username, Callable<T> operation);
    
    /**
     * Lock for comment addition to ensure consistency
     */
    <T> T executeCommentAddition(Long postId, String username, Callable<T> operation);
    
    /**
     * Lock for friend request operations
     */
    <T> T executeFriendOperation(String user1, String user2, Callable<T> operation);
    
    /**
     * Lock for reaction updates to prevent race conditions
     */
    <T> T executeReactionUpdate(String targetType, Long targetId, String username, Callable<T> operation);
    
    /**
     * Lock for notification processing
     */
    <T> T executeNotificationProcessing(String username, Callable<T> operation);
    
    /**
     * Lock statistics
     */
    record LockStatistics(
        long totalLockAcquisitions,
        long successfulAcquisitions,
        long failedAcquisitions,
        long currentlyHeldLocks,
        double averageWaitTime,
        double averageHoldTime,
        long deadlockDetections,
        long forcedUnlocks
    ) {}
}