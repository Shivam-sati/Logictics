package com.logistics.service;

import org.springframework.stereotype.Service;

@Service
public class SimulationService {

    public double getTrafficMultiplier(String trafficLevel) {
        return switch (trafficLevel.toUpperCase()) {
            case "LOW" -> 1.0;
            case "MEDIUM" -> 1.25;
            case "HIGH" -> 1.60;
            default -> 1.0;
        };
    }

    public double getTimeMultiplier(String timeOfDay) {
        return switch (timeOfDay.toUpperCase()) {
            case "MORNING" -> 1.10;
            case "AFTERNOON" -> 1.00;
            case "PEAK" -> 1.40;
            case "NIGHT" -> 0.90;
            default -> 1.0;
        };
    }

    public int encodeTraffic(String trafficLevel) {
        return switch (trafficLevel.toUpperCase()) {
            case "LOW" -> 0;
            case "MEDIUM" -> 1;
            case "HIGH" -> 2;
            default -> 1;
        };
    }

    public int encodeTime(String timeOfDay) {
        return switch (timeOfDay.toUpperCase()) {
            case "MORNING" -> 0;
            case "AFTERNOON" -> 1;
            case "PEAK" -> 2;
            case "NIGHT" -> 3;
            default -> 1;
        };
    }

    public int encodeRoute(String routeType) {
        return switch (routeType.toUpperCase()) {
            case "CITY" -> 0;
            case "HIGHWAY" -> 1;
            default -> 0;
        };
    }

    public double simulatePredictedTime(double baseTime, String traffic,
            String timeOfDay, String routeType) {
        double result = baseTime
                * getTrafficMultiplier(traffic)
                * getTimeMultiplier(timeOfDay);

        if ("HIGHWAY".equalsIgnoreCase(routeType)) {
            result *= 0.90; // highways are faster than city routes
        }

        return result;
    }
}