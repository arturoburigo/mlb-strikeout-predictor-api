package com.mlbsk.api.controller;

import com.mlbsk.api.dto.CreateUserRequest;
import com.mlbsk.api.dto.DeleteBetRequest;
import com.mlbsk.api.dto.ParlayBetRequest;
import com.mlbsk.api.dto.SavePreferencesRequest;
import com.mlbsk.api.dto.SetBetResultRequest;
import com.mlbsk.api.dto.ToggleUserActiveRequest;
import com.mlbsk.api.dto.TrackedBetRequest;
import com.mlbsk.api.dto.UpdateBetRequest;
import com.mlbsk.api.service.WriteService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import reactor.core.publisher.Mono;

@Controller("/api/v1")
public class WriteController {

    private final WriteService writeService;

    public WriteController(WriteService writeService) {
        this.writeService = writeService;
    }

    @Post("/preferences")
    public Mono<HttpResponse<Void>> savePreferences(@Body SavePreferencesRequest request) {
        return writeService.savePreferences(request).thenReturn(HttpResponse.ok());
    }

    @Post("/bets/tracked")
    public Mono<HttpResponse<Void>> trackedBet(@Body TrackedBetRequest request) {
        return writeService.upsertTrackedBet(request).thenReturn(HttpResponse.ok());
    }

    @Post("/bets/parlay")
    public Mono<HttpResponse<Void>> parlayBet(@Body ParlayBetRequest request) {
        return writeService.placeParlayBet(request).thenReturn(HttpResponse.ok());
    }

    @Post("/bets/update")
    public Mono<HttpResponse<Void>> updateBet(@Body UpdateBetRequest request) {
        return writeService.updateBet(request).thenReturn(HttpResponse.ok());
    }

    @Post("/bets/result")
    public Mono<HttpResponse<Void>> setBetResult(@Body SetBetResultRequest request) {
        return writeService.setBetResult(request).thenReturn(HttpResponse.ok());
    }

    @Post("/bets/delete")
    public Mono<HttpResponse<Void>> deleteBet(@Body DeleteBetRequest request) {
        return writeService.deleteBet(request).thenReturn(HttpResponse.ok());
    }

    @Post("/admin/users")
    public Mono<HttpResponse<Void>> createUser(@Body CreateUserRequest request) {
        return writeService.createUser(request).thenReturn(HttpResponse.ok());
    }

    @Post("/admin/users/toggle-active")
    public Mono<HttpResponse<Void>> toggleUserActive(
        @Body ToggleUserActiveRequest request,
        @Header("X-Actor-User-Id") String actorUserId
    ) {
        return writeService.toggleUserActive(request, actorUserId).thenReturn(HttpResponse.ok());
    }
}
