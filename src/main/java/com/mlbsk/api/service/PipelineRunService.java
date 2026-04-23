package com.mlbsk.api.service;

import com.mlbsk.api.dto.PipelineRunResponse;
import com.mlbsk.api.mapper.PipelineRunMapper;
import com.mlbsk.api.repository.PipelineRunRepository;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;

@Singleton
public class PipelineRunService {

    private final PipelineRunRepository repository;

    public PipelineRunService(PipelineRunRepository repository) {
        this.repository = repository;
    }

    public Flux<PipelineRunResponse> getPipelineRuns(int limit, int offset) {
        return repository.findAll(limit, offset).map(PipelineRunMapper::toResponse);
    }
}
