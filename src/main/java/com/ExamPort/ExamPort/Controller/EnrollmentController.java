package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Service.EnrollmentService;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Entity.Enrollment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my-enrollments")
    public ResponseEntity<?> getMyEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrollmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {
        
        if (principal == null) {
            logger.warn("Unauthorized access attempt to my-enrollments endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching enrollments for user: {} - page: {}, size: {}", username, page, size);
        
        try {
            // Create pageable with sorting
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Get enrollments for the user
            Page<Enrollment> enrollmentsPage = enrollmentService.getEnrollmentsByUsername(username, pageable);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("enrollments", enrollmentsPage.getContent());
            response.put("currentPage", enrollmentsPage.getNumber());
            response.put("totalItems", enrollmentsPage.getTotalElements());
            response.put("totalPages", enrollmentsPage.getTotalPages());
            response.put("hasNext", enrollmentsPage.hasNext());
            response.put("hasPrevious", enrollmentsPage.hasPrevious());
            
            logger.info("Retrieved {} enrollments for user: {}", enrollmentsPage.getContent().size(), username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching enrollments for user: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching enrollments");
        }
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<?> getEnrollmentDetails(@PathVariable Long enrollmentId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to enrollment details endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching enrollment details: {} for user: {}", enrollmentId, username);
        
        try {
            Enrollment enrollment = enrollmentService.getEnrollmentById(enrollmentId);
            
            if (enrollment == null) {
                logger.warn("Enrollment not found: {}", enrollmentId);
                return ResponseEntity.notFound().build();
            }
            
            // Check if the enrollment belongs to the requesting user
            if (!enrollment.getStudent().getUsername().equals(username)) {
                logger.warn("Access denied to enrollment: {} for user: {}", enrollmentId, username);
                return ResponseEntity.status(403).body("Access denied");
            }
            
            logger.info("Enrollment details retrieved successfully: {}", enrollmentId);
            return ResponseEntity.ok(enrollment);
            
        } catch (Exception e) {
            logger.error("Error fetching enrollment details: {} for user: {}", enrollmentId, username, e);
            return ResponseEntity.internalServerError().body("Error fetching enrollment details");
        }
    }

    @GetMapping("/check/{courseId}")
    public ResponseEntity<?> checkEnrollment(@PathVariable Long courseId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to check enrollment endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Checking enrollment for course: {} by user: {}", courseId, username);
        
        try {
            boolean isEnrolled = enrollmentService.isUserEnrolledInCourse(username, courseId);
            Enrollment enrollment = enrollmentService.getEnrollmentByUsernameAndCourseId(username, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("isEnrolled", isEnrolled);
            response.put("courseId", courseId);
            if (enrollment != null) {
                response.put("enrollmentId", enrollment.getId());
                response.put("enrollmentDate", enrollment.getEnrollmentDate());
                response.put("status", enrollment.getStatus());
            }
            
            logger.info("Enrollment check completed for course: {} by user: {} - enrolled: {}", 
                       courseId, username, isEnrolled);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking enrollment for course: {} by user: {}", courseId, username, e);
            return ResponseEntity.internalServerError().body("Error checking enrollment");
        }
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Long enrollmentId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to cancel enrollment endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Cancelling enrollment: {} by user: {}", enrollmentId, username);
        
        try {
            Enrollment enrollment = enrollmentService.getEnrollmentById(enrollmentId);
            
            if (enrollment == null) {
                logger.warn("Enrollment not found: {}", enrollmentId);
                return ResponseEntity.notFound().build();
            }
            
            // Check if the enrollment belongs to the requesting user
            if (!enrollment.getStudent().getUsername().equals(username)) {
                logger.warn("Access denied to cancel enrollment: {} for user: {}", enrollmentId, username);
                return ResponseEntity.status(403).body("Access denied");
            }
            
            // Cancel the enrollment
            enrollmentService.cancelEnrollment(enrollmentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Enrollment cancelled successfully");
            response.put("enrollmentId", enrollmentId);
            
            logger.info("Enrollment cancelled successfully: {} by user: {}", enrollmentId, username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error cancelling enrollment: {} by user: {}", enrollmentId, username, e);
            return ResponseEntity.internalServerError().body("Error cancelling enrollment");
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseEnrollments(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        
        if (principal == null) {
            logger.warn("Unauthorized access attempt to course enrollments endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching enrollments for course: {} by instructor: {}", courseId, username);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("enrollmentDate").descending());
            Page<Enrollment> enrollmentsPage = enrollmentService.getCourseEnrollments(courseId, username, pageable);
            
            // Convert enrollments to DTOs to avoid serialization issues
            List<Map<String, Object>> enrollmentDTOs = enrollmentsPage.getContent().stream()
                .map(enrollment -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", enrollment.getId());
                    dto.put("enrollmentDate", enrollment.getEnrollmentDate());
                    dto.put("status", enrollment.getStatus());
                    dto.put("paymentTransactionId", enrollment.getPaymentTransactionId());
                    
                    // Add student info safely
                    if (enrollment.getStudent() != null) {
                        Map<String, Object> studentInfo = new HashMap<>();
                        studentInfo.put("id", enrollment.getStudent().getId());
                        studentInfo.put("username", enrollment.getStudent().getUsername());
                        studentInfo.put("email", enrollment.getStudent().getEmail());
                        studentInfo.put("fullName", enrollment.getStudent().getFullName());
                        dto.put("student", studentInfo);
                    }
                    
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("enrollments", enrollmentDTOs);
            response.put("currentPage", enrollmentsPage.getNumber());
            response.put("totalItems", enrollmentsPage.getTotalElements());
            response.put("totalPages", enrollmentsPage.getTotalPages());
            response.put("hasNext", enrollmentsPage.hasNext());
            response.put("hasPrevious", enrollmentsPage.hasPrevious());
            
            logger.info("Retrieved {} enrollments for course: {}", enrollmentsPage.getContent().size(), courseId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching course enrollments for course: {} by instructor: {}", courseId, username, e);
            return ResponseEntity.internalServerError().body("Error fetching course enrollments");
        }
    }

    @GetMapping("/instructor/stats")
    public ResponseEntity<?> getInstructorStats(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to instructor stats endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching instructor stats for: {}", username);
        
        try {
            Map<String, Object> stats = enrollmentService.getInstructorStats(username);
            
            logger.info("Instructor stats retrieved successfully for: {}", username);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error fetching instructor stats for: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching instructor stats");
        }
    }
}