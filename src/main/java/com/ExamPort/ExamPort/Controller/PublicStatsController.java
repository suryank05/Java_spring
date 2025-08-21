package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PublicStatsController {
    
    private static final Logger logger = LoggerFactory.getLogger(PublicStatsController.class);
    
    @Autowired
    private StatisticsService statisticsService;
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        try {
            logger.info("API request received for public statistics");
            
            Map<String, Object> stats = statisticsService.getPublicStatistics();
            stats.put("timestamp", System.currentTimeMillis());
            
            logger.info("Public statistics returned successfully: {}", stats);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error in PublicStatsController: {}", e.getMessage(), e);
            
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("activeUsers", 0);
            errorResponse.put("examsCompleted", 0);
            errorResponse.put("totalCourses", 0);
            errorResponse.put("totalEnrollments", 0);
            errorResponse.put("totalExams", 0);
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch statistics: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "PublicStatsController");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/debug-counts")
    public ResponseEntity<Map<String, Object>> debugCounts() {
        try {
            logger.info("Debug counts endpoint called");
            
            Map<String, Object> counts = new HashMap<>();
            counts.put("directCourseCount", statisticsService.getDirectCourseCount());
            counts.put("directUserCount", statisticsService.getDirectUserCount());
            counts.put("directExamCount", statisticsService.getDirectExamCount());
            counts.put("directResultCount", statisticsService.getDirectResultCount());
            counts.put("directEnrollmentCount", statisticsService.getDirectEnrollmentCount());
            counts.put("timestamp", System.currentTimeMillis());
            
            logger.info("Debug counts: {}", counts);
            return ResponseEntity.ok(counts);
            
        } catch (Exception e) {
            logger.error("Error in debug counts: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch debug counts: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/database-status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        try {
            logger.info("Database status endpoint called");
            
            Map<String, Object> status = statisticsService.getDatabaseStatus();
            status.put("timestamp", System.currentTimeMillis());
            
            logger.info("Database status: {}", status);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error in database status check: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check database status: " + e.getMessage());
            errorResponse.put("overallStatus", "ERROR");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/check-sample-data")
    public ResponseEntity<Map<String, Object>> checkSampleData() {
        try {
            logger.info("Check sample data endpoint called");
            
            Map<String, Object> result = statisticsService.checkAndCreateSampleData();
            result.put("timestamp", System.currentTimeMillis());
            
            logger.info("Sample data check result: {}", result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in sample data check: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check sample data: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/create-sample-data")
    public ResponseEntity<Map<String, Object>> createSampleData() {
        try {
            logger.info("Create sample data endpoint called");
            
            Map<String, Object> result = statisticsService.createSampleDataIfEmpty();
            result.put("timestamp", System.currentTimeMillis());
            
            logger.info("Sample data creation result: {}", result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error creating sample data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create sample data: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}