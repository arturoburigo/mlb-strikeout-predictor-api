package com.mlbsk.api.filter;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import java.util.UUID;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

@Filter(Filter.MATCH_ALL_PATTERN)
public class RequestIdFilter implements HttpServerFilter {

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String headerRequestId = request.getHeaders().get("X-Request-Id");
        final String requestId = (headerRequestId == null || headerRequestId.isBlank())
            ? UUID.randomUUID().toString()
            : headerRequestId;
        request.setAttribute("requestId", requestId);

        return Flux.from(chain.proceed(request)).map(response -> {
            response.header("X-Request-Id", requestId);
            return response;
        });
    }
}
