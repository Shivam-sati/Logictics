package com.logistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Logistics Route Optimizer backend.
 * Starts on port 8080.
 */
@SpringBootApplication
public class LogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsApplication.class, args);
        System.out.println("\n✅ Logistics Backend started → http://localhost:8080");
        System.out.println("📊 H2 Console → http://localhost:8080/h2-console\n");
    }
}