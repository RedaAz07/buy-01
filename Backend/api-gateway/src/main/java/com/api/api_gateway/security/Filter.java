package com.api.api_gateway.security;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

@Component
public class Filter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/", "/actuator/", "/products", "/api/products");

    private final jwtUtil jwtUtil;

    public Filter(jwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleanedRequest = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-Authenticated-UserID");
                    h.remove("X-Authenticated-Roles");
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate().request(cleanedRequest).build();

        if (isPublic(sanitizedExchange)) {
            return chain.filter(sanitizedExchange);
        }

        String authorization = sanitizedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(sanitizedExchange);
        }

        try {
            Claims claims = jwtUtil.parseClaims(authorization.substring(7));

            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                return unauthorized(sanitizedExchange);
            }
            String userId = userIdObj.toString();

            Object rolesObj = claims.get("role");
            String roles = rolesObj != null ? rolesObj.toString() : "";

            ServerWebExchange authenticatedExchange = sanitizedExchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-Authenticated-UserID", userId);
                        headers.set("X-Authenticated-Roles", roles);
                    }))
                    .build();
            System.err.println("-----------------------------------" + sanitizedExchange.getResponse().getHeaders());
            return chain.filter(authenticatedExchange);
        } catch (JwtException | IllegalArgumentException exception) {
            return unauthorized(sanitizedExchange);
        }
    }

    private boolean isPublic(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (method == HttpMethod.OPTIONS) {
            return true;
        }

        if (path.startsWith("/api/auth/") || path.startsWith("/actuator/")) {
            return true;
        }

        if (method == HttpMethod.GET && (path.startsWith("/products") || path.startsWith("/api/products"))) {
            return true;
        }

        return false;
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