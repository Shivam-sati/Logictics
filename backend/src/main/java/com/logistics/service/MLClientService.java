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
     * Returns the ML-predicted delivery time in minutes.
     *
     * Falls back to SimulationService if Python service is unreachable.
     *
     * @param distanceKm calculated route distance
     * @param traffic    encoded traffic level (0=LOW, 1=MEDIUM, 2=HIGH)
     * @param timeOfDay  encoded time (0=MORNING, 1=AFTERNOON, 2=PEAK, 3=NIGHT)
     * @param routeType  encoded route (0=CITY, 1=HIGHWAY)
     * @param baseTime   base time in minutes (used for fallback)
     * @param trafficStr string traffic level (used for fallback)
     * @param timeStr    string time of day (used for fallback)
     * @param routeStr   string route type (used for fallback)
     */
    public double getPredictedTime(double distanceKm, int traffic, int timeOfDay,
            int routeType, double baseTime,
            String trafficStr, String timeStr, String routeStr) {
        try {
            // Build request body matching Python's PredictRequest schema
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("distance_km", distanceKm);
            requestBody.put("traffic_level", traffic);
            requestBody.put("time_of_day", timeOfDay);
            requestBody.put("route_type", routeType);

            log.info("Calling ML service at {} with features: dist={}, traffic={}, time={}, route={}",
                    mlServiceUrl, distanceKm, traffic, timeOfDay, routeType);

            // POST to Python FastAPI service
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    mlServiceUrl, requestBody, Map.class);

            if (response != null && response.containsKey("predicted_time")) {
                double predicted = ((Number) response.get("predicted_time")).doubleValue();
                log.info("ML prediction received: {:.1f} min", predicted);
                return predicted;
            }

            throw new RuntimeException("ML response missing predicted_time field");

        } catch (Exception e) {
            // This is expected if the Python service isn't started yet
            log.warn("ML service unavailable ({}). Using simulation fallback.", e.getMessage());
            double fallback = simulationService.simulatePredictedTime(
                    baseTime, trafficStr, timeStr, routeStr);
            log.info("Simulation fallback prediction: {:.1f} min", fallback);
            return fallback;
        }
    }
}