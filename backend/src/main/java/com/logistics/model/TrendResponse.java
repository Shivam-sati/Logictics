package com.logistics.model;

public class TrendResponse {

    private double averageDelay; // mean delay across all requests
    private double averagePredictedTime; // mean predicted time across all requests
    private String mostFrequentTraffic; // which traffic level appears most
    private long totalRequests; // total routes optimized so far

    public double getAverageDelay() {
        return averageDelay;
    }

    public void setAverageDelay(double averageDelay) {
        this.averageDelay = Math.round(averageDelay * 10.0) / 10.0;
    }

    public double getAveragePredictedTime() {
        return averagePredictedTime;
    }

    public void setAveragePredictedTime(double averagePredictedTime) {
        this.averagePredictedTime = Math.round(averagePredictedTime * 10.0) / 10.0;
    }

    public String getMostFrequentTraffic() {
        return mostFrequentTraffic;
    }

    public void setMostFrequentTraffic(String mostFrequentTraffic) {
        this.mostFrequentTraffic = mostFrequentTraffic;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }
}