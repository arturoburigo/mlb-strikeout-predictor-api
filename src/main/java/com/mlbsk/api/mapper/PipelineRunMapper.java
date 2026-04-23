package com.mlbsk.api.mapper;

import com.mlbsk.api.domain.PipelineRunRow;
import com.mlbsk.api.dto.PipelineRunResponse;

public final class PipelineRunMapper {

    private PipelineRunMapper() {
    }

    public static PipelineRunResponse toResponse(PipelineRunRow row) {
        return new PipelineRunResponse(
            row.id(),
            row.jobName(),
            row.status(),
            row.startedAt(),
            row.completedAt(),
            row.errorLog()
        );
    }
}
