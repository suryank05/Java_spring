package com.ExamPort.ExamPort.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip JWT processing for auth endpoints and other public endpoints
        if (requestURI.startsWith("/api/auth/") || 
            requestURI.startsWith("/auth/") || 
            requestURI.equals("/welcome") || 
            requestURI.equals("/health") ||
            requestURI.startsWith("/api/courses/public") ||
            method.equals("OPTIONS")) {
            logger.debug("Skipping JWT processing for public endpoint: {} {}", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        logger.debug("Processing request: {} {}", method, requestURI);
        
        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                logger.debug("JWT token found for user: {}", username);
            } catch (Exception e) {
                logger.warn("Invalid JWT token in request to {}: {}", requestURI, e.getMessage());
            }
        } else {
            logger.debug("No Bearer token found in request to: {}", requestURI);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Extract roles array from JWT
                Claims claims = Jwts.parserBuilder().setSigningKey(jwtUtil.getKey()).build().parseClaimsJws(jwt).getBody();
                List<String> roles = claims.get("roles", List.class);
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                    logger.debug("User {} authenticated with roles: {}", username, roles);
                } else {
                    logger.warn("No roles found in JWT for user: {}", username);
                }
                
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                logger.debug("Security context set for user: {}", username);
            } catch (Exception e) {
                logger.error("Error processing JWT token for user: {}", username, e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
