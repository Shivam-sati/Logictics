package com.logistics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MLClientService {

    private static final Logger log = LoggerFactory.getLogger(MLClientService.class);

    private final RestTemplate restTemplate;
    private final SimulationService simulationService;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    public MLClientService(RestTemplate restTemplate, SimulationService simulationService) {
        this.restTemplate = restTemplate;
        this.simulationService = simulationService;
    }

    /**
     * Calls POST http://localhost:5000/predict with route features.
     * Returns the raw DELAY RATIO from ML (e.g. 0.079).
     * RouteService multiplies this by baseTime to get actual delay minutes.
     *
     * Falls back to ratio estimate if Python service is unreachable.
     */
    public double getPredictedTime(double distanceKm, int traffic, int timeOfDay,
            int routeType, double baseTime,
            String trafficStr, String timeStr, String routeStr) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("traffic_level", traffic);
            requestBody.put("time_of_day", timeOfDay);
            requestBody.put("route_type", routeType);

            log.info("Calling ML service at {} with features: traffic={}, time={}, route={}",
                    mlServiceUrl, traffic, timeOfDay, routeType);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    mlServiceUrl, requestBody, Map.class);

            if (response != null && response.containsKey("predicted_time")) {
                // ML returns raw ratio (e.g. 0.079) — return as-is
                // RouteService handles the × baseTime multiplication
                double delayRatio = ((Number) response.get("predicted_time")).doubleValue();
                log.info("ML ratio received: {}", String.format("%.4f", delayRatio));
                return delayRatio;
            }

            throw new RuntimeException("ML response missing predicted_time field");

        } catch (Exception e) {
            log.warn("ML service unavailable ({}). Using simulation fallback.", e.getMessage());

            // Fallback: simple ratio by traffic level (no baseTime multiplication here)
            double fallbackRatio;
            switch (trafficStr) {
                case "LOW":
                    fallbackRatio = 0.10;
                    break;
                case "MEDIUM":
                    fallbackRatio = 0.20;
                    break;
                case "HIGH":
                    fallbackRatio = 0.45;
                    break;
                default:
                    fallbackRatio = 0.20;
            }
            log.info("Simulation fallback ratio: {}", fallbackRatio);
            return fallbackRatio;
        }
    }
}