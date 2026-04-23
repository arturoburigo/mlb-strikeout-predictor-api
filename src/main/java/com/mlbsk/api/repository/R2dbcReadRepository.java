package com.mlbsk.api.repository;

import io.micronaut.core.annotation.Nullable;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class R2dbcReadRepository implements ReadRepository {

    private final ConnectionFactory factory;

    public R2dbcReadRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Mono<LocalDate> findLatestPredictionDate() {
        return queryOne(
            "SELECT MAX(prediction_date) AS prediction_date FROM predictions",
            (row, meta) -> row.get("prediction_date", LocalDate.class)
        );
    }

    @Override
    public Flux<DashboardPredictionRow> findPredictionsByDate(LocalDate date) {
        return queryMany("""
            SELECT p.id, p.prediction_date, p.player, p.team, p.opponent, p.home,
                   p.over_line, p.over_odds, p.under_odds, p.expected_k_poisson,
                   p.api_projected_value, p.ml_predict_value, p.ml_confidence_percentage,
                   p.p_over, p.p_under, p.recommended_side, p.edge_over, p.edge_under,
                   p.result, p.ai_insight, os.bookmaker
            FROM predictions p
            LEFT JOIN odds_snapshots os ON os.id = p.odds_snapshot_id
            WHERE p.prediction_date = $1
            ORDER BY p.created_at DESC
            """, stmt -> stmt.bind("$1", date), (row, meta) -> new DashboardPredictionRow(
            readLong(row, "id"),
            row.get("prediction_date", LocalDate.class),
            row.get("player", String.class),
            row.get("team", String.class),
            row.get("opponent", String.class),
            row.get("home", Boolean.class),
            readDouble(row, "over_line"),
            readDouble(row, "over_odds"),
            readDouble(row, "under_odds"),
            readDouble(row, "expected_k_poisson"),
            readDouble(row, "api_projected_value"),
            readDouble(row, "ml_predict_value"),
            readDouble(row, "ml_confidence_percentage"),
            readDouble(row, "p_over"),
            readDouble(row, "p_under"),
            row.get("recommended_side", String.class),
            readDouble(row, "edge_over"),
            readDouble(row, "edge_under"),
            row.get("bookmaker", String.class),
            row.get("result", String.class),
            row.get("ai_insight", String.class)
        ));
    }

    @Override
    public Flux<DashboardTrackedBetRow> findLatestTrackedBets(String userId, LocalDate date) {
        return queryMany("""
            SELECT prediction_id, id, stake, odds, platform, placed_at, parlay_id
            FROM (
                SELECT ub.*,
                       ROW_NUMBER() OVER (PARTITION BY ub.prediction_id ORDER BY ub.placed_at DESC) AS rn
                FROM user_bets ub
                JOIN predictions p ON p.id = ub.prediction_id
                WHERE ub.user_id = $1 AND p.prediction_date = $2
            ) latest
            WHERE rn = 1
            """, stmt -> stmt.bind("$1", userId).bind("$2", date), (row, meta) -> new DashboardTrackedBetRow(
            readLong(row, "prediction_id"),
            row.get("id", String.class),
            readDouble(row, "stake"),
            readDouble(row, "odds"),
            row.get("platform", String.class),
            row.get("placed_at", Instant.class),
            row.get("parlay_id", String.class)
        ));
    }

    @Override
    public Mono<DashboardMetricsRow> findLatestMetrics() {
        return queryOne("""
            SELECT prediction_date, total_predictions, settled_count, green_count, red_count, push_count,
                   win_rate, brier_score, roi_pct, avg_edge, model_mae
            FROM daily_accuracy_metrics
            ORDER BY prediction_date DESC
            LIMIT 1
            """, (row, meta) -> new DashboardMetricsRow(
            row.get("prediction_date", LocalDate.class),
            readInt(row, "total_predictions"),
            readInt(row, "settled_count"),
            readInt(row, "green_count"),
            readInt(row, "red_count"),
            readInt(row, "push_count"),
            readDouble(row, "win_rate"),
            readDouble(row, "brier_score"),
            readDouble(row, "roi_pct"),
            readDouble(row, "avg_edge"),
            readDouble(row, "model_mae")
        ));
    }

    @Override
    public Flux<UserDailyStatRow> findRecentUserDailyStats(String userId, int limit) {
        return queryMany("""
            SELECT stat_date, bets, wins, losses, staked, profit, roi_pct
            FROM user_daily_stats
            WHERE user_id = $1
            ORDER BY stat_date DESC
            LIMIT $2
            """, stmt -> stmt.bind("$1", userId).bind("$2", limit), (row, meta) -> new UserDailyStatRow(
            row.get("stat_date", LocalDate.class),
            readInt(row, "bets"),
            readInt(row, "wins"),
            readInt(row, "losses"),
            readDouble(row, "staked"),
            readDouble(row, "profit"),
            readDouble(row, "roi_pct")
        ));
    }

    @Override
    public Flux<DashboardRecentBetRow> findRecentDashboardBets(String userId, int limit) {
        return queryMany("""
            SELECT ub.id, ub.stake, ub.odds, ub.platform, ub.placed_at,
                   p.player, p.recommended_side, p.over_line, p.result,
                   ubr.actual_k, ubr.profit, ubr.won
            FROM user_bets ub
            JOIN predictions p ON p.id = ub.prediction_id
            LEFT JOIN user_bet_results ubr ON ubr.user_bet_id = ub.id
            WHERE ub.user_id = $1
            ORDER BY ub.placed_at DESC
            LIMIT $2
            """, stmt -> stmt.bind("$1", userId).bind("$2", limit), (row, meta) -> new DashboardRecentBetRow(
            row.get("id", String.class),
            readDouble(row, "stake"),
            readDouble(row, "odds"),
            row.get("platform", String.class),
            row.get("placed_at", Instant.class),
            row.get("player", String.class),
            row.get("recommended_side", String.class),
            readDouble(row, "over_line"),
            row.get("result", String.class),
            row.get("actual_k", Integer.class),
            readDouble(row, "profit"),
            row.get("won", Boolean.class)
        ));
    }

    @Override
    public Mono<UserPreferenceRow> findUserPreferences(String userId) {
        return queryOne("""
            SELECT default_stake, default_platform, min_edge_filter
            FROM user_preferences
            WHERE user_id = $1
            """, stmt -> stmt.bind("$1", userId), (row, meta) -> new UserPreferenceRow(
            readDouble(row, "default_stake"),
            row.get("default_platform", String.class),
            readDouble(row, "min_edge_filter")
        ));
    }

    @Override
    public Flux<PerformanceMetricsRow> findPerformanceMetrics(int limit) {
        return queryMany("""
            SELECT prediction_date, total_predictions, settled_count, green_count, red_count, push_count,
                   win_rate, brier_score, model_mae, avg_edge, roi_pct
            FROM daily_accuracy_metrics
            ORDER BY prediction_date DESC
            LIMIT $1
            """, stmt -> stmt.bind("$1", limit), (row, meta) -> new PerformanceMetricsRow(
            row.get("prediction_date", LocalDate.class),
            readInt(row, "total_predictions"),
            readInt(row, "settled_count"),
            readInt(row, "green_count"),
            readInt(row, "red_count"),
            readInt(row, "push_count"),
            readDouble(row, "win_rate"),
            readDouble(row, "brier_score"),
            readDouble(row, "model_mae"),
            readDouble(row, "avg_edge"),
            readDouble(row, "roi_pct")
        ));
    }

    @Override
    public Flux<SettledPredictionRow> findSettledPredictions(int limit) {
        return queryMany("""
            SELECT id, prediction_date, player, recommended_side, over_line, expected_k_poisson,
                   actual_strikeouts, result, edge_over, edge_under
            FROM predictions
            WHERE result IS NOT NULL AND actual_strikeouts IS NOT NULL
            ORDER BY prediction_date DESC
            LIMIT $1
            """, stmt -> stmt.bind("$1", limit), (row, meta) -> new SettledPredictionRow(
            readLong(row, "id"),
            row.get("prediction_date", LocalDate.class),
            row.get("player", String.class),
            row.get("recommended_side", String.class),
            readDouble(row, "over_line"),
            readDouble(row, "expected_k_poisson"),
            row.get("actual_strikeouts", Integer.class),
            row.get("result", String.class),
            readDouble(row, "edge_over"),
            readDouble(row, "edge_under")
        ));
    }

    @Override
    public Flux<ModelVersionRow> findModelVersions(int limit) {
        return queryMany("""
            SELECT id, version_label, objective, calibration_brier, created_at
            FROM model_versions
            ORDER BY created_at DESC
            LIMIT $1
            """, stmt -> stmt.bind("$1", limit), (row, meta) -> new ModelVersionRow(
            readLong(row, "id"),
            row.get("version_label", String.class),
            row.get("objective", String.class),
            readDouble(row, "calibration_brier"),
            row.get("created_at", Instant.class)
        ));
    }

    @Override
    public Flux<BetHistoryRow> findBetHistory(String userId) {
        return queryMany("""
            SELECT ub.id, ub.placed_at, ub.parlay_id, ub.stake, ub.odds, ub.platform,
                   up.stake AS parlay_stake, up.odds AS parlay_odds,
                   p.prediction_date, p.player, p.team, p.opponent, p.home,
                   p.recommended_side, p.over_line, p.actual_strikeouts, p.result,
                   ubr.actual_k, ubr.profit, ubr.won
            FROM user_bets ub
            JOIN predictions p ON p.id = ub.prediction_id
            LEFT JOIN user_bet_results ubr ON ubr.user_bet_id = ub.id
            LEFT JOIN user_parlays up ON up.id = ub.parlay_id
            WHERE ub.user_id = $1
            ORDER BY ub.placed_at DESC
            """, stmt -> stmt.bind("$1", userId), (row, meta) -> new BetHistoryRow(
            row.get("id", String.class),
            row.get("placed_at", Instant.class),
            row.get("parlay_id", String.class),
            readDouble(row, "stake"),
            readDouble(row, "odds"),
            row.get("platform", String.class),
            readDouble(row, "parlay_stake"),
            readDouble(row, "parlay_odds"),
            row.get("prediction_date", LocalDate.class),
            row.get("player", String.class),
            row.get("team", String.class),
            row.get("opponent", String.class),
            row.get("home", Boolean.class),
            row.get("recommended_side", String.class),
            readDouble(row, "over_line"),
            row.get("actual_strikeouts", Integer.class),
            row.get("result", String.class),
            row.get("actual_k", Integer.class),
            readDouble(row, "profit"),
            row.get("won", Boolean.class)
        ));
    }

    @Override
    public Flux<AdminUserRow> findAdminUsers() {
        return queryMany("""
            SELECT id, email, display_name, is_active, created_at
            FROM users
            ORDER BY created_at ASC
            """, (row, meta) -> new AdminUserRow(
            row.get("id", String.class),
            row.get("email", String.class),
            row.get("display_name", String.class),
            Boolean.TRUE.equals(row.get("is_active", Boolean.class)),
            row.get("created_at", Instant.class)
        ));
    }

    @Override
    public Flux<AdminDailyStatAggregateRow> findAdminDailyStatAggregates() {
        return queryMany("""
            SELECT user_id, SUM(bets) AS total_bets, SUM(profit) AS total_profit
            FROM user_daily_stats
            GROUP BY user_id
            """, (row, meta) -> new AdminDailyStatAggregateRow(
            row.get("user_id", String.class),
            row.get("total_bets", Integer.class),
            readDouble(row, "total_profit")
        ));
    }

    @Override
    public Flux<AdminBetActivityRow> findAdminBetActivity() {
        return queryMany("""
            SELECT user_id, COUNT(id) AS total_bets, MAX(placed_at) AS last_active_at
            FROM user_bets
            GROUP BY user_id
            """, (row, meta) -> new AdminBetActivityRow(
            row.get("user_id", String.class),
            readLong(row, "total_bets"),
            row.get("last_active_at", Instant.class)
        ));
    }

    @Override
    public Flux<PitcherGameStatRow> findPitcherGameHistory(String pitcherId, int limit) {
        return queryMany("""
            SELECT id, game_date, team, opponent, innings_pitched, strikeouts, pitches_total
            FROM pitcher_game_stats
            WHERE pitcher_id = $1
            ORDER BY game_date DESC
            LIMIT $2
            """, stmt -> stmt.bind("$1", pitcherId).bind("$2", limit), (row, meta) -> new PitcherGameStatRow(
            readLong(row, "id"),
            row.get("game_date", LocalDate.class),
            row.get("team", String.class),
            row.get("opponent", String.class),
            readDouble(row, "innings_pitched"),
            row.get("strikeouts", Integer.class),
            row.get("pitches_total", Integer.class)
        ));
    }

    @Override
    public Mono<String> findLatestPitcherHand(String pitcherId, @Nullable LocalDate predictionDate) {
        if (predictionDate == null) {
            return queryOne("""
                SELECT pitcher_hand
                FROM pitcher_game_context
                WHERE pitcher_id = $1
                ORDER BY game_date DESC
                LIMIT 1
                """, stmt -> stmt.bind("$1", pitcherId), (row, meta) -> row.get("pitcher_hand", String.class));
        }
        return queryOne("""
            SELECT pitcher_hand
            FROM pitcher_game_context
            WHERE pitcher_id = $1 AND game_date <= $2
            ORDER BY game_date DESC
            LIMIT 1
            """, stmt -> stmt.bind("$1", pitcherId).bind("$2", predictionDate), (row, meta) -> row.get("pitcher_hand", String.class));
    }

    @Override
    public Mono<OpponentTrendRow> findOpponentTrend(String opponent, @Nullable LocalDate predictionDate) {
        if (predictionDate == null) {
            return queryOne("""
                SELECT as_of_date, team, window_days, games_sampled, k_pct_overall, k_pct_vs_rhp, k_pct_vs_lhp
                FROM team_batting_trends
                WHERE team = $1 AND window_days IN (15, 30)
                ORDER BY as_of_date DESC, window_days ASC
                LIMIT 1
                """, stmt -> stmt.bind("$1", opponent), this::mapOpponentTrend);
        }
        return queryOne("""
            SELECT as_of_date, team, window_days, games_sampled, k_pct_overall, k_pct_vs_rhp, k_pct_vs_lhp
            FROM team_batting_trends
            WHERE team = $1 AND window_days IN (15, 30) AND as_of_date <= $2
            ORDER BY as_of_date DESC, window_days ASC
            LIMIT 1
            """, stmt -> stmt.bind("$1", opponent).bind("$2", predictionDate), this::mapOpponentTrend);
    }

    private OpponentTrendRow mapOpponentTrend(Row row, RowMetadata meta) {
        return new OpponentTrendRow(
            row.get("as_of_date", LocalDate.class),
            row.get("team", String.class),
            readInt(row, "window_days"),
            row.get("games_sampled", Integer.class),
            readDouble(row, "k_pct_overall"),
            readDouble(row, "k_pct_vs_rhp"),
            readDouble(row, "k_pct_vs_lhp")
        );
    }

    private <T> Flux<T> queryMany(String sql, RowMapper<T> mapper) {
        return queryMany(sql, stmt -> stmt, mapper);
    }

    private <T> Flux<T> queryMany(String sql, StatementBinder binder, RowMapper<T> mapper) {
        return Flux.usingWhen(
            Mono.from(factory.create()),
            conn -> Flux.from(binder.bind(conn.createStatement(sql)).execute())
                .flatMap(result -> result.map(mapper::map)),
            conn -> Mono.from(conn.close())
        );
    }

    private <T> Mono<T> queryOne(String sql, RowMapper<T> mapper) {
        return queryOne(sql, stmt -> stmt, mapper);
    }

    private <T> Mono<T> queryOne(String sql, StatementBinder binder, RowMapper<T> mapper) {
        return queryMany(sql, binder, mapper).next();
    }

    private LocalDate readLocalDate(Row row, String column) {
        return row.get(column, LocalDate.class);
    }

    private long readLong(Row row, String column) {
        Number value = row.get(column, Number.class);
        return value != null ? value.longValue() : 0L;
    }

    private int readInt(Row row, String column) {
        Number value = row.get(column, Number.class);
        return value != null ? value.intValue() : 0;
    }

    private Double readDouble(Row row, String column) {
        Number value = row.get(column, Number.class);
        return value != null ? value.doubleValue() : null;
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
