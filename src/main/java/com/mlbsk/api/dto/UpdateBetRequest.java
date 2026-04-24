package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record UpdateBetRequest(
    String userId,
    String betId,
    Double stake,
    Double odds,
    String platform
) {
}
