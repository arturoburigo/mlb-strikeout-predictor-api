package com.mlbsk.api.service;

import com.mlbsk.api.repository.ReadRepository;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import reactor.core.publisher.Mono;

@Singleton
public class ReadService {

    private static final int RECENT_FINAL_RECONCILIATION_DAYS = 14;
    private final ReadRepository repository;

    public ReadService(ReadRepository repository) {
        this.repository = repository;
    }

    public Mono<Map<String, Object>> getDashboard(String userId) {
        return repository.findLatestPredictionDate()
            .flatMap(date -> Mono.zip(
                repository.findPredictionsByDate(date).collectList(),
                repository.findLatestTrackedBets(userId, date).collectList(),
                repository.findLatestMetrics().map(Optional::of).defaultIfEmpty(Optional.empty()),
                repository.findRecentUserDailyStats(userId, 7).collectList(),
                repository.findRecentDashboardBets(userId, 5).collectList(),
                repository.findUserPreferences(userId).map(Optional::of).defaultIfEmpty(Optional.empty())
            ).map(tuple -> {
                List<ReadRepository.DashboardPredictionRow> predictionRows = tuple.getT1();
                Map<Long, ReadRepository.DashboardTrackedBetRow> trackedBetByPredictionId = new LinkedHashMap<>();
                for (ReadRepository.DashboardTrackedBetRow row : tuple.getT2()) {
                    trackedBetByPredictionId.put(row.predictionId(), row);
                }
                ReadRepository.DashboardMetricsRow latestMetrics = tuple.getT3().orElse(null);
                ReadRepository.UserPreferenceRow preferencesRow = tuple.getT6().orElse(null);

                List<Map<String, Object>> topPredictions = predictionRows.stream()
                    .map(row -> {
                        ReadRepository.DashboardTrackedBetRow trackedBet = trackedBetByPredictionId.get(row.id());
                        Double bestEdge = selectBestEdge(row.recommendedSide(), row.edgeOver(), row.edgeUnder());

                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", row.id());
                        item.put("predictionDate", row.predictionDate());
                        item.put("player", row.player());
                        item.put("team", row.team());
                        item.put("opponent", row.opponent());
                        item.put("home", row.home());
                        item.put("matchup", buildMatchup(row.team(), row.opponent(), row.home()));
                        item.put("line", row.line());
                        item.put("overOdds", row.overOdds());
                        item.put("underOdds", row.underOdds());
                        item.put("expectedStrikeouts", row.expectedStrikeouts());
                        item.put("apiProjectedValue", row.apiProjectedValue());
                        item.put("mlPredictValue", row.mlPredictValue());
                        item.put("mlConfidencePercentage", row.mlConfidencePercentage());
                        item.put("overProbability", row.overProbability());
                        item.put("underProbability", row.underProbability());
                        item.put("recommendedSide", row.recommendedSide());
                        item.put("recommendedOdds", selectOdds(row.recommendedSide(), row.overOdds(), row.underOdds()));
                        item.put("bestEdge", bestEdge);
                        item.put("bookmaker", row.bookmaker());
                        item.put("result", row.result());
                        item.put("aiInsight", row.aiInsight());
                        item.put("isTracked", trackedBet != null);
                        item.put("isSettled", row.result() != null && !"OPEN".equals(row.result()));
                        item.put("live", null);
                        item.put("trackedBet", trackedBet == null ? null : mapOf(
                            "id", trackedBet.id(),
                            "stake", trackedBet.stake(),
                            "odds", trackedBet.odds(),
                            "platform", trackedBet.platform(),
                            "placedAt", trackedBet.placedAt(),
                            "parlayId", trackedBet.parlayId()
                        ));
                        return item;
                    })
                    .sorted(Comparator.comparing(
                        item -> (Double) item.get("bestEdge"),
                        Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .toList();

                List<Double> recommendedEdges = topPredictions.stream()
                    .map(item -> (Double) item.get("bestEdge"))
                    .filter(value -> value != null)
                    .toList();

                int betsLast7Days = tuple.getT4().stream().mapToInt(ReadRepository.UserDailyStatRow::bets).sum();
                Double profitLast7Days = tuple.getT4().stream()
                    .map(ReadRepository.UserDailyStatRow::profit)
                    .filter(value -> value != null)
                    .reduce(0.0, Double::sum);

                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("totalPredictions", predictionRows.size());
                summary.put("recommendedPlays", predictionRows.stream().filter(row -> row.recommendedSide() != null).count());
                summary.put("averageRecommendedEdge", average(recommendedEdges));
                summary.put("strongestEdge", topPredictions.isEmpty() ? null : topPredictions.getFirst().get("bestEdge"));
                summary.put("settledWinRate", latestMetrics == null ? null : latestMetrics.winRate());
                summary.put("settledRoiPct", latestMetrics == null ? null : latestMetrics.roiPct());
                summary.put("betsLast7Days", betsLast7Days);
                summary.put("profitLast7Days", tuple.getT4().isEmpty() ? null : profitLast7Days);

                Map<String, Object> preferences = new LinkedHashMap<>();
                preferences.put("defaultStake", preferencesRow == null ? null : preferencesRow.defaultStake());
                preferences.put("defaultPlatform", preferencesRow == null ? null : preferencesRow.defaultPlatform());
                preferences.put("minEdgeFilter", preferencesRow == null ? null : preferencesRow.minEdgeFilter());

                List<Map<String, Object>> recentBets = tuple.getT5().stream().map(row -> mapOf(
                    "id", row.id(),
                    "player", row.player(),
                    "placedAt", row.placedAt(),
                    "stake", row.stake(),
                    "odds", row.odds(),
                    "platform", row.platform(),
                    "side", row.recommendedSide(),
                    "line", row.line(),
                    "result", row.result(),
                    "profit", row.profit() != null ? row.profit() : calculateProfit(row.result(), row.stake(), row.odds()),
                    "actualStrikeouts", row.actualK(),
                    "won", row.won()
                )).toList();

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "ready");
                response.put("warning", null);
                response.put("latestPredictionDate", date);
                response.put("summary", summary);
                response.put("preferences", preferences);
                response.put("latestMetrics", latestMetrics == null ? null : mapOf(
                    "predictionDate", latestMetrics.predictionDate(),
                    "totalPredictions", latestMetrics.totalPredictions(),
                    "settledCount", latestMetrics.settledCount(),
                    "greenCount", latestMetrics.greenCount(),
                    "redCount", latestMetrics.redCount(),
                    "pushCount", latestMetrics.pushCount(),
                    "winRate", latestMetrics.winRate(),
                    "brierScore", latestMetrics.brierScore(),
                    "roiPct", latestMetrics.roiPct(),
                    "avgEdge", latestMetrics.avgEdge(),
                    "modelMae", latestMetrics.modelMae()
                ));
                response.put("topPredictions", topPredictions);
                response.put("recentBets", recentBets);
                return response;
            }))
            .defaultIfEmpty(mapOf(
                "status", "degraded",
                "warning", "No prediction data available.",
                "latestPredictionDate", null,
                "summary", mapOf(
                    "totalPredictions", 0,
                    "recommendedPlays", 0,
                    "averageRecommendedEdge", null,
                    "strongestEdge", null,
                    "settledWinRate", null,
                    "settledRoiPct", null,
                    "betsLast7Days", 0,
                    "profitLast7Days", null
                ),
                "preferences", mapOf(
                    "defaultStake", null,
                    "defaultPlatform", null,
                    "minEdgeFilter", null
                ),
                "latestMetrics", null,
                "topPredictions", List.of(),
                "recentBets", List.of()
            ));
    }

    public Mono<Map<String, Object>> getPerformance() {
        return Mono.zip(
            repository.findPerformanceMetrics(90).collectList(),
            repository.findSettledPredictions(60).collectList(),
            repository.findModelVersions(10).collectList()
        ).map(tuple -> {
            List<Map<String, Object>> dailyMetrics = tuple.getT1().stream().map(row -> mapOf(
                "predictionDate", row.predictionDate(),
                "totalPredictions", row.totalPredictions(),
                "settledCount", row.settledCount(),
                "greenCount", row.greenCount(),
                "redCount", row.redCount(),
                "pushCount", row.pushCount(),
                "winRate", row.winRate(),
                "brierScore", row.brierScore(),
                "modelMae", row.modelMae(),
                "avgEdge", row.avgEdge(),
                "roiPct", row.roiPct()
            )).toList();

            List<Map<String, Object>> settledPredictions = tuple.getT2().stream().map(row -> {
                Double edge = "OVER".equals(row.recommendedSide()) ? row.edgeOver()
                    : "UNDER".equals(row.recommendedSide()) ? row.edgeUnder()
                    : null;
                Double error = row.expectedK() != null && row.actualK() != null
                    ? Math.abs(row.expectedK() - row.actualK())
                    : null;
                return mapOf(
                    "id", row.id(),
                    "predictionDate", row.predictionDate(),
                    "player", row.player(),
                    "recommendedSide", row.recommendedSide(),
                    "line", row.line(),
                    "expectedK", row.expectedK(),
                    "actualK", row.actualK(),
                    "error", error,
                    "result", row.result(),
                    "edge", edge
                );
            }).toList();

            List<Map<String, Object>> modelVersions = tuple.getT3().stream().map(row -> mapOf(
                "id", row.id(),
                "versionLabel", row.versionLabel(),
                "objective", row.objective(),
                "calibrationBrier", row.calibrationBrier(),
                "createdAt", row.createdAt()
            )).toList();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("summary", buildPerformanceSummary(tuple.getT1()));
            response.put("dailyMetrics", dailyMetrics);
            response.put("settledPredictions", settledPredictions);
            response.put("edgeBuckets", buildEdgeBuckets(tuple.getT2()));
            response.put("modelVersions", modelVersions);
            return response;
        });
    }

    public Mono<Map<String, Object>> getBetHistory(String userId) {
        return repository.findBetHistory(userId).collectList().map(rows -> {
            List<Map<String, Object>> bets = new ArrayList<>();
            Map<LocalDate, Map<String, Object>> dailyStats = new LinkedHashMap<>();

            int wins = 0;
            int losses = 0;
            int pushes = 0;
            int settledBets = 0;
            int openBets = 0;
            int overBets = 0;
            int underBets = 0;
            int overWins = 0;
            int underWins = 0;
            double totalStaked = 0;
            double netProfit = 0;
            double oddsSum = 0;

            for (ReadRepository.BetHistoryRow row : rows) {
                Double stake = row.stake() != null ? row.stake() : row.parlayStake();
                Double odds = row.odds() != null ? row.odds() : row.parlayOdds();
                Integer actualK = row.actualK() != null ? row.actualK() : row.predictionActualStrikeouts();
                String result = resolveResult(row.predictionResult(), row.recommendedSide(), row.line(), actualK);
                Double profit = row.storedProfit() != null ? row.storedProfit() : calculateProfit(result, stake, odds);

                Map<String, Object> bet = new LinkedHashMap<>();
                bet.put("id", row.id());
                bet.put("player", row.player());
                bet.put("matchup", buildMatchup(row.team(), row.opponent(), row.home()));
                bet.put("side", row.recommendedSide());
                bet.put("line", row.line());
                bet.put("placedAt", row.placedAt());
                bet.put("stake", stake);
                bet.put("odds", odds);
                bet.put("platform", row.platform());
                bet.put("result", result);
                bet.put("actualK", actualK);
                bet.put("profit", profit);
                bet.put("won", row.won() != null ? row.won() : ("GREEN".equals(result) ? Boolean.TRUE : "RED".equals(result) ? Boolean.FALSE : null));
                bet.put("liveStatus", inferLiveStatus(row.predictionDate(), actualK, row.predictionResult()));
                bet.put("parlayId", row.parlayId());
                bets.add(bet);

                if (stake != null) {
                    totalStaked += stake;
                }
                if (odds != null) {
                    oddsSum += odds;
                }
                if ("OVER".equals(row.recommendedSide())) {
                    overBets++;
                } else if ("UNDER".equals(row.recommendedSide())) {
                    underBets++;
                }
                if ("GREEN".equals(result)) {
                    wins++;
                    settledBets++;
                    netProfit += profit != null ? profit : 0;
                    if ("OVER".equals(row.recommendedSide())) overWins++;
                    if ("UNDER".equals(row.recommendedSide())) underWins++;
                } else if ("RED".equals(result)) {
                    losses++;
                    settledBets++;
                    netProfit += profit != null ? profit : 0;
                } else if ("PUSH".equals(result)) {
                    pushes++;
                    settledBets++;
                } else {
                    openBets++;
                }

                Map<String, Object> stat = dailyStats.computeIfAbsent(row.predictionDate(), date -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("statDate", date);
                    item.put("bets", 0);
                    item.put("wins", 0);
                    item.put("losses", 0);
                    item.put("staked", 0.0);
                    item.put("profit", 0.0);
                    item.put("roiPct", 0.0);
                    return item;
                });
                stat.put("bets", ((Integer) stat.get("bets")) + 1);
                stat.put("staked", ((Double) stat.get("staked")) + (stake != null ? stake : 0.0));
                if ("GREEN".equals(result)) {
                    stat.put("wins", ((Integer) stat.get("wins")) + 1);
                    stat.put("profit", ((Double) stat.get("profit")) + (profit != null ? profit : 0.0));
                } else if ("RED".equals(result)) {
                    stat.put("losses", ((Integer) stat.get("losses")) + 1);
                    stat.put("profit", ((Double) stat.get("profit")) + (profit != null ? profit : 0.0));
                }
                double staked = (Double) stat.get("staked");
                double profitValue = (Double) stat.get("profit");
                stat.put("roiPct", staked > 0 ? (profitValue / staked) * 100 : 0.0);
            }

            List<Map<String, Object>> dailyStatList = dailyStats.values().stream()
                .sorted((left, right) -> ((LocalDate) right.get("statDate")).compareTo((LocalDate) left.get("statDate")))
                .limit(60)
                .toList();

            int decidedBets = wins + losses;
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalBets", bets.size());
            summary.put("settledBets", settledBets);
            summary.put("openBets", openBets);
            summary.put("wins", wins);
            summary.put("losses", losses);
            summary.put("pushes", pushes);
            summary.put("winRate", decidedBets > 0 ? (double) wins / decidedBets : null);
            summary.put("totalStaked", totalStaked > 0 ? totalStaked : null);
            summary.put("netProfit", settledBets > 0 ? netProfit : null);
            summary.put("roi", totalStaked > 0 && settledBets > 0 ? (netProfit / totalStaked) * 100 : null);
            summary.put("avgOdds", bets.isEmpty() ? null : oddsSum / bets.size());
            summary.put("overBets", overBets);
            summary.put("underBets", underBets);
            summary.put("overWins", overWins);
            summary.put("underWins", underWins);

            return mapOf(
                "summary", summary,
                "bets", bets,
                "dailyStats", dailyStatList
            );
        });
    }

