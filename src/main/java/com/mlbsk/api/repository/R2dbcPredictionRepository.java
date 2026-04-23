package com.mlbsk.api.repository;

import com.mlbsk.api.domain.PredictionRow;
import io.micronaut.core.annotation.Nullable;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class R2dbcPredictionRepository implements PredictionRepository {

    private final ConnectionFactory factory;

    public R2dbcPredictionRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Flux<PredictionRow> findByDate(@Nullable LocalDate date, int limit, int offset) {
        return Flux.usingWhen(
            Mono.from(factory.create()),
            conn -> {
                var statement = date != null
                    ? conn.createStatement("""
                        SELECT p.id, p.prediction_date, p.player, p.team, p.opponent, p.home,
                               p.over_line, p.over_odds, p.under_odds, p.expected_k_poisson,
                               p.p_over, p.p_under, p.recommended_side, p.edge_over, p.edge_under,
                               p.result, p.ai_insight, os.bookmaker
                        FROM predictions p
                        LEFT JOIN odds_snapshots os ON os.id = p.odds_snapshot_id
                        WHERE p.prediction_date = $1
                        ORDER BY p.created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                        .bind("$1", date)
                        .bind("$2", limit)
                        .bind("$3", offset)
                    : conn.createStatement("""
                        SELECT p.id, p.prediction_date, p.player, p.team, p.opponent, p.home,
                               p.over_line, p.over_odds, p.under_odds, p.expected_k_poisson,
                               p.p_over, p.p_under, p.recommended_side, p.edge_over, p.edge_under,
                               p.result, p.ai_insight, os.bookmaker
                        FROM predictions p
                        LEFT JOIN odds_snapshots os ON os.id = p.odds_snapshot_id
                        WHERE p.prediction_date = (SELECT MAX(prediction_date) FROM predictions)
                        ORDER BY p.created_at DESC
                        LIMIT $1 OFFSET $2
                        """)
                        .bind("$1", limit)
                        .bind("$2", offset);

                return Flux.from(statement.execute())
                    .flatMap(result -> result.map(this::mapRow));
            },
            conn -> Mono.from(conn.close())
        );
    }

    private PredictionRow mapRow(Row row, RowMetadata metadata) {
        Number idValue = row.get("id", Number.class);
        return new PredictionRow(
            idValue != null ? idValue.longValue() : 0L,
            row.get("prediction_date", LocalDate.class),
            row.get("player", String.class),
            row.get("team", String.class),
            row.get("opponent", String.class),
            row.get("home", Boolean.class),
            readDouble(row, "over_line"),
            readDouble(row, "over_odds"),
            readDouble(row, "under_odds"),
            readDouble(row, "expected_k_poisson"),
            readDouble(row, "p_over"),
            readDouble(row, "p_under"),
            row.get("recommended_side", String.class),
            readDouble(row, "edge_over"),
            readDouble(row, "edge_under"),
            row.get("bookmaker", String.class),
            row.get("result", String.class),
            row.get("ai_insight", String.class)
        );
    }

    private Double readDouble(Row row, String column) {
        Number value = row.get(column, Number.class);
        return value != null ? value.doubleValue() : null;
    }
}
