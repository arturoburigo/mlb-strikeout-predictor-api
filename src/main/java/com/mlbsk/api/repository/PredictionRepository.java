package com.mlbsk.api.repository;

import com.mlbsk.api.domain.PredictionRow;
import io.micronaut.core.annotation.Nullable;
import java.time.LocalDate;
import reactor.core.publisher.Flux;

public interface PredictionRepository {

    Flux<PredictionRow> findByDate(@Nullable LocalDate date, int limit, int offset);
}
