package com.hiba.meeting_backend.Security;

import java.util.Date;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JwtUtil {

    private final String ACCESS_SECRET = "ACCESS_SECRET_123";
    private final String REFRESH_SECRET = "REFRESH_SECRET_456";

    private final long ACCESS_EXP = 15 * 60 * 1000; // 15 min
    private final long REFRESH_EXP = 7L * 24 * 60 * 60 * 1000; // 7 jours

    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXP))
                .signWith(SignatureAlgorithm.HS256, ACCESS_SECRET)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXP))
                .signWith(SignatureAlgorithm.HS256, REFRESH_SECRET)
                .compact();
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .setSigningKey(ACCESS_SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    public Claims validateRefreshToken(String token) {
        return Jwts.parser()
                .setSigningKey(REFRESH_SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}