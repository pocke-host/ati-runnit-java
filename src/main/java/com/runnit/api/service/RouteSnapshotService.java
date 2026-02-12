// ========== RouteSnapshotService.java ==========
package com.runnit.api.service;

import com.runnit.api.util.PolylineDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteSnapshotService {

    private final RestTemplate restTemplate;
    private final S3Service s3Service;
    
    @Value("${mapbox.api.key:}")
    private String mapboxApiKey;
    
    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;
    
    private static final int MAP_WIDTH = 600;
    private static final int MAP_HEIGHT = 400;
    private static final String ROUTE_COLOR = "0000ff"; // Blue
    private static final int ROUTE_WEIGHT = 3;
    
    // Record definition - ONLY ONCE at the top
    private record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}

    /**
     * Generate a static map image from an encoded polyline
     * Uses Mapbox Static Images API (free tier available)
     */
    public String generateRouteSnapshot(String encodedPolyline) {
        log.info("Generating route snapshot from polyline");
        
        try {
            // Option 1: Use Mapbox (recommended - generous free tier)
            if (mapboxApiKey != null && !mapboxApiKey.isEmpty()) {
                return generateMapboxSnapshot(encodedPolyline);
            }
            
            // Option 2: Use Google Maps Static API (requires billing)
            if (googleMapsApiKey != null && !googleMapsApiKey.isEmpty()) {
                return generateGoogleMapsSnapshot(encodedPolyline);
            }
            
            log.warn("No map API key configured, skipping route snapshot generation");
            return null;
            
        } catch (Exception e) {
            log.error("Failed to generate route snapshot", e);
            return null;
        }
    }
    
    private String generateMapboxSnapshot(String encodedPolyline) throws IOException {
        log.info("Generating Mapbox route snapshot");
        
        // Decode polyline to get coordinates
        List<PolylineDecoder.LatLng> coordinates = PolylineDecoder.decode(encodedPolyline);
        
        if (coordinates.isEmpty()) {
            log.warn("No coordinates in polyline");
            return null;
        }
        
        // Get bounding box for auto-zoom
        BoundingBox bbox = calculateBoundingBox(coordinates);
        
        // Simplify path if too many points (Mapbox has URL length limits)
        List<PolylineDecoder.LatLng> simplifiedPath = simplifyPath(coordinates, 100);
        
        // Build path overlay string
        StringBuilder pathOverlay = new StringBuilder("path-" + ROUTE_WEIGHT + "+")
            .append(ROUTE_COLOR)
            .append("(");
        
        for (int i = 0; i < simplifiedPath.size(); i++) {
            PolylineDecoder.LatLng point = simplifiedPath.get(i);
            if (i > 0) pathOverlay.append(",");
            pathOverlay.append(String.format("%.6f,%.6f", point.getLng(), point.getLat()));
        }
        pathOverlay.append(")");
        
        // Calculate center and zoom
        double centerLng = (bbox.minLng() + bbox.maxLng()) / 2;
        double centerLat = (bbox.minLat() + bbox.maxLat()) / 2;
        int zoom = calculateZoomLevel(bbox);
        
        String mapboxUrl = String.format(
            "https://api.mapbox.com/styles/v1/mapbox/outdoors-v12/static/%s/%f,%f,%d,0,0/%dx%d@2x?access_token=%s",
            URLEncoder.encode(pathOverlay.toString(), StandardCharsets.UTF_8),
            centerLng,
            centerLat,
            zoom,
            MAP_WIDTH,
            MAP_HEIGHT,
            mapboxApiKey
        );
        
        log.info("Fetching Mapbox image: {}", mapboxUrl.substring(0, Math.min(100, mapboxUrl.length())));
        
        // Download map image
        byte[] imageBytes = restTemplate.getForObject(mapboxUrl, byte[].class);
        
        // Upload to S3
        String filename = "route-snapshots/" + UUID.randomUUID() + ".png";
        return s3Service.uploadFile(imageBytes, filename, "image/png");
    }
    
    private String generateGoogleMapsSnapshot(String encodedPolyline) throws IOException {
        log.info("Generating Google Maps route snapshot");
        
        // Google Maps Static API URL
        String googleMapsUrl = String.format(
            "https://maps.googleapis.com/maps/api/staticmap?size=%dx%d&path=enc:%s&key=%s",
            MAP_WIDTH,
            MAP_HEIGHT,
            URLEncoder.encode(encodedPolyline, StandardCharsets.UTF_8),
            googleMapsApiKey
        );
        
        // Download map image
        byte[] imageBytes = restTemplate.getForObject(googleMapsUrl, byte[].class);
        
        // Upload to S3
        String filename = "route-snapshots/" + UUID.randomUUID() + ".png";
        return s3Service.uploadFile(imageBytes, filename, "image/png");
    }
    
    private BoundingBox calculateBoundingBox(List<PolylineDecoder.LatLng> coordinates) {
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;
        
        for (PolylineDecoder.LatLng coord : coordinates) {
            minLat = Math.min(minLat, coord.getLat());
            maxLat = Math.max(maxLat, coord.getLat());
            minLng = Math.min(minLng, coord.getLng());
            maxLng = Math.max(maxLng, coord.getLng());
        }
        
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }
    
    private int calculateZoomLevel(BoundingBox bbox) {
        // Calculate appropriate zoom level based on bounding box size
        double latDiff = bbox.maxLat() - bbox.minLat();
        double lngDiff = bbox.maxLng() - bbox.minLng();
        double maxDiff = Math.max(latDiff, lngDiff);
        
        // Rough zoom calculation
        if (maxDiff > 10) return 5;
        if (maxDiff > 5) return 7;
        if (maxDiff > 2) return 9;
        if (maxDiff > 1) return 10;
        if (maxDiff > 0.5) return 11;
        if (maxDiff > 0.1) return 13;
        return 14;
    }
    
    private List<PolylineDecoder.LatLng> simplifyPath(List<PolylineDecoder.LatLng> path, int maxPoints) {
        if (path.size() <= maxPoints) {
            return path;
        }
        
        // Simple decimation - take every Nth point
        int step = path.size() / maxPoints;
        return path.stream()
            .filter(coord -> path.indexOf(coord) % step == 0)
            .toList();
    }
}