    public Mono<List<Map<String, Object>>> getAdminUsers() {
        return Mono.zip(
            repository.findAdminUsers().collectList(),
            repository.findAdminDailyStatAggregates().collectList(),
            repository.findAdminBetActivity().collectList()
        ).map(tuple -> {
            Map<String, ReadRepository.AdminDailyStatAggregateRow> statsMap = new LinkedHashMap<>();
            for (ReadRepository.AdminDailyStatAggregateRow row : tuple.getT2()) {
                statsMap.put(row.userId(), row);
            }
            Map<String, ReadRepository.AdminBetActivityRow> activityMap = new LinkedHashMap<>();
            for (ReadRepository.AdminBetActivityRow row : tuple.getT3()) {
                activityMap.put(row.userId(), row);
            }

            return tuple.getT1().stream().map(user -> {
                ReadRepository.AdminDailyStatAggregateRow stats = statsMap.get(user.id());
                ReadRepository.AdminBetActivityRow activity = activityMap.get(user.id());
                return mapOf(
                    "id", user.id(),
                    "email", user.email(),
                    "displayName", user.displayName(),
                    "isActive", user.isActive(),
                    "createdAt", user.createdAt(),
                    "totalBets", activity == null ? 0L : activity.totalBets(),
                    "totalProfit", stats == null ? null : stats.totalProfit(),
                    "lastActiveAt", activity == null ? null : activity.lastActiveAt()
                );
            }).toList();
        });
    }

