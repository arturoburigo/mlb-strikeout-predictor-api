package com.mlbsk.api.service;

import com.mlbsk.api.dto.DailyMetricsResponse;
import com.mlbsk.api.mapper.MetricsMapper;
import com.mlbsk.api.repository.MetricsRepository;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

@Singleton
public class MetricsService {

    private final MetricsRepository metricsRepository;
    private final MetricsMapper metricsMapper;

    public MetricsService(MetricsRepository metricsRepository, MetricsMapper metricsMapper) {
        this.metricsRepository = metricsRepository;
        this.metricsMapper = metricsMapper;
    }

    public Mono<DailyMetricsResponse> getDailyMetrics(@Nullable LocalDate date) {
        return metricsRepository.findDailyMetricsByDate(date)
            .map(metricsMapper::toDto)
            .defaultIfEmpty(new DailyMetricsResponse(
                date != null ? date : LocalDate.now(),
                null,
                null,
                null,
                null,
                0
            ));
    }
}
