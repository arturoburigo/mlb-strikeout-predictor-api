package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record ToggleUserActiveRequest(
    String userId,
    Boolean currentActive
) {
}
