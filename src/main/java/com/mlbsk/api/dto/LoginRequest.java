package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record LoginRequest(
    String email,
    String password
) {
}
