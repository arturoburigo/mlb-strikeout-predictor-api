package com.mlbsk.api.repository;

import io.micronaut.core.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReadRepository {

    Mono<LocalDate> findLatestPredictionDate();

    Flux<DashboardPredictionRow> findPredictionsByDate(LocalDate date);

    Flux<DashboardOddsBookmakerRow> findOddsBookmakersByDate(LocalDate date);

    Flux<DashboardTrackedBetRow> findLatestTrackedBets(String userId, LocalDate date);

    Mono<DashboardMetricsRow> findLatestMetrics();

    Flux<UserDailyStatRow> findRecentUserDailyStats(String userId, int limit);

    Flux<DashboardRecentBetRow> findRecentDashboardBets(String userId, int limit);

    Mono<UserPreferenceRow> findUserPreferences(String userId);

    Flux<PerformanceMetricsRow> findPerformanceMetrics(int limit);

    Flux<SettledPredictionRow> findSettledPredictions(int limit);

    Flux<ModelVersionRow> findModelVersions(int limit);

    Flux<BetHistoryRow> findBetHistory(String userId);

    Flux<AdminUserRow> findAdminUsers();

    Flux<AdminDailyStatAggregateRow> findAdminDailyStatAggregates();

    Flux<AdminBetActivityRow> findAdminBetActivity();

    Flux<PitcherGameStatRow> findPitcherGameHistory(String pitcherId, int limit);

    Mono<String> findLatestPitcherHand(String pitcherId, @Nullable LocalDate predictionDate);

    Mono<OpponentTrendRow> findOpponentTrend(String opponent, @Nullable LocalDate predictionDate);

    record DashboardPredictionRow(
        long id,
        LocalDate predictionDate,
        String player,
        String team,
        String opponent,
        Boolean home,
        Double line,
        Double overOdds,
        Double underOdds,
        Double expectedStrikeouts,
        Double apiProjectedValue,
        Double mlPredictValue,
        Double mlConfidencePercentage,
        Double overProbability,
        Double underProbability,
        String recommendedSide,
        Double edgeOver,
        Double edgeUnder,
        String bookmaker,
        String result,
        String aiInsight
    ) {}

    record DashboardTrackedBetRow(
        long predictionId,
        String id,
        Double stake,
        Double odds,
        String platform,
        Instant placedAt,
        String parlayId
    ) {}

    record DashboardMetricsRow(
        LocalDate predictionDate,
        int totalPredictions,
        int settledCount,
        int greenCount,
        int redCount,
        int pushCount,
        Double winRate,
        Double brierScore,
        Double roiPct,
        Double avgEdge,
        Double modelMae
    ) {}

    record UserDailyStatRow(
        LocalDate statDate,
        int bets,
        int wins,
        int losses,
        Double staked,
        Double profit,
        Double roiPct
    ) {}

    record DashboardRecentBetRow(
        String id,
        Double stake,
        Double odds,
        String platform,
        Instant placedAt,
        String player,
        String recommendedSide,
        Double line,
        String result,
        Integer actualK,
        Double profit,
        Boolean won
    ) {}

    record UserPreferenceRow(
        Double defaultStake,
        String defaultPlatform,
        Double minEdgeFilter
    ) {}

    record PerformanceMetricsRow(
        LocalDate predictionDate,
        int totalPredictions,
        int settledCount,
        int greenCount,
        int redCount,
        int pushCount,
        Double winRate,
        Double brierScore,
        Double modelMae,
        Double avgEdge,
        Double roiPct
    ) {}

    record SettledPredictionRow(
        long id,
        LocalDate predictionDate,
        String player,
        String recommendedSide,
        Double line,
        Double expectedK,
        Integer actualK,
        String result,
        Double edgeOver,
        Double edgeUnder
    ) {}

    record ModelVersionRow(
        long id,
        String versionLabel,
        String objective,
        Double calibrationBrier,
        Instant createdAt
    ) {}

    record BetHistoryRow(
        String id,
        Instant placedAt,
        String parlayId,
        Double stake,
        Double odds,
        String platform,
        Double parlayStake,
        Double parlayOdds,
        LocalDate predictionDate,
        String player,
        String team,
        String opponent,
        Boolean home,
        String recommendedSide,
        Double line,
        Integer predictionActualStrikeouts,
        String predictionResult,
        Integer actualK,
        Double storedProfit,
        Boolean won
    ) {}

    record AdminUserRow(
        String id,
        String email,
        String displayName,
        boolean isActive,
        Instant createdAt
    ) {}

    record AdminDailyStatAggregateRow(
        String userId,
        Integer totalBets,
        Double totalProfit
    ) {}

    record AdminBetActivityRow(
        String userId,
        long totalBets,
        Instant lastActiveAt
    ) {}

    record PitcherGameStatRow(
        long id,
        LocalDate gameDate,
        String team,
        String opponent,
        Double inningsPitched,
        Integer strikeouts,
        Integer pitchesTotal
    ) {}

    record OpponentTrendRow(
        LocalDate asOfDate,
        String team,
        int windowDays,
        Integer gamesSampled,
        Double kPctOverall,
        Double kPctVsRhp,
        Double kPctVsLhp
    ) {}

    record DashboardOddsBookmakerRow(
        String player,
        String bookmaker,
        Double overLine,
        Double overOdds,
        Double underOdds,
        String alternativeLinesJson
    ) {}
}
