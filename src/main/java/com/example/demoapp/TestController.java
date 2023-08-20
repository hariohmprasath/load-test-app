package com.example.demoapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final int MAX_REQUESTS = System.getenv("MAX_REQUESTS") == null
            ? 10 : Integer.parseInt(System.getenv("MAX_REQUESTS"));

    @GetMapping
    public ResponseEntity<String> all() {
        if (MetricEmitter.COUNTER >= MAX_REQUESTS) 
            return ResponseEntity.status(429).body("Too many requests");        
        // Increment the counter
        MetricEmitter.increment();
        final double total = MetricEmitter.COUNTER;
        try {
            // Generate a random number between 0 and 10
            int myRandInt = (int) (Math.random() * 15000 + 1);
            Thread.sleep(myRandInt);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Decrement the counter
        MetricEmitter.decrement();
        return ResponseEntity.ok(String.valueOf(total));
    }
}
