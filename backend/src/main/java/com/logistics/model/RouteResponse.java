package com.logistics.model;

public class RouteResponse {

    private String source;
    private String destination;
    private double distance;
    private double baseTime;
    private double predictedTime;
    private double delay;
    private String delayRisk;
    private String trafficLevel;
    private String timeOfDay;
    private String routeType;
    private String dataSource;

    public static RouteResponse of(String source, String destination,
            double distance, double baseTime,
            double predictedTime, String traffic,
            String timeOfDay, String routeType,
            String dataSource) {
        RouteResponse r = new RouteResponse();
        r.source = source;
        r.destination = destination;
        r.distance = Math.round(distance * 10.0) / 10.0;
        r.baseTime = Math.round(baseTime * 10.0) / 10.0;
        r.predictedTime = Math.round(predictedTime * 10.0) / 10.0;
        r.delay = Math.round((predictedTime - baseTime) * 10.0) / 10.0;
        r.trafficLevel = traffic;
        r.timeOfDay = timeOfDay;
        r.routeType = routeType;
        r.dataSource = dataSource;

        if (r.delay < 10) {
            r.delayRisk = "LOW";
        } else if (r.delay < 25) {
            r.delayRisk = "MEDIUM";
        } else {
            r.delayRisk = "HIGH";
        }

        return r;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }

    public double getBaseTime() {
        return baseTime;
    }

    public double getPredictedTime() {
        return predictedTime;
    }

    public double getDelay() {
        return delay;
    }

    public String getDelayRisk() {
        return delayRisk;
    }

    public String getTrafficLevel() {
        return trafficLevel;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public String getRouteType() {
        return routeType;
    }

    public String getDataSource() {
        return dataSource;
    }
}