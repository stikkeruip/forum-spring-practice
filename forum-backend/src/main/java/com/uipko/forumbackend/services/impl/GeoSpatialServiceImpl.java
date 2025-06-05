package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.GeoSpatialService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.GeoEntry;
import org.redisson.api.GeoPosition;
import org.redisson.api.RGeo;
import org.redisson.api.RedissonClient;
import org.redisson.api.geo.GeoSearchArgs;
import org.redisson.api.GeoUnit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GeoSpatialServiceImpl implements GeoSpatialService {
    
    private final RedissonClient redissonClient;
    
    // Redis keys
    private static final String USER_LOCATIONS_KEY = "forum:geo:users";
    private static final String EVENTS_LOCATIONS_KEY = "forum:geo:events";
    
    public GeoSpatialServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    @Override
    public void addUserLocation(String username, double longitude, double latitude) {
        RGeo<String> userLocations = redissonClient.getGeo(USER_LOCATIONS_KEY);
        userLocations.add(longitude, latitude, username);
        
        log.debug("Added location for user {}: ({}, {})", username, longitude, latitude);
    }
    
    @Override
    public List<NearbyUser> getNearbyUsers(String username, double radiusKm) {
        RGeo<String> userLocations = redissonClient.getGeo(USER_LOCATIONS_KEY);
        
        // Get users within radius
        List<String> nearby = userLocations.radius(username, radiusKm, GeoUnit.KILOMETERS);
        
        return nearby.stream()
            .filter(user -> !user.equals(username)) // Exclude self
            .map(user -> {
                Map<String, GeoPosition> positions = userLocations.pos(user);
                GeoPosition position = positions.get(user);
                Double distance = userLocations.dist(username, user, GeoUnit.KILOMETERS);
                
                return new NearbyUser(
                    user,
                    distance != null ? distance : 0.0,
                    position != null ? position.getLongitude() : 0.0,
                    position != null ? position.getLatitude() : 0.0
                );
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public Double getDistanceBetweenUsers(String user1, String user2) {
        RGeo<String> userLocations = redissonClient.getGeo(USER_LOCATIONS_KEY);
        return userLocations.dist(user1, user2, GeoUnit.KILOMETERS);
    }
    
    @Override
    public void addLocationEvent(String eventId, double longitude, double latitude, String eventType) {
        RGeo<String> eventLocations = redissonClient.getGeo(EVENTS_LOCATIONS_KEY);
        eventLocations.add(longitude, latitude, eventId);
        
        log.debug("Added location event {}: ({}, {}) type: {}", eventId, longitude, latitude, eventType);
    }
    
    @Override
    public List<LocationEvent> getEventsNearLocation(double longitude, double latitude, double radiusKm) {
        RGeo<String> eventLocations = redissonClient.getGeo(EVENTS_LOCATIONS_KEY);
        
        // Get events within radius
        List<String> nearby = eventLocations.radius(longitude, latitude, radiusKm, GeoUnit.KILOMETERS);
        
        return nearby.stream()
            .map(eventId -> {
                Map<String, GeoPosition> positions = eventLocations.pos(eventId);
                GeoPosition position = positions.get(eventId);
                
                // Calculate distance manually using Haversine formula
                double distance = 0.0;
                if (position != null) {
                    distance = calculateDistance(latitude, longitude, position.getLatitude(), position.getLongitude());
                }
                
                return new LocationEvent(
                    eventId,
                    "event", // Would need additional metadata storage
                    distance,
                    position != null ? position.getLongitude() : 0.0,
                    position != null ? position.getLatitude() : 0.0
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
}