package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Service.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class UserManagementController {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        try {
            System.out.println("UserManagementController: getAllUsers called");
            
            List<User> users = userRepository.findAll();
            System.out.println("Found " + users.size() + " users");
            
            List<Map<String, Object>> userList = users.stream()
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userList);
        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of()); // Return empty list on error
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            System.out.println("UserManagementController: getUserStats called");
            
            long totalUsers = userRepository.count();
            
            // Count users by role
            List<Object[]> usersByRoleRaw = userRepository.countUsersByRole();
            Map<String, Long> roleStats = new HashMap<>();
            roleStats.put("admin", 0L);
            roleStats.put("instructor", 0L);
            roleStats.put("student", 0L);
            
            for (Object[] row : usersByRoleRaw) {
                String role = ((String) row[0]).toLowerCase();
                Long count = (Long) row[1];
                roleStats.put(role, count);
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("adminCount", roleStats.get("admin"));
            stats.put("instructorCount", roleStats.get("instructor"));
            stats.put("studentCount", roleStats.get("student"));
            
            System.out.println("User stats: " + stats);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error fetching user stats: " + e.getMessage());
            e.printStackTrace();
            
            // Return default stats on error
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalUsers", 0L);
            defaultStats.put("adminCount", 0L);
            defaultStats.put("instructorCount", 0L);
            defaultStats.put("studentCount", 0L);
            
            return ResponseEntity.ok(defaultStats);
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        try {
            logger.info("UserManagementController: deleteUser called for ID: {}", userId);
            
            if (!userRepository.existsById(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Get deletion impact before deletion
            UserManagementService.UserDeletionImpact impact = userManagementService.getUserDeletionImpact(userId);
            if (impact != null) {
                logger.info("Deletion impact for user {}: {}", userId, impact);
            }
            
            // Perform safe deletion
            boolean deleted = userManagementService.deleteUserSafely(userId);
            
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User and all related data deleted successfully");
                if (impact != null) {
                    response.put("deletionImpact", Map.of(
                        "verificationTokens", impact.getVerificationTokens(),
                        "enrollments", impact.getEnrollments(),
                        "results", impact.getResults(),
                        "courses", impact.getCourses()
                    ));
                }
                
                logger.info("User {} deleted successfully", userId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Failed to delete user");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", userId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete user: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/{userId}/deletion-impact")
    public ResponseEntity<Map<String, Object>> getUserDeletionImpact(@PathVariable Long userId) {
        try {
            logger.info("Getting deletion impact for user: {}", userId);
            
            UserManagementService.UserDeletionImpact impact = userManagementService.getUserDeletionImpact(userId);
            
            if (impact == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("impact", Map.of(
                "userId", impact.getUserId(),
                "username", impact.getUsername(),
                "email", impact.getEmail(),
                "verificationTokens", impact.getVerificationTokens(),
                "enrollments", impact.getEnrollments(),
                "results", impact.getResults(),
                "courses", impact.getCourses()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting deletion impact for user {}: {}", userId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get deletion impact: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().toLowerCase());
        userMap.put("fullName", user.getFullName());
        userMap.put("phoneNumber", user.getPhoneNumber());
        userMap.put("status", user.isEmailVerified() ? "active" : "inactive");
        userMap.put("createdAt", "2024-01-01"); // You can add a createdAt field to User entity if needed
        
        return userMap;
    }
}