    public Mono<Map<String, Object>> getPitcherHistory(String pitcherId, String opponent, LocalDate predictionDate, int limit) {
        Mono<List<Map<String, Object>>> historyMono = repository.findPitcherGameHistory(pitcherId, limit)
            .map(row -> mapOf(
                "id", row.id(),
                "game_date", row.gameDate(),
                "team", row.team(),
                "opponent", row.opponent(),
                "innings_pitched", row.inningsPitched(),
                "strikeouts", row.strikeouts(),
                "pitches_total", row.pitchesTotal()
            ))
            .collectList();

        Mono<Optional<String>> handMono = repository.findLatestPitcherHand(pitcherId, predictionDate).map(Optional::of).defaultIfEmpty(Optional.empty());
        Mono<Optional<ReadRepository.OpponentTrendRow>> trendMono = opponent == null
            ? Mono.just(Optional.empty())
            : repository.findOpponentTrend(opponent, predictionDate).map(Optional::of).defaultIfEmpty(Optional.empty());

        return Mono.zip(historyMono, handMono, trendMono).map(tuple -> {
            String pitcherHand = tuple.getT2().orElse(null);
            ReadRepository.OpponentTrendRow trend = tuple.getT3().orElse(null);
            Map<String, Object> opponentTrend = null;
            if (trend != null) {
                opponentTrend = new LinkedHashMap<>();
                opponentTrend.put("team", trend.team());
                opponentTrend.put("asOfDate", trend.asOfDate());
                opponentTrend.put("windowDays", trend.windowDays());
                opponentTrend.put("gamesSampled", trend.gamesSampled());
                opponentTrend.put("pitcherHand", pitcherHand);
                opponentTrend.put("kPctOverall", trend.kPctOverall());
                opponentTrend.put("kPctVsHand", "R".equals(pitcherHand) ? trend.kPctVsRhp() : "L".equals(pitcherHand) ? trend.kPctVsLhp() : null);
            }
            return mapOf(
                "history", tuple.getT1(),
                "opponentTrend", opponentTrend
            );
        });
    }

