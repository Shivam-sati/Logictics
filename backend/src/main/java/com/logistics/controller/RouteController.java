package com.logistics.controller;

import com.logistics.entity.RouteHistory;
import com.logistics.model.RouteRequest;
import com.logistics.model.RouteResponse;
import com.logistics.model.TrendResponse;
import com.logistics.service.RouteService;
import com.logistics.service.TrendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/route")
@CrossOrigin(origins = "*") // Allow frontend to call regardless of port
public class RouteController {

    private static final Logger log = LoggerFactory.getLogger(RouteController.class);

    private final RouteService routeService;
    private final TrendService trendService;

    public RouteController(RouteService routeService, TrendService trendService) {
        this.routeService = routeService;
        this.trendService = trendService;
    }

    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeRoute(@RequestBody RouteRequest request) {
        try {
            // Validate required fields
            if (request.getSource() == null || request.getSource().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Source location is required"));
            }
            if (request.getDestination() == null || request.getDestination().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Destination location is required"));
            }

            RouteResponse response = routeService.optimizeRoute(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error optimizing route: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Route optimization failed: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<RouteHistory>> getHistory() {
        return ResponseEntity.ok(routeService.getHistory());
    }

    @GetMapping("/trends")
    public ResponseEntity<TrendResponse> getTrends() {
        return ResponseEntity.ok(trendService.getTrends());
    }
}