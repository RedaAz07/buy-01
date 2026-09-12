package com.user_service.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtUtil {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

  
    public Key getSigningkey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

  
  

    public String generateToken(UserDetails userDetails, String id) {

        Map<String, Object> claims = new HashMap<>();

        claims.put(
                "userId",
                id);
        claims.put(
                "role",
                userDetails.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority());

        return createToken(claims, userDetails.getUsername());
    }

   

    public String createToken(Map<String, Object> extractClaims, String username) {
        return Jwts.builder().setClaims(extractClaims).setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningkey(), SignatureAlgorithm.HS256).compact();
    }

     
}