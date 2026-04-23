package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;
import java.util.List;

@Introspected
public record ParlayBetRequest(
    String userId,
    List<Long> predictionIds,
    Double stake,
    Double odds,
    String platform
) {
}
