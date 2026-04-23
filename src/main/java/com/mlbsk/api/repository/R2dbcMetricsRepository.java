package com.mlbsk.api.repository;

import com.mlbsk.api.domain.DailyMetricsRow;
import io.micronaut.core.annotation.Nullable;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

@Singleton
public class R2dbcMetricsRepository implements MetricsRepository {

    private final ConnectionFactory factory;

    public R2dbcMetricsRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Mono<DailyMetricsRow> findDailyMetricsByDate(@Nullable LocalDate date) {
        return Mono.from(factory.create()).flatMap(conn -> {
            var statement = date != null
                ? conn.createStatement("""
                    SELECT prediction_date as date,
                           model_mae as mae,
                           brier_score as log_loss,
                           avg_edge as avg_ev,
                           win_rate as hit_rate,
                           total_predictions
                    FROM daily_accuracy_metrics
                    WHERE prediction_date = $1
                    LIMIT 1
                    """).bind("$1", date)
                : conn.createStatement("""
                    SELECT prediction_date as date,
                           model_mae as mae,
                           brier_score as log_loss,
                           avg_edge as avg_ev,
                           win_rate as hit_rate,
                           total_predictions
                    FROM daily_accuracy_metrics
                    ORDER BY prediction_date DESC
                    LIMIT 1
                    """);

            return Mono.from(statement.execute())
                .doFinally(signalType -> conn.close())
                .flatMap(result -> Mono.from(result.map((row, metadata) -> mapRow(row))));
        });
    }

    private DailyMetricsRow mapRow(Row row) {
        LocalDate metricDate = row.get("date", LocalDate.class);
        Number totalPredictions = row.get("total_predictions", Number.class);
        return new DailyMetricsRow(
            metricDate != null ? metricDate : LocalDate.now(),
            readDouble(row, "mae"),
            readDouble(row, "log_loss"),
            readDouble(row, "avg_ev"),
            readDouble(row, "hit_rate"),
            totalPredictions != null ? totalPredictions.intValue() : 0
        );
    }

    private Double readDouble(Row row, String column) {
        Number value = row.get(column, Number.class);
        return value != null ? value.doubleValue() : null;
    }
}
