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

                // Step 1: Get distance and real base travel time from Google Maps
                double distanceKm = request.getDistanceKm();
                double baseTime = request.getBaseTime(); // from Google Maps Distance Matrix

                // Step 2: Encode categorical features for ML model
                int trafficEncoded = simulationService.encodeTraffic(request.getTrafficLevel());
                int timeEncoded = simulationService.encodeTime(request.getTimeOfDay());
                int routeEncoded = simulationService.encodeRoute(request.getRouteType());

                // Step 3: ML model returns a DELAY RATIO (e.g. 0.2 = 20% of base time)
                // Falls back to simulation if Python service is down.
                double delayRatio = mlClientService.getPredictedTime(
                                distanceKm, trafficEncoded, timeEncoded, routeEncoded,
                                baseTime,
                                request.getTrafficLevel(), request.getTimeOfDay(), request.getRouteType());

                // Step 4: Convert ratio → actual delay minutes using real Google Maps base time
                // This is the key fix: delay scales correctly for ANY route length.
                // e.g. ratio 0.2 on a 1500-min trip → 300 min delay (realistic)
                // ratio 0.2 on a 120-min trip → 24 min delay (realistic)
                double predictedDelay = delayRatio * baseTime;

                // Step 5: Safety clamp — ratio must stay within traffic-level bounds
                // (guards against any ML outlier predictions)
                double maxRatio;
                switch (request.getTrafficLevel()) {
                        case "LOW":
                                maxRatio = 0.25;
                                break;
                        case "MEDIUM":
                                maxRatio = 0.50;
                                break;
                        case "HIGH":
                                maxRatio = 0.90;
                                break;
                        default:
                                maxRatio = 0.60;
                }
                predictedDelay = Math.min(predictedDelay, maxRatio * baseTime);

                // Step 6: Final predicted total travel time
                double predictedTime = baseTime + predictedDelay;

                // Step 7: Determine data source label for UI transparency
                String dataSource = "Google Maps + ML Model";

                // Step 8: Build response object
                RouteResponse response = RouteResponse.of(
                                request.getSource(), request.getDestination(),
                                distanceKm, baseTime, predictedTime,
                                request.getTrafficLevel(), request.getTimeOfDay(), request.getRouteType(),
                                dataSource);

                // Step 9: Persist to database for trend analytics
                saveToHistory(request, response);

                log.info("Result: dist={} km, base={} min, ratio={}, delay={} min, predicted={} min, risk={}",
                                response.getDistance(), response.getBaseTime(),
                                String.format("%.3f", delayRatio),
                                response.getDelay(), response.getPredictedTime(), response.getDelayRisk());

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