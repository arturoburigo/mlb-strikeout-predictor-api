package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record SetBetResultRequest(
    String userId,
    String betId,
    String result
) {
}
