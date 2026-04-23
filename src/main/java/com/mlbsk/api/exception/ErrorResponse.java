package com.mlbsk.api.exception;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record ErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    String requestId
) {
}
