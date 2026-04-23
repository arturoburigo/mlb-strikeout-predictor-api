package com.mlbsk.api.controller;

import com.mlbsk.api.dto.LoginRequest;
import com.mlbsk.api.dto.LoginResponse;
import com.mlbsk.api.service.WriteService;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import static io.micronaut.http.HttpResponse.ok;
import static io.micronaut.http.HttpResponse.unauthorized;
import reactor.core.publisher.Mono;

@Controller("/public/auth")
public class PublicAuthController {

    private final WriteService writeService;

    public PublicAuthController(WriteService writeService) {
        this.writeService = writeService;
    }

    @Post("/login")
    public Mono<MutableHttpResponse<LoginResponse>> login(@Body LoginRequest request) {
        return writeService.login(request)
            .map(response -> ok(response))
            .defaultIfEmpty(unauthorized());
    }
}
