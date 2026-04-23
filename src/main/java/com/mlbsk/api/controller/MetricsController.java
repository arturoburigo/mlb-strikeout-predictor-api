package com.mlbsk.api.controller;

import com.mlbsk.api.dto.DailyMetricsResponse;
import com.mlbsk.api.service.MetricsService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

@Controller("/api/v1/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Get("/daily")
    public Mono<DailyMetricsResponse> getDailyMetrics(@Nullable @QueryValue LocalDate date) {
        return metricsService.getDailyMetrics(date);
    }
}
