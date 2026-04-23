package com.mlbsk.api.repository;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class R2dbcWriteRepository implements WriteRepository {

    private final ConnectionFactory factory;

    public R2dbcWriteRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Mono<AuthUserRow> findUserByEmail(String email) {
        return queryOne("""
            SELECT id, email, display_name, password_hash, is_active
            FROM users
            WHERE email = $1
            """, stmt -> stmt.bind("$1", email), (row, meta) -> new AuthUserRow(
            row.get("id", String.class),
            row.get("email", String.class),
            row.get("display_name", String.class),
            row.get("password_hash", String.class),
            Boolean.TRUE.equals(row.get("is_active", Boolean.class))
        ));
    }

    @Override
    public Mono<Void> upsertUserPreferences(String userId, Double defaultStake, String defaultPlatform, Double minEdgeFilter) {
        return execute("""
            INSERT INTO user_preferences (user_id, default_stake, default_platform, min_edge_filter)
            VALUES ($1, $2, $3, $4)
            ON CONFLICT (user_id) DO UPDATE
            SET default_stake = EXCLUDED.default_stake,
                default_platform = EXCLUDED.default_platform,
                min_edge_filter = EXCLUDED.min_edge_filter
            """, stmt -> {
                io.r2dbc.spi.Statement bound = bindNullable(stmt.bind("$1", userId), "$2", defaultStake, Double.class)
                    .bind("$3", defaultPlatform);
                return bindNullable(bound, "$4", minEdgeFilter, Double.class);
            });
    }

    @Override
    public Mono<PredictionAvailabilityRow> findPredictionAvailability(long predictionId) {
        return queryOne("""
            SELECT id, recommended_side
            FROM predictions
            WHERE id = $1
            """, stmt -> stmt.bind("$1", predictionId), (row, meta) -> new PredictionAvailabilityRow(
            readLong(row, "id"),
            row.get("recommended_side", String.class)
        ));
    }

    @Override
    public Mono<String> findLatestTrackedBetId(String userId, long predictionId) {
        return queryOne("""
            SELECT id
            FROM user_bets
            WHERE user_id = $1 AND prediction_id = $2
            ORDER BY placed_at DESC
            LIMIT 1
            """, stmt -> stmt.bind("$1", userId).bind("$2", predictionId), (row, meta) -> row.get("id", String.class));
    }

    @Override
    public Mono<Void> updateTrackedBet(String betId, Double stake, Double odds, String platform) {
        return execute("""
            UPDATE user_bets
            SET stake = $2, odds = $3, platform = $4, placed_at = $5
            WHERE id = $1
            """, stmt -> stmt.bind("$1", betId)
            .bind("$2", stake)
            .bind("$3", odds)
            .bind("$4", platform)
            .bind("$5", Instant.now()));
    }

    @Override
    public Mono<Void> createTrackedBet(String id, String userId, long predictionId, Double stake, Double odds, String platform) {
        return execute("""
            INSERT INTO user_bets (id, user_id, prediction_id, stake, odds, platform, placed_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            """, stmt -> stmt.bind("$1", id)
            .bind("$2", userId)
            .bind("$3", predictionId)
            .bind("$4", stake)
            .bind("$5", odds)
            .bind("$6", platform)
            .bind("$7", Instant.now()));
    }

    @Override
    public Flux<PredictionAvailabilityRow> findPredictionAvailabilities(Iterable<Long> predictionIds) {
        List<Long> ids = new ArrayList<>();
        predictionIds.forEach(ids::add);
        if (ids.isEmpty()) {
            return Flux.empty();
        }

        StringBuilder sql = new StringBuilder("SELECT id, recommended_side FROM predictions WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("$").append(i + 1);
        }
        sql.append(")");

        return queryMany(sql.toString(), stmt -> {
            for (int i = 0; i < ids.size(); i++) {
                stmt.bind("$" + (i + 1), ids.get(i));
            }
            return stmt;
        }, (row, meta) -> new PredictionAvailabilityRow(
            readLong(row, "id"),
            row.get("recommended_side", String.class)
        ));
    }

    @Override
    public Mono<Void> createParlayWithBets(String parlayId, String userId, Double stake, Double odds, String platform, Iterable<Long> predictionIds, Iterable<String> betIds) {
        List<Long> ids = new ArrayList<>();
        predictionIds.forEach(ids::add);
        List<String> newBetIds = new ArrayList<>();
        betIds.forEach(newBetIds::add);

        return Mono.usingWhen(
            Mono.from(factory.create()),
            conn -> Mono.from(conn.beginTransaction())
                .then(executeWithConnection(conn, """
                    INSERT INTO user_parlays (id, user_id, stake, odds, platform, placed_at)
                    VALUES ($1, $2, $3, $4, $5, $6)
                    """, stmt -> stmt.bind("$1", parlayId)
                    .bind("$2", userId)
                    .bind("$3", stake)
                    .bind("$4", odds)
                    .bind("$5", platform)
                    .bind("$6", Instant.now())))
                .thenMany(Flux.range(0, ids.size())
                    .concatMap(index -> executeWithConnection(conn, """
                        INSERT INTO user_bets (id, user_id, prediction_id, parlay_id, stake, odds, platform, placed_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                        """, stmt -> stmt.bind("$1", newBetIds.get(index))
                        .bind("$2", userId)
                        .bind("$3", ids.get(index))
                        .bind("$4", parlayId)
                        .bindNull("$5", Double.class)
                        .bindNull("$6", Double.class)
                        .bind("$7", platform)
                        .bind("$8", Instant.now()))))
                .then(Mono.from(conn.commitTransaction()))
                .onErrorResume(error -> Mono.from(conn.rollbackTransaction()).then(Mono.error(error))),
            conn -> Mono.from(conn.close())
        );
    }

    @Override
    public Mono<AuthUserRow> findUserById(String userId) {
        return queryOne("""
            SELECT id, email, display_name, password_hash, is_active
            FROM users
            WHERE id = $1
            """, stmt -> stmt.bind("$1", userId), (row, meta) -> new AuthUserRow(
            row.get("id", String.class),
            row.get("email", String.class),
            row.get("display_name", String.class),
            row.get("password_hash", String.class),
            Boolean.TRUE.equals(row.get("is_active", Boolean.class))
        ));
    }

    @Override
    public Mono<Void> createUser(String id, String email, String displayName, String passwordHash) {
        return execute("""
            INSERT INTO users (id, email, display_name, password_hash, is_active, created_at)
            VALUES ($1, $2, $3, $4, TRUE, $5)
            """, stmt -> bindNullable(stmt.bind("$1", id).bind("$2", email), "$3", displayName, String.class)
            .bind("$4", passwordHash)
            .bind("$5", Instant.now()));
    }

    @Override
    public Mono<Void> updateUserActive(String userId, boolean active) {
        return execute("""
            UPDATE users
            SET is_active = $2
            WHERE id = $1
            """, stmt -> stmt.bind("$1", userId).bind("$2", active));
    }

    private <T> Flux<T> queryMany(String sql, StatementBinder binder, RowMapper<T> mapper) {
        return Mono.from(factory.create()).flatMapMany(conn -> Flux.from(binder.bind(conn.createStatement(sql)).execute())
            .flatMap(result -> result.map(mapper::map))
            .doFinally(signalType -> conn.close()));
    }

    private <T> Mono<T> queryOne(String sql, StatementBinder binder, RowMapper<T> mapper) {
        return queryMany(sql, binder, mapper).next();
    }

    private Mono<Void> execute(String sql, StatementBinder binder) {
        return Mono.usingWhen(
            Mono.from(factory.create()),
            conn -> executeWithConnection(conn, sql, binder),
            conn -> Mono.from(conn.close())
        );
    }

    private Mono<Void> executeWithConnection(Connection conn, String sql, StatementBinder binder) {
        return Mono.from(binder.bind(conn.createStatement(sql)).execute()).then();
    }

    private long readLong(Row row, String column) {
        Number value = row.get(column, Number.class);
        return value != null ? value.longValue() : 0L;
    }

    private io.r2dbc.spi.Statement bindNullable(io.r2dbc.spi.Statement stmt, String key, Object value, Class<?> type) {
        if (value == null) {
            stmt.bindNull(key, type);
        } else {
            stmt.bind(key, value);
        }
        return stmt;
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(Row row, RowMetadata metadata);
    }

    @FunctionalInterface
    private interface StatementBinder {
        io.r2dbc.spi.Statement bind(io.r2dbc.spi.Statement statement);
    }
}
