package com.mlbsk.api.controller;

import com.mlbsk.api.service.ReadService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

@Controller("/api/v1")
public class ReadController {

    private final ReadService service;

    public ReadController(ReadService service) {
        this.service = service;
    }

    @Get("/dashboard")
    public Mono<Map<String, Object>> dashboard(@QueryValue String userId) {
        return service.getDashboard(userId);
    }

    @Get("/performance")
    public Mono<Map<String, Object>> performance() {
        return service.getPerformance();
    }

    @Get("/bets")
    public Mono<Map<String, Object>> bets(@QueryValue String userId) {
        return service.getBetHistory(userId);
    }

    @Get("/admin/users")
    public Mono<List<Map<String, Object>>> adminUsers() {
        return service.getAdminUsers();
    }

    @Get("/pitcher-history")
    public Mono<Map<String, Object>> pitcherHistory(
        @QueryValue String pitcherId,
        @Nullable @QueryValue String opponent,
        @Nullable @QueryValue LocalDate predictionDate,
        @QueryValue(defaultValue = "5") int limit
    ) {
        return service.getPitcherHistory(pitcherId, opponent, predictionDate, limit);
    }
}
