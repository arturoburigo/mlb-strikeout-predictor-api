package com.mlbsk.api.domain;

import java.time.LocalDate;

public record PredictionRow(
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
    Double overProbability,
    Double underProbability,
    String recommendedSide,
    Double edgeOver,
    Double edgeUnder,
    String bookmaker,
    String result,
    String aiInsight
) {
}
