package com.logistics.service;

import com.logistics.entity.RouteHistory;
import com.logistics.model.RouteRequest;
import com.logistics.model.RouteResponse;
import com.logistics.repository.RouteHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final MapService mapService;
    private final MLClientService mlClientService;
    private final SimulationService simulationService;
    private final RouteHistoryRepository repository;

    public RouteService(MapService mapService,
            MLClientService mlClientService,
            SimulationService simulationService,
            RouteHistoryRepository repository) {
        this.mapService = mapService;
        this.mlClientService = mlClientService;
        this.simulationService = simulationService;
        this.repository = repository;
    }

    public RouteResponse optimizeRoute(RouteRequest request) {
        log.info("Optimizing route: {} → {} | Traffic: {} | Time: {} | Route: {}",
                request.getSource(), request.getDestination(),
                request.getTrafficLevel(), request.getTimeOfDay(), request.getRouteType());

        // Step 1: Get distance and base travel time
        double[] distanceAndTime = mapService.getDistanceAndTime(
                request.getSource(), request.getDestination());
        double distanceKm = distanceAndTime[0];
        double baseTime = distanceAndTime[1];

        // Step 2: Encode categorical features for ML model
        int trafficEncoded = simulationService.encodeTraffic(request.getTrafficLevel());
        int timeEncoded = simulationService.encodeTime(request.getTimeOfDay());
        int routeEncoded = simulationService.encodeRoute(request.getRouteType());

        // Step 3: Get ML prediction (falls back to simulation if Python is down)
        double predictedTime = mlClientService.getPredictedTime(
                distanceKm, trafficEncoded, timeEncoded, routeEncoded,
                baseTime,
                request.getTrafficLevel(), request.getTimeOfDay(), request.getRouteType());

        // Step 4: Determine data source label for transparency
        // (frontend shows this so panel knows we have fallback logic)
        String dataSource = "Google Maps + ML Model";

        // Step 5: Build response object
        RouteResponse response = RouteResponse.of(
                request.getSource(), request.getDestination(),
                distanceKm, baseTime, predictedTime,
                request.getTrafficLevel(), request.getTimeOfDay(), request.getRouteType(),
                dataSource);

        // Step 6: Persist to database for trend analytics
        saveToHistory(request, response);

        log.info("Result: dist={} km, base={} min, predicted={} min, delay={} min, risk={}",
                response.getDistance(), response.getBaseTime(),
                response.getPredictedTime(), response.getDelay(), response.getDelayRisk());

        return response;
    }

    public List<RouteHistory> getHistory() {
        return repository.findTop20ByOrderByCreatedAtDesc();
    }

    private void saveToHistory(RouteRequest request, RouteResponse response) {
        RouteHistory history = new RouteHistory();
        history.setSource(request.getSource());
        history.setDestination(request.getDestination());
        history.setTrafficLevel(request.getTrafficLevel());
        history.setTimeOfDay(request.getTimeOfDay());
        history.setRouteType(request.getRouteType());
        history.setDistance(response.getDistance());
        history.setBaseTime(response.getBaseTime());
        history.setPredictedTime(response.getPredictedTime());
        history.setDelay(response.getDelay());
        repository.save(history);
        log.info("Route saved to database.");
    }
}