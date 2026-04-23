package com.mlbsk.api.repository;

import com.mlbsk.api.domain.PredictionRow;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import reactor.core.publisher.Flux;

@Singleton
@Replaces(R2dbcPredictionRepository.class)
public class StubPredictionRepository implements PredictionRepository {

    @Override
    public Flux<PredictionRow> findByDate(@Nullable LocalDate date, int limit, int offset) {
        if (offset > 0) {
            return Flux.empty();
        }

        if (date == null || LocalDate.parse("2026-04-18").equals(date)) {
            return Flux.just(new PredictionRow(
                1L,
                LocalDate.parse("2026-04-18"),
                "Chris Sale",
                "ATL",
                "NYM",
                true,
                5.5,
                1.91,
                1.87,
                6.2,
                0.58,
                0.42,
                "OVER",
                0.08,
                -0.03,
                "Betano",
                "OPEN",
                "insight"
            ));
        }

        return Flux.empty();
    }
}
