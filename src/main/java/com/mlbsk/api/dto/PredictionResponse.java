package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;
import java.time.LocalDate;

@Introspected
public record PredictionResponse(
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
    Double bestEdge,
    String bookmaker,
    String result,
    String aiInsight
) {
}
