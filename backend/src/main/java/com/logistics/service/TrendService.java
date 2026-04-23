package com.logistics.service;

import com.logistics.model.TrendResponse;
import com.logistics.repository.RouteHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class TrendService {

    private final RouteHistoryRepository repository;

    public TrendService(RouteHistoryRepository repository) {
        this.repository = repository;
    }

    public TrendResponse getTrends() {
        TrendResponse trends = new TrendResponse();

        long count = repository.count();
        trends.setTotalRequests(count);

        if (count == 0) {
            // No data yet — return zeros
            trends.setAverageDelay(0);
            trends.setAveragePredictedTime(0);
            trends.setMostFrequentTraffic("N/A");
            return trends;
        }

        // Average delay across all stored routes
        Double avgDelay = repository.findAverageDelay();
        trends.setAverageDelay(avgDelay != null ? avgDelay : 0);

        // Average ML-predicted time
        Double avgPredicted = repository.findAveragePredictedTime();
        trends.setAveragePredictedTime(avgPredicted != null ? avgPredicted : 0);

        // Most submitted traffic level (mode)
        String mostFrequent = repository.findMostFrequentTrafficLevel();
        trends.setMostFrequentTraffic(mostFrequent != null ? mostFrequent : "N/A");

        return trends;
    }
}