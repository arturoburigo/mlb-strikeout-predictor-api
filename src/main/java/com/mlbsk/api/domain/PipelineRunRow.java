package com.mlbsk.api.domain;

import java.time.Instant;

public record PipelineRunRow(
    long id,
    String jobName,
    String status,
    Instant startedAt,
    Instant completedAt,
    String errorLog
) {
}
