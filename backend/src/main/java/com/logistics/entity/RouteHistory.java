package com.logistics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_history")
public class RouteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String destination;
    private String trafficLevel; // LOW / MEDIUM / HIGH
    private String timeOfDay; // MORNING / AFTERNOON / PEAK / NIGHT
    private String routeType; // CITY / HIGHWAY

    private double distance; // km
    private double baseTime; // minutes (Google Maps / fallback)
    private double predictedTime; // minutes (ML model)
    private double delay; // predictedTime - baseTime

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public RouteHistory() {
    }

    public Long getId() {
        return id;
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

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(double baseTime) {
        this.baseTime = baseTime;
    }

    public double getPredictedTime() {
        return predictedTime;
    }

    public void setPredictedTime(double predictedTime) {
        this.predictedTime = predictedTime;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}