package com.mlbsk.api.repository;

import com.mlbsk.api.domain.PipelineRunRow;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import jakarta.inject.Singleton;
import java.time.Instant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class R2dbcPipelineRunRepository implements PipelineRunRepository {

    private final ConnectionFactory factory;

    public R2dbcPipelineRunRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Flux<PipelineRunRow> findAll(int limit, int offset) {
        return Flux.usingWhen(
            Mono.from(factory.create()),
            conn -> Flux.from(conn.createStatement("""
                    SELECT id, job_name, status, started_at, finished_at, error_message
                    FROM pipeline_runs
                    ORDER BY started_at DESC
                    LIMIT $1 OFFSET $2
                    """)
                    .bind("$1", limit)
                    .bind("$2", offset)
                    .execute())
                .flatMap(result -> result.map(this::mapRow)),
            conn -> Mono.from(conn.close())
        );
    }

    private PipelineRunRow mapRow(Row row, io.r2dbc.spi.RowMetadata metadata) {
        Number idValue = row.get("id", Number.class);
        String rawId = row.get("id", String.class);
        long id = idValue != null ? idValue.longValue() : rawId != null ? rawId.hashCode() : 0L;

        return new PipelineRunRow(
            id,
            row.get("job_name", String.class),
            row.get("status", String.class),
            row.get("started_at", Instant.class),
            row.get("finished_at", Instant.class),
            row.get("error_message", String.class)
        );
    }
}
