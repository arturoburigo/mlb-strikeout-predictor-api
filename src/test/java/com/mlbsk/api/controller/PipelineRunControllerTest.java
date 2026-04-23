package com.mlbsk.api.controller;

import com.mlbsk.api.dto.PaginatedResponse;
import com.mlbsk.api.dto.PipelineRunResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
class PipelineRunControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void returnsEmptyListWhenOffsetIsHigh() {
        Argument<?> responseType = Argument.of(PaginatedResponse.class, PipelineRunResponse.class);

        PaginatedResponse<?> response = (PaginatedResponse<?>) client.toBlocking()
            .retrieve(
                HttpRequest.GET("/api/v1/pipeline-runs?page=2&size=10").header("X-API-Key", "default-dev-key"),
                responseType
            );

        Assertions.assertEquals(2, response.page());
        Assertions.assertEquals(10, response.size());
        Assertions.assertEquals(0, response.items().size());
    }

    @Test
    void returnsPopulatedListForFirstPage() {
        Argument<?> responseType = Argument.of(PaginatedResponse.class, PipelineRunResponse.class);

        PaginatedResponse<?> response = (PaginatedResponse<?>) client.toBlocking()
            .retrieve(
                HttpRequest.GET("/api/v1/pipeline-runs?page=1&size=50").header("X-API-Key", "default-dev-key"),
                responseType
            );

        Assertions.assertEquals(1, response.page());
        Assertions.assertEquals(50, response.size());
        Assertions.assertEquals(2, response.items().size());

        PipelineRunResponse item = (PipelineRunResponse) response.items().getFirst();
        Assertions.assertEquals(1L, item.id());
        Assertions.assertEquals("daily-pipeline", item.jobName());
        Assertions.assertEquals("SUCCESS", item.status());
    }
}
