package com.structexam.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> DEFAULT_EXCLUSION_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Value("${jwt.secret:structexam-secret-key-for-jwt-token-generation}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isExclusionPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null || !validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String userId = getUserIdFromToken(token);
        String username = getUsernameFromToken(token);
        String role = getRoleFromToken(token);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isExclusionPath(String path) {
        return DEFAULT_EXCLUSION_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            byte[] keyBytes = jwtSecret.getBytes();
            javax.crypto.SecretKey signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes.length < 32 ? padKey(keyBytes) : keyBytes);
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().after(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    private String getUserIdFromToken(String token) {
        byte[] keyBytes = jwtSecret.getBytes();
        javax.crypto.SecretKey signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes.length < 32 ? padKey(keyBytes) : keyBytes);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    private String getUsernameFromToken(String token) {
        byte[] keyBytes = jwtSecret.getBytes();
        javax.crypto.SecretKey signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes.length < 32 ? padKey(keyBytes) : keyBytes);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username", String.class);
    }

    private String getRoleFromToken(String token) {
        byte[] keyBytes = jwtSecret.getBytes();
        javax.crypto.SecretKey signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes.length < 32 ? padKey(keyBytes) : keyBytes);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    private byte[] padKey(byte[] keyBytes) {
        byte[] paddedKey = new byte[32];
        System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
        return paddedKey;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
