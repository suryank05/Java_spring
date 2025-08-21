package com.ExamPort.ExamPort.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class LoggingConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor());
    }

    public static class RequestLoggingInterceptor implements HandlerInterceptor {
        private static final Logger logger = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            long startTime = System.currentTimeMillis();
            request.setAttribute("startTime", startTime);
            
            // Log request details (only for non-auth endpoints to reduce noise)
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String remoteAddr = getClientIpAddress(request);
            
            if (queryString != null) {
                uri += "?" + queryString;
            }
            
            // Only log non-auth requests to reduce verbosity
            if (!uri.startsWith("/api/auth/")) {
                logger.info("WEB {} {} from {} - Started", method, uri, remoteAddr);
            }
            return true;
        }
        
        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            String remoteAddr = request.getRemoteAddr();
            return "127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) ? "localhost" : remoteAddr;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                  Object handler, Exception ex) {
            long startTime = (Long) request.getAttribute("startTime");
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();
            
            // Only log completion for non-auth endpoints and important requests
            if (!uri.startsWith("/api/auth/") || status >= 400) {
                if (ex != null) {
                    logger.error("ERROR {} {} - Completed with error in {}ms - Status: {} - Error: {}", 
                               method, uri, duration, status, ex.getMessage());
                } else {
                    String statusText = getStatusText(status);
                    logger.info("{} {} {} - Completed in {}ms - Status: {}", 
                              statusText, method, uri, duration, status);
                }
            }
            
            // Always log auth requests but with simpler format
            if (uri.startsWith("/api/auth/")) {
                String statusText = getStatusText(status);
                logger.info("AUTH {} {} - {}ms - {}", method, uri, duration, status);
            }
        }
        
        private String getStatusText(int status) {
            if (status >= 200 && status < 300) return "SUCCESS";
            if (status >= 300 && status < 400) return "REDIRECT";
            if (status >= 400 && status < 500) return "CLIENT_ERROR";
            if (status >= 500) return "SERVER_ERROR";
            return "INFO";
        }
    }
}