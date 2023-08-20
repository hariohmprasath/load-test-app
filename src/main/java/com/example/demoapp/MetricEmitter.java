package com.example.demoapp;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricEmitter {
    static double COUNTER = 0;
    private static final String CONCURRENCY = "CONCURRENCY";

    private final CloudWatchClient CLOUDWATCH = CloudWatchClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    private static final Dimension DIMENSION = Dimension.builder()
            .name("QUALIFIER")
            .value("FUNCTION_ARN")
            .build();

    public void beep() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            System.out.println("Emitting metric: " + COUNTER);
            final String time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            final Instant instant = Instant.parse(time);

            final MetricDatum datum = MetricDatum.builder()
                    .metricName(CONCURRENCY)
                    .unit(StandardUnit.PERCENT)
                    .value(COUNTER)
                    .timestamp(instant)
                    .dimensions(DIMENSION).build();

            final PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace("APPULSE")
                    .metricData(datum).build();

            CLOUDWATCH.putMetricData(request);
        }, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized static void increment() {
        COUNTER++;
    }

    public synchronized static void decrement() {
        COUNTER--;
    }

}
