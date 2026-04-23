package com.mlbsk.api.repository;

import com.mlbsk.api.domain.PipelineRunRow;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import java.time.Instant;
import reactor.core.publisher.Flux;

@Singleton
@Replaces(R2dbcPipelineRunRepository.class)
public class StubPipelineRunRepository implements PipelineRunRepository {

    @Override
    public Flux<PipelineRunRow> findAll(int limit, int offset) {
        if (offset == 0) {
            return Flux.just(
                new PipelineRunRow(
                    1L,
                    "daily-pipeline",
                    "SUCCESS",
                    Instant.parse("2026-04-18T10:00:00Z"),
                    Instant.parse("2026-04-18T10:05:00Z"),
                    null
                ),
                new PipelineRunRow(
                    2L,
                    "daily-pipeline",
                    "FAILED",
                    Instant.parse("2026-04-17T10:00:00Z"),
                    Instant.parse("2026-04-17T10:02:00Z"),
                    "Something went wrong"
                )
            ).take(limit);
        }
        return Flux.empty();
    }
}
