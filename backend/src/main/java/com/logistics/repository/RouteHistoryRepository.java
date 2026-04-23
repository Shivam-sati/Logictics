package com.logistics.repository;

import com.logistics.entity.RouteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteHistoryRepository extends JpaRepository<RouteHistory, Long> {

    @Query("SELECT AVG(r.delay) FROM RouteHistory r")
    Double findAverageDelay();

    @Query("SELECT AVG(r.predictedTime) FROM RouteHistory r")
    Double findAveragePredictedTime();

    @Query("SELECT r.trafficLevel FROM RouteHistory r " +
            "GROUP BY r.trafficLevel " +
            "ORDER BY COUNT(r.trafficLevel) DESC " +
            "LIMIT 1")
    String findMostFrequentTrafficLevel();

    List<RouteHistory> findTop20ByOrderByCreatedAtDesc();
}