package com.microservices.product_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

    public SecurityConfig(GatewayHeaderAuthFilter gatewayHeaderAuthFilter) {
        this.gatewayHeaderAuthFilter = gatewayHeaderAuthFilter;
    }

    @Bean
    public SecurityFilterChain SecurityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/my").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**").hasRole("SELLER")
                        .anyRequest().authenticated())
                .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}