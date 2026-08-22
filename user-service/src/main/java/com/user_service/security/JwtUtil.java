package com.user_service.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
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

    public String extractUsername(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> calimResolve) {
        final Claims claims = extractAllClaims(token);
        return calimResolve.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningkey()).build().parseClaimsJws(token).getBody();
    }

    public Key getSigningkey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValide(String jwt, UserDetails userDetails) {
        final String username = extractUsername(jwt);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(jwt));
    }

    public boolean isTokenExpired(String token) {

        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String Token) {

        return extractClaim(Token, Claims::getExpiration);
    }

    public String generateToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        claims.put(
                "role",
                userDetails.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority());

        return createToken(claims, userDetails.getUsername());
    }

    public Map<String, Object> extractUserClaims(String token) {

        Claims claims = extractAllClaims(token);

        Map<String, Object> userClaims = new HashMap<>();

        userClaims.put("username", claims.getSubject());
        userClaims.put("role", claims.get("role"));

        return userClaims;
    }

    public String createToken(Map<String, Object> extractClaims, String username) {
        return Jwts.builder().setClaims(extractClaims).setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningkey(), SignatureAlgorithm.HS256).compact();
    }
}