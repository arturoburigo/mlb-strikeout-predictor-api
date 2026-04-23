package com.mlbsk.api.repository;

import com.mlbsk.api.domain.DailyMetricsRow;
import io.micronaut.core.annotation.Nullable;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

public interface MetricsRepository {

    Mono<DailyMetricsRow> findDailyMetricsByDate(@Nullable LocalDate date);
}
