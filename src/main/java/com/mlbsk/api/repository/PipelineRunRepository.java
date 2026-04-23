package com.mlbsk.api.repository;

import com.mlbsk.api.domain.PipelineRunRow;
import reactor.core.publisher.Flux;

public interface PipelineRunRepository {

    Flux<PipelineRunRow> findAll(int limit, int offset);
}
