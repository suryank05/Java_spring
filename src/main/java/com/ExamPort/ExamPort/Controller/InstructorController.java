package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Service.EnrollmentService;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/instructor")
public class InstructorController {
    
    private static final Logger logger = LoggerFactory.getLogger(InstructorController.class);
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to instructor dashboard endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching dashboard data for instructor: {}", username);
        
        try {
            // Get instructor user
            Optional<User> instructorOpt = userRepository.findByUsername(username);
            if (instructorOpt.isEmpty()) {
                logger.warn("Instructor not found: {}", username);
                return ResponseEntity.status(404).body("Instructor not found");
            }
            
            User instructor = instructorOpt.get();
            
            // Get instructor statistics from enrollment service
            Map<String, Object> enrollmentStats = enrollmentService.getInstructorStats(username);
            
            // Create comprehensive stats object
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCourses", enrollmentStats.get("totalCourses"));
            stats.put("totalEnrollments", enrollmentStats.get("totalEnrollments"));
            stats.put("totalRevenue", enrollmentStats.get("totalRevenue"));
            stats.put("totalExams", 0); // TODO: Add exam count when exam service is available
            stats.put("totalStudents", enrollmentStats.get("totalEnrollments")); // For now, same as enrollments
            stats.put("averageScore", 0.0); // TODO: Calculate from exam results
            stats.put("completionRate", 0.0); // TODO: Calculate from exam completions
            
            // Create dashboard response
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("stats", stats);
            dashboardData.put("myExams", new java.util.ArrayList<>()); // Empty for now, can be populated later
            
            logger.info("Dashboard data retrieved successfully for instructor: {} - Courses: {}, Enrollments: {}, Revenue: {}", 
                       username, stats.get("totalCourses"), stats.get("totalEnrollments"), stats.get("totalRevenue"));
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            logger.error("Error fetching dashboard data for instructor: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching dashboard data");
        }
    }
}