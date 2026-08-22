package com.Media.Service.security;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtUtil {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningkey()).build().parseClaimsJws(token).getBody();
    }

    public Key getSigningkey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Map<String, Object> extractUserClaims(String token) {

        Claims claims = extractAllClaims(token);

        Map<String, Object> userClaims = new HashMap<>();

        userClaims.put("username", claims.getSubject());
        userClaims.put("role", claims.get("role"));

        return userClaims;
    }

}