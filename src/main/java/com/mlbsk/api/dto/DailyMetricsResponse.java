package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;
import java.time.LocalDate;

@Introspected
public record DailyMetricsResponse(
    LocalDate date,
    Double mae,
    Double logLoss,
    Double avgEv,
    Double hitRate,
    int totalPredictions
) {
}
