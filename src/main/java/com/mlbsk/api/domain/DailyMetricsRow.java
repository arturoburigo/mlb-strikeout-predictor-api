package com.mlbsk.api.domain;

import java.time.LocalDate;

public record DailyMetricsRow(
    LocalDate date,
    Double mae,
    Double logLoss,
    Double avgEv,
    Double hitRate,
    int totalPredictions
) {
}
