package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record CreateUserRequest(
    String email,
    String displayName,
    String password
) {
}
