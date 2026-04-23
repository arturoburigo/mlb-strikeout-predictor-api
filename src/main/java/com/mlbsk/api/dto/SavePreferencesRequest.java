package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record SavePreferencesRequest(
    String userId,
    Double defaultStake,
    String defaultPlatform,
    Double minEdgePercent
) {
}
