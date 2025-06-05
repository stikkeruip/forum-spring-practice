package com.uipko.forumbackend.services;

import java.util.List;
import java.util.Map;

/**
 * Service for geospatial features using Redis GEO commands
 * Provides location-based functionality for the forum
 */
public interface GeoSpatialService {
    
    /**
     * Add location for user
     */
    void addUserLocation(String username, double longitude, double latitude);
    
    /**
     * Get nearby users
     */
    List<NearbyUser> getNearbyUsers(String username, double radiusKm);
    
    /**
     * Get distance between users
     */
    Double getDistanceBetweenUsers(String user1, String user2);
    
    /**
     * Add location-based event
     */
    void addLocationEvent(String eventId, double longitude, double latitude, String eventType);
    
    /**
     * Get events near location
     */
    List<LocationEvent> getEventsNearLocation(double longitude, double latitude, double radiusKm);
    
    /**
     * Nearby user result
     */
    record NearbyUser(
        String username,
        double distance,
        double longitude,
        double latitude
    ) {}
    
    /**
     * Location event
     */
    record LocationEvent(
        String eventId,
        String eventType,
        double distance,
        double longitude,
        double latitude
    ) {}
}