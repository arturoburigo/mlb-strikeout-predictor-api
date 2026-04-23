package com.mlbsk.api.controller;

import com.mlbsk.api.dto.PaginatedResponse;
import com.mlbsk.api.dto.PredictionResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
class PredictionControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void returnsEmptyListWhenNoPredictionsForDate() {
        Argument<?> responseType = Argument.of(PaginatedResponse.class, PredictionResponse.class);

        PaginatedResponse<?> response = (PaginatedResponse<?>) client.toBlocking()
            .retrieve(
                HttpRequest.GET("/api/v1/predictions?date=2024-01-01&page=0&size=10").header("X-API-Key", "default-dev-key"),
                responseType
            );

        Assertions.assertEquals(0, response.page());
        Assertions.assertEquals(10, response.size());
        Assertions.assertEquals(0, response.items().size());
    }

    @Test
    void returnsPopulatedListForValidDate() {
        Argument<?> responseType = Argument.of(PaginatedResponse.class, PredictionResponse.class);

        PaginatedResponse<?> response = (PaginatedResponse<?>) client.toBlocking()
            .retrieve(
                HttpRequest.GET("/api/v1/predictions?date=2026-04-18&page=0&size=50").header("X-API-Key", "default-dev-key"),
                responseType
            );

        Assertions.assertEquals(0, response.page());
        Assertions.assertEquals(50, response.size());
        Assertions.assertEquals(1, response.items().size());

        PredictionResponse item = (PredictionResponse) response.items().getFirst();
        Assertions.assertEquals("Chris Sale", item.player());
        Assertions.assertEquals(5.5, item.line());
        Assertions.assertEquals("OVER", item.recommendedSide());
    }

    @Test
    void returnsLatestPredictionsWhenDateIsOmitted() {
        Argument<?> responseType = Argument.of(PaginatedResponse.class, PredictionResponse.class);

        PaginatedResponse<?> response = (PaginatedResponse<?>) client.toBlocking()
            .retrieve(
                HttpRequest.GET("/api/v1/predictions?page=0&size=10").header("X-API-Key", "default-dev-key"),
                responseType
            );

        Assertions.assertEquals(1, response.items().size());
        PredictionResponse item = (PredictionResponse) response.items().getFirst();
        Assertions.assertEquals(LocalDate.parse("2026-04-18"), item.predictionDate());
    }
}
