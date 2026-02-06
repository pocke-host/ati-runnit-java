// ========== PolylineDecoder.java ==========
package com.runnit.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to decode Google Polyline encoded strings
 * Used by Garmin, Strava, and other fitness platforms
 */
public class PolylineDecoder {
    
    public static List<LatLng> decode(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return poly;
    }
    
    @Data
    public static class LatLng {
        private final double lat;
        private final double lng;
    }
}