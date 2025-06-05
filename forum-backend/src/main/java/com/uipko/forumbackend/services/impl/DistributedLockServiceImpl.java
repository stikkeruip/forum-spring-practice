package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
@Slf4j
public class DistributedLockServiceImpl implements DistributedLockService {
    
    private final RedissonClient redissonClient;
    
    // Default lock timeouts
    private static final Duration DEFAULT_WAIT_TIME = Duration.ofSeconds(10);
    private static final Duration DEFAULT_LEASE_TIME = Duration.ofMinutes(5);
    
    // Lock key prefixes
    private static final String LOCK_PREFIX = "forum:lock:";
    private static final String FAIR_LOCK_PREFIX = "forum:fairlock:";
    private static final String RW_LOCK_PREFIX = "forum:rwlock:";
    
    // Statistics tracking
    private final AtomicLong totalAcquisitions = new AtomicLong(0);
    private final AtomicLong successfulAcquisitions = new AtomicLong(0);
    private final AtomicLong failedAcquisitions = new AtomicLong(0);
    private final AtomicLong deadlockDetections = new AtomicLong(0);
    private final AtomicLong forcedUnlocks = new AtomicLong(0);
    
    public DistributedLockServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        totalAcquisitions.incrementAndGet();
        
        try {
            if (lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
                successfulAcquisitions.incrementAndGet();
                log.debug("Acquired lock for key: {}", lockKey);
                
                try {
                    return operation.call();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Released lock for key: {}", lockKey);
                    }
                }
            } else {
                failedAcquisitions.incrementAndGet();
                log.warn("Failed to acquire lock for key: {} within {} ms", lockKey, waitTime.toMillis());
                throw new RuntimeException("Failed to acquire lock for key: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            throw new RuntimeException("Lock acquisition interrupted", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error executing operation with lock", e);
        }
    }
    
    @Override
    public <T> T executeWithLock(String lockKey, Callable<T> operation) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, operation);
    }
    
    @Override
    @Async
    public <T> CompletableFuture<T> executeWithLockAsync(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation) {
        return CompletableFuture.supplyAsync(() -> executeWithLock(lockKey, waitTime, leaseTime, operation));
    }
    
    @Override
    public boolean tryLock(String lockKey, Duration leaseTime) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        totalAcquisitions.incrementAndGet();
        
        try {
            boolean acquired = lock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (acquired) {
                successfulAcquisitions.incrementAndGet();
                log.debug("Immediately acquired lock for key: {}", lockKey);
            } else {
                failedAcquisitions.incrementAndGet();
                log.debug("Lock already held for key: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while trying lock: {}", lockKey, e);
            return false;
        }
    }
    
    @Override
    public boolean tryLock(String lockKey, Duration waitTime, Duration leaseTime) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        totalAcquisitions.incrementAndGet();
        
        try {
            boolean acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (acquired) {
                successfulAcquisitions.incrementAndGet();
                log.debug("Acquired lock for key: {} after waiting", lockKey);
            } else {
                failedAcquisitions.incrementAndGet();
                log.debug("Failed to acquire lock for key: {} within timeout", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            return false;
        }
    }
    
    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Manually released lock for key: {}", lockKey);
        } else {
            log.warn("Attempted to unlock key: {} not held by current thread", lockKey);
        }
    }
    
    @Override
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        return lock.isLocked();
    }
    
    @Override
    public void forceUnlock(String lockKey) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        if (lock.isLocked()) {
            lock.forceUnlock();
            forcedUnlocks.incrementAndGet();
            log.warn("Force unlocked key: {}", lockKey);
        }
    }
    
    @Override
    public <T> T executeWithOptimisticLock(String lockKey, Supplier<T> operation, int maxRetries) {
        int attempts = 0;
        
        while (attempts < maxRetries) {
            attempts++;
            
            try {
                // Try to execute without lock first
                T result = operation.get();
                
                // If successful, try to acquire lock briefly to ensure consistency
                if (tryLock(lockKey, Duration.ofMillis(100))) {
                    try {
                        // Re-execute to ensure consistency
                        result = operation.get();
                        return result;
                    } finally {
                        unlock(lockKey);
                    }
                }
            } catch (Exception e) {
                if (attempts >= maxRetries) {
                    throw new RuntimeException("Optimistic lock failed after " + maxRetries + " attempts", e);
                }
                log.debug("Optimistic lock attempt {} failed, retrying...", attempts);
                
                // Wait briefly before retry
                try {
                    Thread.sleep(50 * attempts); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during optimistic lock retry", ie);
                }
            }
        }
        
        throw new RuntimeException("Optimistic lock failed after " + maxRetries + " attempts");
    }
    
    @Override
    public <T> T executeWithFairLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation) {
        RLock fairLock = redissonClient.getFairLock(FAIR_LOCK_PREFIX + lockKey);
        totalAcquisitions.incrementAndGet();
        
        try {
            if (fairLock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
                successfulAcquisitions.incrementAndGet();
                log.debug("Acquired fair lock for key: {}", lockKey);
                
                try {
                    return operation.call();
                } finally {
                    if (fairLock.isHeldByCurrentThread()) {
                        fairLock.unlock();
                        log.debug("Released fair lock for key: {}", lockKey);
                    }
                }
            } else {
                failedAcquisitions.incrementAndGet();
                log.warn("Failed to acquire fair lock for key: {}", lockKey);
                throw new RuntimeException("Failed to acquire fair lock for key: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for fair lock: {}", lockKey, e);
            throw new RuntimeException("Fair lock acquisition interrupted", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error executing operation with fair lock", e);
        }
    }
    
    @Override
    public <T> T executeWithReadLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(RW_LOCK_PREFIX + lockKey);
        RLock readLock = rwLock.readLock();
        
        try {
            if (readLock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
                log.debug("Acquired read lock for key: {}", lockKey);
                
                try {
                    return operation.call();
                } finally {
                    if (readLock.isHeldByCurrentThread()) {
                        readLock.unlock();
                        log.debug("Released read lock for key: {}", lockKey);
                    }
                }
            } else {
                log.warn("Failed to acquire read lock for key: {}", lockKey);
                throw new RuntimeException("Failed to acquire read lock for key: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for read lock: {}", lockKey, e);
            throw new RuntimeException("Read lock acquisition interrupted", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error executing operation with read lock", e);
        }
    }
    
    @Override
    public <T> T executeWithWriteLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> operation) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(RW_LOCK_PREFIX + lockKey);
        RLock writeLock = rwLock.writeLock();
        
        try {
            if (writeLock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
                log.debug("Acquired write lock for key: {}", lockKey);
                
                try {
                    return operation.call();
                } finally {
                    if (writeLock.isHeldByCurrentThread()) {
                        writeLock.unlock();
                        log.debug("Released write lock for key: {}", lockKey);
                    }
                }
            } else {
                log.warn("Failed to acquire write lock for key: {}", lockKey);
                throw new RuntimeException("Failed to acquire write lock for key: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for write lock: {}", lockKey, e);
            throw new RuntimeException("Write lock acquisition interrupted", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error executing operation with write lock", e);
        }
    }
    
    @Override
    public LockStatistics getLockStatistics() {
        long currentlyHeld = redissonClient.getKeys().count();
        
        return new LockStatistics(
            totalAcquisitions.get(),
            successfulAcquisitions.get(),
            failedAcquisitions.get(),
            currentlyHeld,
            0.0, // Would need additional tracking
            0.0, // Would need additional tracking
            deadlockDetections.get(),
            forcedUnlocks.get()
        );
    }
    
    // Lock-specific operations for forum actions
    
    @Override
    public <T> T executePostCreation(String username, Callable<T> operation) {
        String lockKey = "post:create:" + username;
        // Prevent rapid post creation from same user
        return executeWithLock(lockKey, Duration.ofSeconds(5), Duration.ofSeconds(30), operation);
    }
    
    @Override
    public <T> T executeCommentAddition(Long postId, String username, Callable<T> operation) {
        String lockKey = "comment:add:" + postId + ":" + username;
        // Prevent duplicate comments
        return executeWithLock(lockKey, Duration.ofSeconds(3), Duration.ofSeconds(10), operation);
    }
    
    @Override
    public <T> T executeFriendOperation(String user1, String user2, Callable<T> operation) {
        // Always lock in consistent order to prevent deadlocks
        String lockKey = user1.compareTo(user2) < 0 ? 
            "friend:" + user1 + ":" + user2 : 
            "friend:" + user2 + ":" + user1;
        
        return executeWithFairLock(lockKey, Duration.ofSeconds(5), Duration.ofSeconds(30), operation);
    }
    
    @Override
    public <T> T executeReactionUpdate(String targetType, Long targetId, String username, Callable<T> operation) {
        String lockKey = "reaction:" + targetType + ":" + targetId + ":" + username;
        // Quick lock for reaction updates
        return executeWithLock(lockKey, Duration.ofSeconds(2), Duration.ofSeconds(5), operation);
    }
    
    @Override
    public <T> T executeNotificationProcessing(String username, Callable<T> operation) {
        String lockKey = "notification:process:" + username;
        // Ensure notifications are processed sequentially per user
        return executeWithFairLock(lockKey, Duration.ofSeconds(10), Duration.ofMinutes(1), operation);
    }
}