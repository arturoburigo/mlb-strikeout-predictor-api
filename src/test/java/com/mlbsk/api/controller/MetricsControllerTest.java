package com.mlbsk.api.controller;

import com.mlbsk.api.dto.DailyMetricsResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
class MetricsControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void returnsMetricsForRequestedDate() {
        var response = client.toBlocking()
            .exchange(
                HttpRequest.GET("/api/v1/metrics/daily?date=2026-04-18").header("X-API-Key", "default-dev-key"),
                DailyMetricsResponse.class
            );

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertNotNull(response.header("X-Request-Id"));

        DailyMetricsResponse body = response.body();
        Assertions.assertNotNull(body);

        Assertions.assertEquals(LocalDate.parse("2026-04-18"), body.date());
        Assertions.assertEquals(1.25, body.mae());
        Assertions.assertEquals(10, body.totalPredictions());
    }

    @Test
    void returnsLatestMetricsWhenDateIsOmitted() {
        var response = client.toBlocking()
            .exchange(
                HttpRequest.GET("/api/v1/metrics/daily").header("X-API-Key", "default-dev-key"),
                DailyMetricsResponse.class
            );

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertNotNull(response.header("X-Request-Id"));

        DailyMetricsResponse body = response.body();
        Assertions.assertNotNull(body);

        Assertions.assertEquals(LocalDate.parse("2026-04-18"), body.date());
        Assertions.assertEquals(0.65, body.hitRate());
    }

    @Test
    void returnsEmptyMetricsForUnknownDate() {
        var response = client.toBlocking()
            .exchange(
                HttpRequest.GET("/api/v1/metrics/daily?date=2024-01-01").header("X-API-Key", "default-dev-key"),
                DailyMetricsResponse.class
            );

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertNotNull(response.header("X-Request-Id"));

        DailyMetricsResponse body = response.body();
        Assertions.assertNotNull(body);

        Assertions.assertEquals(LocalDate.parse("2024-01-01"), body.date());
        Assertions.assertNull(body.mae());
        Assertions.assertEquals(0, body.totalPredictions());
    }

    @Test
    void rejectsInvalidDateQueryParameter() {
        HttpClientResponseException exception = Assertions.assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/metrics/daily?date=invalid-date").header("X-API-Key", "default-dev-key"),
                DailyMetricsResponse.class
            )
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        Assertions.assertNotNull(exception.getResponse().header("X-Request-Id"));
    }
}
