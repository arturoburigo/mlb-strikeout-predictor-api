package com.mlbsk.api.service;

import com.mlbsk.api.dto.PaginatedResponse;
import com.mlbsk.api.dto.PredictionResponse;
import com.mlbsk.api.mapper.PredictionMapper;
import com.mlbsk.api.repository.PredictionRepository;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

@Singleton
public class PredictionService {

    private final PredictionRepository repository;
    private final PredictionMapper mapper;

    public PredictionService(PredictionRepository repository, PredictionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Mono<PaginatedResponse<PredictionResponse>> getPredictionsByDate(@Nullable LocalDate date, int page, int size) {
        int offset = page * size;
        return repository.findByDate(date, size, offset)
            .map(mapper::toResponse)
            .collectList()
            .map(items -> new PaginatedResponse<>(items, page, size));
    }
}
