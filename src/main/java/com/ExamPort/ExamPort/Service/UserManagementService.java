package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private ResultRepository resultRepository;
    
    /**
     * Delete a user and all related data safely
     */
    @Transactional
    public boolean deleteUserSafely(Long userId) {
        try {
            logger.info("Starting safe deletion of user with ID: {}", userId);
            
            // Check if user exists
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User with ID {} not found", userId);
                return false;
            }
            
            User user = userOpt.get();
            logger.info("Deleting user: {} ({})", user.getUsername(), user.getEmail());
            
            // Step 1: Delete verification tokens
            int deletedTokens = verificationTokenRepository.deleteByUser(user);
            logger.info("Deleted {} verification tokens for user {}", deletedTokens, userId);
            
            // Step 2: Delete enrollments where user is a student
            int deletedEnrollments = enrollmentRepository.deleteByStudent(user);
            logger.info("Deleted {} enrollments for user {}", deletedEnrollments, userId);
            
            // Step 3: Delete results for this user
            int deletedResults = resultRepository.deleteByUserId(userId);
            logger.info("Deleted {} results for user {}", deletedResults, userId);
            
            // Step 4: Handle courses where user is an instructor
            // Option A: Delete courses (if no enrollments)
            // Option B: Set instructor to null (if you want to keep courses)
            // For now, let's set instructor to null to preserve courses
            int updatedCourses = courseRepository.updateInstructorToNull(user);
            logger.info("Updated {} courses to remove instructor reference for user {}", updatedCourses, userId);
            
            // Step 5: Finally delete the user
            userRepository.delete(user);
            logger.info("Successfully deleted user: {} (ID: {})", user.getUsername(), userId);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error during safe deletion of user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete user safely: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if user can be deleted (has no critical dependencies)
     */
    public boolean canDeleteUser(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }
            
            User user = userOpt.get();
            
            // Check if user has courses with enrollments
            long coursesWithEnrollments = courseRepository.countCoursesWithEnrollmentsByInstructor(user);
            if (coursesWithEnrollments > 0) {
                logger.warn("User {} has {} courses with active enrollments", userId, coursesWithEnrollments);
                // Still allow deletion but warn about it
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking if user {} can be deleted: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get user deletion impact summary
     */
    public UserDeletionImpact getUserDeletionImpact(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return null;
            }
            
            User user = userOpt.get();
            
            UserDeletionImpact impact = new UserDeletionImpact();
            impact.setUserId(userId);
            impact.setUsername(user.getUsername());
            impact.setEmail(user.getEmail());
            
            // Count related data
            impact.setVerificationTokens(verificationTokenRepository.countByUser(user));
            impact.setEnrollments(enrollmentRepository.countByStudent(user));
            impact.setResults(resultRepository.countByUserId(userId));
            impact.setCourses(courseRepository.countByInstructor(user));
            
            return impact;
            
        } catch (Exception e) {
            logger.error("Error getting deletion impact for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Inner class to represent deletion impact
     */
    public static class UserDeletionImpact {
        private Long userId;
        private String username;
        private String email;
        private long verificationTokens;
        private long enrollments;
        private long results;
        private long courses;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public long getVerificationTokens() { return verificationTokens; }
        public void setVerificationTokens(long verificationTokens) { this.verificationTokens = verificationTokens; }
        
        public long getEnrollments() { return enrollments; }
        public void setEnrollments(long enrollments) { this.enrollments = enrollments; }
        
        public long getResults() { return results; }
        public void setResults(long results) { this.results = results; }
        
        public long getCourses() { return courses; }
        public void setCourses(long courses) { this.courses = courses; }
        
        @Override
        public String toString() {
            return String.format("UserDeletionImpact{userId=%d, username='%s', tokens=%d, enrollments=%d, results=%d, courses=%d}",
                    userId, username, verificationTokens, enrollments, results, courses);
        }
    }
}