    private Map<String, Object> buildPerformanceSummary(List<ReadRepository.PerformanceMetricsRow> rows) {
        if (rows.isEmpty()) {
            return mapOf(
                "totalDates", 0,
                "totalSettled", 0,
                "overallWinRate", null,
                "overallRoi", null,
                "avgBrier", null,
                "avgMae", null,
                "avgEdge", null,
                "bestDate", null,
                "worstDate", null
            );
        }

        int totalGreen = 0;
        int totalDecided = 0;
        int totalSettled = 0;
        List<Double> briers = new ArrayList<>();
        List<Double> maes = new ArrayList<>();
        List<Double> edges = new ArrayList<>();
        List<Double> rois = new ArrayList<>();
        Double bestRoi = null;
        Double worstRoi = null;
        LocalDate bestDate = null;
        LocalDate worstDate = null;

        for (ReadRepository.PerformanceMetricsRow row : rows) {
            totalGreen += row.greenCount();
            totalDecided += row.greenCount() + row.redCount();
            totalSettled += row.settledCount();
            if (row.brierScore() != null) briers.add(row.brierScore());
            if (row.modelMae() != null) maes.add(row.modelMae());
            if (row.avgEdge() != null) edges.add(row.avgEdge());
            if (row.roiPct() != null) {
                rois.add(row.roiPct());
                if (bestRoi == null || row.roiPct() > bestRoi) {
                    bestRoi = row.roiPct();
                    bestDate = row.predictionDate();
                }
                if (worstRoi == null || row.roiPct() < worstRoi) {
                    worstRoi = row.roiPct();
                    worstDate = row.predictionDate();
                }
            }
        }

        return mapOf(
            "totalDates", rows.size(),
            "totalSettled", totalSettled,
            "overallWinRate", totalDecided > 0 ? (double) totalGreen / totalDecided : null,
            "overallRoi", average(rois),
            "avgBrier", average(briers),
            "avgMae", average(maes),
            "avgEdge", average(edges),
            "bestDate", bestDate,
            "worstDate", worstDate
        );
    }

