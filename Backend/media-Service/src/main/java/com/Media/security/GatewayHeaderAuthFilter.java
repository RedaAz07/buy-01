package com.Media.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader("X-Authenticated-UserID");
        String rolesHeader = request.getHeader("X-Authenticated-Roles");

        if (userId != null && !userId.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Format roles into GrantedAuthorities (e.g., "ROLE_ADMIN" or "ROLE_USER")
            List<SimpleGrantedAuthority> authorities = List.of();
            if (rolesHeader != null && !rolesHeader.isBlank()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            // Set userId as the Principal
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}