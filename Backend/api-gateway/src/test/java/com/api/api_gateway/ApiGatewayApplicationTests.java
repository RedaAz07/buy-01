package com.api.api_gateway;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.api.api_gateway.security.Filter;
import com.api.api_gateway.security.jwtUtil;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ApiGatewayApplicationTests {

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private jwtUtil jwtUtil;

    private Filter filter;

    @BeforeEach
    void setUp() {
        filter = new Filter(jwtUtil);
    }

    @Test
    void shouldRejectRequestWithoutToken() {
        // 1. Arrange: Create a mock request without Authorization header
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/resource").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 2. Act & Assert: Execute the reactive chain and verify response
        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        // Verify that HTTP status is 401 UNAUTHORIZED
        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;

        // Verify that the request was never passed down the filter chain
        verify(chain, never()).filter(any());
    }
}