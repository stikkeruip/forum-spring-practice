package com.uipko.forumbackend.config;

import com.uipko.forumbackend.services.AuthenticationCacheService;
import com.uipko.forumbackend.services.CacheWarmingService;
import com.uipko.forumbackend.services.RealTimeDataCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handles cache warming during application startup with duplicate execution prevention
 */
@Component
@Slf4j
public class CacheWarmUpEventListener {
    
    private final CacheWarmingService cacheWarmingService;
    private final AuthenticationCacheService authCacheService;
    private final RealTimeDataCacheService realTimeCacheService;
    
    // Synchronization flag to prevent duplicate execution
    private volatile boolean warmUpInProgress = false;
    private volatile boolean warmUpCompleted = false;
    
    public CacheWarmUpEventListener(CacheWarmingService cacheWarmingService,
                                  AuthenticationCacheService authCacheService,
                                  RealTimeDataCacheService realTimeCacheService) {
        this.cacheWarmingService = cacheWarmingService;
        this.authCacheService = authCacheService;
        this.realTimeCacheService = realTimeCacheService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void onApplicationReady() {
        // Prevent duplicate execution using double-checked locking pattern
        if (warmUpCompleted) {
            log.debug("Cache warm-up already completed, skipping duplicate execution");
            return;
        }
        
        if (warmUpInProgress) {
            log.debug("Cache warm-up already in progress, skipping duplicate execution");
            return;
        }
        
        warmUpInProgress = true;
        log.info("Application ready - starting initial cache warm-up (execution ID: {})", 
            Thread.currentThread().getName());
        
        try {
            // Start initial cache warm-up asynchronously (only once on startup)
            cacheWarmingService.warmUpAllCaches()
                .thenRun(() -> {
                    synchronized (this) {
                        warmUpInProgress = false;
                        warmUpCompleted = true;
                        log.info("Initial cache warm-up completed successfully - further duplicates will be prevented");
                        
                        // Initialize periodic scheduling after initial warm-up
                        if (cacheWarmingService instanceof com.uipko.forumbackend.services.impl.CacheWarmingServiceImpl impl) {
                            impl.initializePeriodicWarmUp();
                        }
                    }
                })
                .exceptionally(throwable -> {
                    synchronized (this) {
                        warmUpInProgress = false;
                        // Don't mark as completed on failure - allow retry
                        log.error("Initial cache warm-up failed", throwable);
                    }
                    return null;
                });
            
        } catch (Exception e) {
            synchronized (this) {
                warmUpInProgress = false;
                log.error("Failed to initialize cache warm-up", e);
            }
        }
    }
    
    /**
     * Check if cache warm-up has been completed
     * Useful for health checks and monitoring
     */
    public boolean isWarmUpCompleted() {
        return warmUpCompleted;
    }
    
    /**
     * Check if cache warm-up is currently in progress
     */
    public boolean isWarmUpInProgress() {
        return warmUpInProgress;
    }
}