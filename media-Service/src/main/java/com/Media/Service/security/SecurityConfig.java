package com.Media.Service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;

    public SecurityConfig(JwtAuthFilter JwtFilter) {
    this.jwtFilter = JwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        (req, res, authException) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error")))
                .authorizeHttpRequests(auth ->

                auth.requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/media/images").hasRole("SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/media/images/{id}").hasRole("SELLER")
                        .anyRequest().authenticated())
         .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }



    
}