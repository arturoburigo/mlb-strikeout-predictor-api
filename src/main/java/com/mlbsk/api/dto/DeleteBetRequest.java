package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record DeleteBetRequest(
    String userId,
    String betId
) {
}
