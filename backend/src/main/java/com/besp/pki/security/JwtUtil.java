package com.besp.pki.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, String role, String device, String ip) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        
        // Generate unique JWT ID (jti)
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti) // JWT ID for tracking
                .setSubject(subject)
                .claim("roles", List.of(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .claim("device", device) // Device/browser info
                .claim("ip", ip) // IP address
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validate(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }
    
    public String getJti(String token) {
        return getClaims(token).getId();
    }
    
    public String getDevice(String token) {
        Claims claims = getClaims(token);
        return claims.get("device", String.class);
    }
    
    public String getIp(String token) {
        Claims claims = getClaims(token);
        return claims.get("ip", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}





