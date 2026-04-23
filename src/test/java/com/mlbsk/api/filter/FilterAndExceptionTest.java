package com.mlbsk.api.filter;

import com.mlbsk.api.exception.ErrorResponse;
import com.mlbsk.api.exception.ResourceNotFoundException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Controller("/api/test")
class TestController {

    @Get("/ok")
    String ok() {
        return "OK";
    }

    @Get("/not-found")
    String notFound() {
        throw new ResourceNotFoundException("Test resource not found");
    }
}

@Controller("/public/test")
class PublicTestController {

    @Get("/ok")
    String ok() {
        return "Public OK";
    }
}

@MicronautTest
class FilterAndExceptionTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testPublicEndpointAllowedWithoutKey() {
        var response = client.toBlocking().exchange(HttpRequest.GET("/public/test/ok"), String.class);
        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertEquals("Public OK", response.body());
        Assertions.assertNotNull(response.header("X-Request-Id"));
    }

    @Test
    void testApiEndpointFailsWithoutKey() {
        HttpClientResponseException exception = Assertions.assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(HttpRequest.GET("/api/test/ok"), String.class)
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        Assertions.assertNotNull(exception.getResponse().header("X-Request-Id"));
    }

    @Test
    void testApiEndpointSucceedsWithCorrectKey() {
        var response = client.toBlocking().exchange(
            HttpRequest.GET("/api/test/ok").header("X-API-Key", "default-dev-key"),
            String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertEquals("OK", response.body());
        Assertions.assertNotNull(response.header("X-Request-Id"));
    }

    @Test
    void testApiEndpointFailsWithWrongKey() {
        HttpClientResponseException exception = Assertions.assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(
                HttpRequest.GET("/api/test/ok").header("X-API-Key", "wrong-key"),
                String.class
            )
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void testResourceNotFoundExceptionHandler() {
        HttpClientResponseException exception = Assertions.assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(
                HttpRequest.GET("/api/test/not-found").header("X-API-Key", "default-dev-key"),
                ErrorResponse.class
            )
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        ErrorResponse errorResponse = exception.getResponse().getBody(ErrorResponse.class).orElse(null);
        Assertions.assertNotNull(errorResponse);
        Assertions.assertEquals("about:blank", errorResponse.type());
        Assertions.assertEquals("Resource Not Found", errorResponse.title());
        Assertions.assertEquals(404, errorResponse.status());
        Assertions.assertEquals("Test resource not found", errorResponse.detail());
        Assertions.assertEquals("/api/test/not-found", errorResponse.instance());
        Assertions.assertNotNull(errorResponse.requestId());
        Assertions.assertEquals(exception.getResponse().header("X-Request-Id"), errorResponse.requestId());
    }
}
