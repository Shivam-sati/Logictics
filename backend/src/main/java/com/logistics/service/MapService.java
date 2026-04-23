package com.logistics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;

/**
 * MapService fetches real-world distance and base travel time
 * from the Google Maps Distance Matrix API.
 *
 * If the API key is missing or the call fails, it falls back to
 * a simple simulation based on average Indian logistics speeds.
 */
@Service
public class MapService {

    private static final Logger log = LoggerFactory.getLogger(MapService.class);

    private final RestTemplate restTemplate;

    @Value("${google.maps.api.key}")
    private String apiKey;

    // Pre-seeded distance table for common Indian city pairs
    // Used as fallback when Google Maps is unavailable
    private static final Map<String, double[]> DISTANCE_TABLE = new HashMap<>();

    static {
        // Key: "SOURCE|DESTINATION", Value: [distance_km, base_time_min]
        DISTANCE_TABLE.put("MUMBAI|PUNE", new double[] { 148, 155 });
        DISTANCE_TABLE.put("MUMBAI|NASHIK", new double[] { 167, 200 });
        DISTANCE_TABLE.put("MUMBAI|DELHI", new double[] { 1415, 1350 });
        DISTANCE_TABLE.put("MUMBAI|BANGALORE", new double[] { 984, 960 });
        DISTANCE_TABLE.put("DELHI|AGRA", new double[] { 233, 210 });
        DISTANCE_TABLE.put("DELHI|JAIPUR", new double[] { 281, 270 });
        DISTANCE_TABLE.put("DELHI|CHANDIGARH", new double[] { 248, 240 });
        DISTANCE_TABLE.put("BANGALORE|CHENNAI", new double[] { 346, 330 });
        DISTANCE_TABLE.put("BANGALORE|HYDERABAD", new double[] { 569, 540 });
        DISTANCE_TABLE.put("CHENNAI|HYDERABAD", new double[] { 630, 600 });
        DISTANCE_TABLE.put("KOLKATA|BHUBANESWAR", new double[] { 440, 420 });
        DISTANCE_TABLE.put("PUNE|NASHIK", new double[] { 210, 195 });
        DISTANCE_TABLE.put("DELHI|LUCKNOW", new double[] { 555, 480 });
        DISTANCE_TABLE.put("SURAT|AHMEDABAD", new double[] { 265, 240 });
        DISTANCE_TABLE.put("HYDERABAD|VIJAYAWADA", new double[] { 275, 265 });
    }

    public MapService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public double[] getDistanceAndTime(String source, String destination) {
        if (apiKey != null && !apiKey.equals("YOUR_GOOGLE_MAPS_API_KEY") && !apiKey.isBlank()) {
            try {
                return fetchFromGoogleMaps(source, destination);
            } catch (Exception e) {
                log.warn("Google Maps API failed ({}). Using simulation fallback.", e.getMessage());
            }
        } else {
            log.info("No Google Maps API key configured. Using simulation fallback.");
        }

        return simulateFallback(source, destination);
    }

    @SuppressWarnings("unchecked")
    private double[] fetchFromGoogleMaps(String source, String destination) {
        String url = String.format(
                "https://maps.googleapis.com/maps/api/distancematrix/json" +
                        "?origins=%s&destinations=%s&key=%s",
                source.replace(" ", "+"),
                destination.replace(" ", "+"),
                apiKey);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null)
            throw new RuntimeException("Empty response from Google Maps");

        var rows = (java.util.List<Map<String, Object>>) response.get("rows");
        var element = (Map<String, Object>) ((java.util.List<Map<String, Object>>) rows.get(0).get("elements")).get(0);

        if (!"OK".equals(element.get("status"))) {
            throw new RuntimeException("Google Maps element status: " + element.get("status"));
        }

        double distanceMeters = (double) ((Map<String, Object>) element.get("distance")).get("value");
        double durationSeconds = (double) ((Map<String, Object>) element.get("duration")).get("value");

        double distanceKm = distanceMeters / 1000.0;
        double durationMin = durationSeconds / 60.0;

        log.info("Google Maps: {} → {} = {:.1f} km, {:.0f} min", source, destination, distanceKm, durationMin);
        return new double[] { distanceKm, durationMin };
    }

    private double[] simulateFallback(String source, String destination) {
        String key = (source.toUpperCase() + "|" + destination.toUpperCase());
        String reverseKey = (destination.toUpperCase() + "|" + source.toUpperCase());

        // Check lookup table (both directions)
        if (DISTANCE_TABLE.containsKey(key)) {
            log.info("Fallback table hit: {}", key);
            return DISTANCE_TABLE.get(key);
        }
        if (DISTANCE_TABLE.containsKey(reverseKey)) {
            log.info("Fallback table hit (reverse): {}", reverseKey);
            return DISTANCE_TABLE.get(reverseKey);
        }
        int seed = (source + destination).chars().sum();
        double estimatedKm = 100 + (seed % 400); // range: 100–500 km
        double estimatedMin = estimatedKm / 0.65; // ~39 km/h average road speed

        log.info("Estimated fallback: {} → {} ≈ {:.0f} km", source, destination, estimatedKm);
        return new double[] { estimatedKm, estimatedMin };
    }
}