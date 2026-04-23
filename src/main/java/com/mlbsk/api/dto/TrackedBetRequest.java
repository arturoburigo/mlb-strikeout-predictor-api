package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record TrackedBetRequest(
    String userId,
    Long predictionId,
    Double stake,
    Double odds,
    String platform
) {
}
