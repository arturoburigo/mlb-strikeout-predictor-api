package com.mlbsk.api.filter;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@Filter("/**")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApmHttpServerFilter implements HttpServerFilter {

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Mono.defer(() -> {
            Transaction transaction = ElasticApm.startTransaction();

            transaction.setName(request.getMethodName() + " " + request.getPath());
            transaction.setType("request");
            transaction.addLabel("http.method", request.getMethodName());
            transaction.addLabel("http.path", request.getPath());

            return Mono.from(chain.proceed(request))
                .doOnNext(response -> {
                    int statusCode = response.code();
                    transaction.setResult("HTTP " + statusCode);
                    transaction.addLabel("http.status_code", statusCode);
                })
                .doOnError(transaction::captureException)
                .doFinally(signalType -> {
                    transaction.end();
                });
        });
    }
}
