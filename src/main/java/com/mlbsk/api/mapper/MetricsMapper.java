package com.mlbsk.api.mapper;

import com.mlbsk.api.domain.DailyMetricsRow;
import com.mlbsk.api.dto.DailyMetricsResponse;
import jakarta.inject.Singleton;

@Singleton
public class MetricsMapper {

    public DailyMetricsResponse toDto(DailyMetricsRow row) {
        return new DailyMetricsResponse(
            row.date(),
            row.mae(),
            row.logLoss(),
            row.avgEv(),
            row.hitRate(),
            row.totalPredictions()
        );
    }
}
