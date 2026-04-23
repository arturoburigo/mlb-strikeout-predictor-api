package com.mlbsk.api.filter;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@Filter("/api/**")
public class AuthFilter implements HttpServerFilter {

    private final String configuredApiKey;

    public AuthFilter(@Property(name = "api.key") String configuredApiKey) {
        this.configuredApiKey = configuredApiKey;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String apiKey = request.getHeaders().get("X-API-Key");
        if (apiKey != null && apiKey.equals(configuredApiKey)) {
            return chain.proceed(request);
        }
        return Mono.just(HttpResponse.unauthorized());
    }
}
