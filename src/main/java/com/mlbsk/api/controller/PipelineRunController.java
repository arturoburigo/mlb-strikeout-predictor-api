package com.mlbsk.api.controller;

import com.mlbsk.api.dto.PaginatedResponse;
import com.mlbsk.api.dto.PipelineRunResponse;
import com.mlbsk.api.service.PipelineRunService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import reactor.core.publisher.Mono;

@Controller("/api/v1/pipeline-runs")
public class PipelineRunController {

    private final PipelineRunService service;

    public PipelineRunController(PipelineRunService service) {
        this.service = service;
    }

    @Get
    public Mono<PaginatedResponse<PipelineRunResponse>> list(
        @QueryValue(defaultValue = "1") int page,
        @QueryValue(defaultValue = "20") int size
    ) {
        int offset = (page - 1) * size;
        return service.getPipelineRuns(size, offset)
            .collectList()
            .map(items -> new PaginatedResponse<>(items, page, size));
    }
}