    private List<Map<String, Object>> buildEdgeBuckets(List<ReadRepository.SettledPredictionRow> rows) {
        Map<String, int[]> buckets = new LinkedHashMap<>();
        buckets.put("0–5%", new int[] {0, 0});
        buckets.put("5–10%", new int[] {0, 0});
        buckets.put(">10%", new int[] {0, 0});

        for (ReadRepository.SettledPredictionRow row : rows) {
            if (row.result() == null || "PUSH".equals(row.result()) || "OPEN".equals(row.result())) {
                continue;
            }
            Double edge = "OVER".equals(row.recommendedSide()) ? row.edgeOver()
                : "UNDER".equals(row.recommendedSide()) ? row.edgeUnder()
                : null;
            String label = bucketEdge(edge);
            if (label == null) {
                continue;
            }
            int[] values = buckets.get(label);
            if ("GREEN".equals(row.result())) values[0]++;
            if ("RED".equals(row.result())) values[1]++;
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : buckets.entrySet()) {
            int wins = entry.getValue()[0];
            int losses = entry.getValue()[1];
            int predictions = wins + losses;
            response.add(mapOf(
                "label", entry.getKey(),
                "predictions", predictions,
                "wins", wins,
                "losses", losses,
                "winRate", predictions > 0 ? (double) wins / predictions : null
            ));
        }
        return response;
    }

