package com.ExamPort.ExamPort.Security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    public Key getKey() {
        return key;
    }
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 5; // 5 hours

    public String generateToken(String username, String role) {
        logger.debug("Generating JWT token for user: {} with role: {}", username, role);
        
        try {
            Map<String, Object> claims = new HashMap<>();
            // Ensure the role is prefixed with 'ROLE_'
            String springRole = role.startsWith("ROLE_") ? role : ("ROLE_" + role.toUpperCase());
            claims.put("roles", java.util.Arrays.asList(springRole));
            
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(username)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(key)
                    .compact();
            
            logger.info("JWT token generated successfully for user: {}", username);
            return token;
        } catch (Exception e) {
            logger.error("Error generating JWT token for user: {}", username, e);
            throw e;
        }
    }

    public String extractUsername(String token) {
        try {
            String username = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
            logger.debug("Extracted username from token: {}", username);
            return username;
        } catch (Exception e) {
            logger.warn("Error extracting username from JWT token: {}", e.getMessage());
            throw e;
        }
    }

    public String extractRole(String token) {
        try {
            String role = (String) Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role");
            logger.debug("Extracted role from token: {}", role);
            return role;
        } catch (Exception e) {
            logger.warn("Error extracting role from JWT token: {}", e.getMessage());
            throw e;
        }
    }
}
