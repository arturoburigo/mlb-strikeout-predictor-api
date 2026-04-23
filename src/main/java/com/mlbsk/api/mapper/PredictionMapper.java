package com.mlbsk.api.mapper;

import com.mlbsk.api.domain.PredictionRow;
import com.mlbsk.api.dto.PredictionResponse;
import jakarta.inject.Singleton;
import java.util.stream.Stream;

@Singleton
public class PredictionMapper {

    private Double selectBestEdge(PredictionRow row) {
        if ("OVER".equals(row.recommendedSide())) {
            return row.edgeOver();
        }
        if ("UNDER".equals(row.recommendedSide())) {
            return row.edgeUnder();
        }
        return Stream.of(row.edgeOver(), row.edgeUnder())
            .filter(value -> value != null)
            .max(Double::compareTo)
            .orElse(null);
    }

    public PredictionResponse toResponse(PredictionRow row) {
        return new PredictionResponse(
            row.id(),
            row.predictionDate(),
            row.player(),
            row.team(),
            row.opponent(),
            row.home(),
            row.line(),
            row.overOdds(),
            row.underOdds(),
            row.expectedStrikeouts(),
            row.overProbability(),
            row.underProbability(),
            row.recommendedSide(),
            selectBestEdge(row),
            row.bookmaker(),
            row.result(),
            row.aiInsight()
        );
    }
}
