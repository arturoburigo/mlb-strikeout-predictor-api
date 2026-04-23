package com.mlbsk.api.dto;

import io.micronaut.core.annotation.Introspected;
import java.util.List;

@Introspected
public record PaginatedResponse<T>(
    List<T> items,
    int page,
    int size,
    Long total
) {
    public PaginatedResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public PaginatedResponse(List<T> items, int page, int size) {
        this(items, page, size, null);
    }
}
