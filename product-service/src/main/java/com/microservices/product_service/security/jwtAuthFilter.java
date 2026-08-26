package com.microservices.product_service.security;

import java.io.IOException;
import java.util.Map;
import java.util.List;

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
public class jwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil JwtUtil;

    public jwtAuthFilter(JwtUtil JwtUtil) {
        this.JwtUtil = JwtUtil;
    }

    @Override
    public void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String t = token.substring(7);
            Map<String, Object> j = JwtUtil.extractUserClaims(t);
            System.out.println(j);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        j.get("username"),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        j.get("role").toString().toUpperCase())));

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
            }

        } catch (Exception e) {
            // TODO: handle exception
            System.out.println("error:" + e);
            SecurityContextHolder.clearContext();

        }
        filterChain.doFilter(request, response);
    }
}
