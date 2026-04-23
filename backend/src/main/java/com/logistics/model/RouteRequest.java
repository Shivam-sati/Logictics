package com.logistics.model;

public class RouteRequest {

    private String source; // e.g., "Mumbai"
    private String destination; // e.g., "Pune"
    private String trafficLevel; // LOW | MEDIUM | HIGH
    private String timeOfDay; // MORNING | AFTERNOON | PEAK | NIGHT
    private String routeType; // CITY | HIGHWAY
    private double distanceKm;
    private double baseTime;

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public void setBaseTime(double baseTime) {
        this.baseTime = baseTime;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getBaseTime() {
        return baseTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTrafficLevel() {
        return trafficLevel;
    }

    public void setTrafficLevel(String trafficLevel) {
        this.trafficLevel = trafficLevel;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }
}