    private String bucketEdge(Double edge) {
        if (edge == null) return null;
        if (edge < 0.05) return "0–5%";
        if (edge < 0.10) return "5–10%";
        return ">10%";
    }

    private Double average(List<Double> values) {
        return values.isEmpty() ? null : values.stream().reduce(0.0, Double::sum) / values.size();
    }

    private Double selectBestEdge(String side, Double edgeOver, Double edgeUnder) {
        if ("OVER".equals(side)) return edgeOver;
        if ("UNDER".equals(side)) return edgeUnder;
        if (edgeOver == null && edgeUnder == null) return null;
        return Math.max(edgeOver != null ? edgeOver : Double.NEGATIVE_INFINITY, edgeUnder != null ? edgeUnder : Double.NEGATIVE_INFINITY);
    }

    private Double selectOdds(String side, Double overOdds, Double underOdds) {
        if ("OVER".equals(side)) return overOdds;
        if ("UNDER".equals(side)) return underOdds;
        return null;
    }

    private String buildMatchup(String team, String opponent, Boolean home) {
        if (team != null && opponent != null) {
            return Boolean.TRUE.equals(home) ? team + " vs " + opponent : team + " @ " + opponent;
        }
        return team != null ? team : opponent != null ? opponent : "—";
    }

    private Double calculateProfit(String result, Double stake, Double odds) {
        if (stake == null || odds == null) return null;
        if ("GREEN".equals(result)) return stake * (odds - 1);
        if ("RED".equals(result)) return -stake;
        if ("PUSH".equals(result)) return 0.0;
        return null;
    }

    private String resolveResult(String storedResult, String side, Double line, Integer actualK) {
        if (storedResult != null && !"OPEN".equals(storedResult)) {
            return storedResult;
        }
        if (!"OVER".equals(side) && !"UNDER".equals(side)) return storedResult;
        if (line == null || actualK == null) return storedResult;
        if (actualK.doubleValue() == line) return "PUSH";
        if ("OVER".equals(side)) return actualK > line ? "GREEN" : "RED";
        return actualK < line ? "GREEN" : "RED";
    }

    private String inferLiveStatus(LocalDate predictionDate, Integer actualK, String predictionResult) {
        boolean recent = predictionDate != null
            && predictionDate.atStartOfDay().toInstant(ZoneOffset.UTC).isAfter(Instant.now().minusSeconds(RECENT_FINAL_RECONCILIATION_DAYS * 86400L));
        if (actualK != null || (predictionResult != null && !"OPEN".equals(predictionResult))) {
            return "FINAL";
        }
        return recent ? "SCHEDULED" : null;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], entries[i + 1]);
        }
        return map;
    }
}
