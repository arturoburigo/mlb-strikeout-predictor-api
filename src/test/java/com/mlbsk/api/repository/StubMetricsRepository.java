package com.mlbsk.api.repository;

import com.mlbsk.api.domain.DailyMetricsRow;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

@Singleton
@Replaces(R2dbcMetricsRepository.class)
public class StubMetricsRepository implements MetricsRepository {

    @Override
    public Mono<DailyMetricsRow> findDailyMetricsByDate(@Nullable LocalDate date) {
        if (date == null || LocalDate.parse("2026-04-18").equals(date)) {
            return Mono.just(new DailyMetricsRow(
                LocalDate.parse("2026-04-18"),
                1.25,
                0.55,
                0.12,
                0.65,
                10
            ));
        }
        return Mono.empty();
    }
}
