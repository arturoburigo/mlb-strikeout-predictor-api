package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record LoginResponse(
    String id,
    String email,
    String displayName,
    boolean active
) {
}
