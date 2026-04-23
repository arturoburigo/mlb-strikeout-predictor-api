package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;
import java.time.Instant;

@Introspected
public record PipelineRunResponse(
    long id,
    String jobName,
    String status,
    Instant startedAt,
    Instant completedAt,
    String errorLog
) {
}
