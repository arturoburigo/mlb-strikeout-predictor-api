package com.mlbsk.api.controller;

import com.mlbsk.api.dto.PaginatedResponse;
import com.mlbsk.api.dto.PredictionResponse;
import com.mlbsk.api.service.PredictionService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

@Controller("/api/v1/predictions")
public class PredictionController {

    private final PredictionService service;

    public PredictionController(PredictionService service) {
        this.service = service;
    }

    @Get
    public Mono<PaginatedResponse<PredictionResponse>> list(
        @Nullable @QueryValue LocalDate date,
        @QueryValue(defaultValue = "0") int page,
        @QueryValue(defaultValue = "50") int size
    ) {
        return service.getPredictionsByDate(date, page, size);
    }
}
