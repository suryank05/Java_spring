package com.ExamPort.ExamPort.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                org.springframework.web.cors.CorsConfiguration corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(java.util.Arrays.asList("http://localhost:5173", "http://localhost:3000"));
                corsConfig.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                corsConfig.setAllowedHeaders(java.util.Arrays.asList("*"));
                corsConfig.setAllowCredentials(true);
                corsConfig.setMaxAge(3600L);
                return corsConfig;
            }))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeRequests(auth -> auth
                // Public endpoints - no authentication required
                .antMatchers("/api/auth/**", "/welcome", "/health", "/api/courses/public", "/api/test/**", "/api/public/**").permitAll()
                .antMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                
                // Review endpoints - public access for landing page
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/public", "/api/reviews/recent", "/api/reviews/statistics", "/api/reviews/test").permitAll()
                
                // Course endpoints
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/courses/public").permitAll()
                .antMatchers(org.springframework.http.HttpMethod.POST, "/api/courses/create").hasAnyRole("ADMIN", "INSTRUCTOR")
                .antMatchers("/api/courses/**").authenticated()
                
                // Exam endpoints
                .antMatchers(org.springframework.http.HttpMethod.GET, "/exam", "/exam/**", "/api/exams", "/api/exams/**").hasAnyRole("ADMIN", "STUDENT", "INSTRUCTOR")
                .antMatchers(org.springframework.http.HttpMethod.POST, "/exam", "/exam/**", "/api/exams", "/api/exams/**").hasAnyRole("ADMIN", "STUDENT", "INSTRUCTOR")
                
                // User endpoints
                .antMatchers("/api/users/me").authenticated()
                
                // Enrollment endpoints
                .antMatchers("/api/enrollments/**").authenticated()
                
                // Payment endpoints
                .antMatchers("/api/payments/**").authenticated()
                
                // Allow contact form submissions without authentication
                .antMatchers(org.springframework.http.HttpMethod.POST, "/api/contact").permitAll()
                
                // Review endpoints - authenticated access for user actions
                .antMatchers("/api/reviews/submit", "/api/reviews/update", "/api/reviews/my-review", "/api/reviews/delete").authenticated()
                .antMatchers("/api/reviews/pending", "/api/reviews/*/approve", "/api/reviews/*/reject").hasRole("ADMIN")
                
                // Admin endpoints
                .antMatchers("/api/admin/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
