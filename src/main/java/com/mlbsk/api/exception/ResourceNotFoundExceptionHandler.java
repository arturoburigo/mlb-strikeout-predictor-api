package com.mlbsk.api.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {ResourceNotFoundException.class, ExceptionHandler.class})
public class ResourceNotFoundExceptionHandler
    implements ExceptionHandler<ResourceNotFoundException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, ResourceNotFoundException exception) {
        String requestId = request.getAttribute("requestId", String.class).orElse("unknown");
        ErrorResponse errorResponse = new ErrorResponse(
            "about:blank",
            "Resource Not Found",
            HttpStatus.NOT_FOUND.getCode(),
            exception.getMessage() != null ? exception.getMessage() : "The requested resource was not found",
            request.getPath(),
            requestId
        );
        return HttpResponse.<ErrorResponse>notFound()
            .body(errorResponse)
            .header("Content-Type", "application/problem+json");
    }
}
