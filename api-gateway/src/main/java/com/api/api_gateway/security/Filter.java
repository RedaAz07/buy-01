package com.api.api_gateway.security;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class Filter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/", "/actuator/");

    private final jwtUtil jwtUtil;

    public Filter(jwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isPublic(exchange)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = jwtUtil.parseClaims(authorization.substring(7));
            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                return unauthorized(exchange);
            }

            ServerWebExchange authenticatedExchange = exchange.mutate()
                    .request(request -> request.headers(headers -> headers.set("X-Authenticated-User", username)))
                    .build();
            return chain.filter(authenticatedExchange);
        } catch (RuntimeException exception) {
            return unauthorized(exchange);
        }
    }

    private boolean isPublic(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        return exchange.getRequest().getMethod() == HttpMethod.OPTIONS
                || PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
