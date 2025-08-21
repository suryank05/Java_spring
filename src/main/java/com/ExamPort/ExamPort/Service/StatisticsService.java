package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Repository.*;
import com.ExamPort.ExamPort.Entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StatisticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ResultRepository resultRepository;
    
    @Autowired
    private Exam_repo examRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    /**
     * Get public statistics for landing page
     */
    public Map<String, Object> getPublicStatistics() {
        logger.info("Calculating public statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total registered users
            long totalUsers = userRepository.count();
            logger.debug("Total users count: {}", totalUsers);
            
            // Total exam attempts/results
            long totalExamAttempts = resultRepository.count();
            logger.debug("Total exam attempts count: {}", totalExamAttempts);
            
            // Total courses available
            long totalCourses = courseRepository.count();
            logger.debug("Total courses count: {}", totalCourses);
            
            // Total enrollments
            long totalEnrollments = enrollmentRepository.count();
            logger.debug("Total enrollments count: {}", totalEnrollments);
            
            // Total exams created
            long totalExams = examRepository.count();
            logger.debug("Total exams count: {}", totalExams);
            
            // Calculate some derived statistics
            double averageAttemptsPerUser = totalUsers > 0 ? (double) totalExamAttempts / totalUsers : 0;
            double averageEnrollmentsPerUser = totalUsers > 0 ? (double) totalEnrollments / totalUsers : 0;
            
            // Build statistics map
            stats.put("activeUsers", totalUsers);
            stats.put("examsCompleted", totalExamAttempts);
            stats.put("totalCourses", totalCourses);
            stats.put("totalEnrollments", totalEnrollments);
            stats.put("totalExams", totalExams);
            stats.put("averageAttemptsPerUser", Math.round(averageAttemptsPerUser * 100.0) / 100.0);
            stats.put("averageEnrollmentsPerUser", Math.round(averageEnrollmentsPerUser * 100.0) / 100.0);
            stats.put("success", true);
            
            logger.info("Public statistics calculated: {} users, {} exam attempts, {} courses, {} enrollments, {} exams", 
                       totalUsers, totalExamAttempts, totalCourses, totalEnrollments, totalExams);
            
        } catch (Exception e) {
            logger.error("Error calculating public statistics: {}", e.getMessage(), e);
            
            // Return default values on error
            stats.put("activeUsers", 0);
            stats.put("examsCompleted", 0);
            stats.put("totalCourses", 0);
            stats.put("totalEnrollments", 0);
            stats.put("totalExams", 0);
            stats.put("averageAttemptsPerUser", 0.0);
            stats.put("averageEnrollmentsPerUser", 0.0);
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Get detailed statistics for admin dashboard
     */
    public Map<String, Object> getDetailedStatistics() {
        logger.info("Calculating detailed statistics for admin dashboard");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get basic statistics
            Map<String, Object> publicStats = getPublicStatistics();
            stats.putAll(publicStats);
            
            // Add more detailed statistics
            // User statistics by role
            try {
                var usersByRole = userRepository.countUsersByRole();
                Map<String, Long> roleStats = new HashMap<>();
                
                for (Object[] row : usersByRole) {
                    String role = (String) row[0];
                    Long count = (Long) row[1];
                    roleStats.put(role.toLowerCase(), count);
                }
                
                stats.put("usersByRole", roleStats);
                
            } catch (Exception e) {
                logger.warn("Could not fetch user role statistics: {}", e.getMessage());
                stats.put("usersByRole", new HashMap<>());
            }
            
            // Course statistics
            try {
                long publicCourses = courseRepository.countPublicCourses();
                stats.put("publicCourses", publicCourses);
                stats.put("privateCourses", (Long) stats.get("totalCourses") - publicCourses);
            } catch (Exception e) {
                logger.warn("Could not fetch course visibility statistics: {}", e.getMessage());
                stats.put("publicCourses", 0);
                stats.put("privateCourses", 0);
            }
            
            logger.info("Detailed statistics calculated successfully");
            
        } catch (Exception e) {
            logger.error("Error calculating detailed statistics: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    // Debug methods to test individual repository counts
    public long getDirectCourseCount() {
        try {
            long count = courseRepository.count();
            logger.info("Direct course count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting direct course count: {}", e.getMessage(), e);
            return -1;
        }
    }
    
    public long getDirectUserCount() {
        try {
            long count = userRepository.count();
            logger.info("Direct user count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting direct user count: {}", e.getMessage(), e);
            return -1;
        }
    }
    
    public long getDirectExamCount() {
        try {
            long count = examRepository.count();
            logger.info("Direct exam count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting direct exam count: {}", e.getMessage(), e);
            return -1;
        }
    }
    
    public long getDirectResultCount() {
        try {
            long count = resultRepository.count();
            logger.info("Direct result count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting direct result count: {}", e.getMessage(), e);
            return -1;
        }
    }
    
    public long getDirectEnrollmentCount() {
        try {
            long count = enrollmentRepository.count();
            logger.info("Direct enrollment count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting direct enrollment count: {}", e.getMessage(), e);
            return -1;
        }
    }
    
    /**
     * Get database connection status and basic info
     */
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test each repository
            status.put("userRepositoryWorking", userRepository != null);
            status.put("courseRepositoryWorking", courseRepository != null);
            status.put("examRepositoryWorking", examRepository != null);
            status.put("resultRepositoryWorking", resultRepository != null);
            status.put("enrollmentRepositoryWorking", enrollmentRepository != null);
            
            // Try to execute simple queries
            try {
                long userCount = userRepository.count();
                status.put("userCountQuery", "SUCCESS");
                status.put("userCount", userCount);
            } catch (Exception e) {
                status.put("userCountQuery", "FAILED: " + e.getMessage());
                status.put("userCount", -1);
            }
            
            try {
                long courseCount = courseRepository.count();
                status.put("courseCountQuery", "SUCCESS");
                status.put("courseCount", courseCount);
            } catch (Exception e) {
                status.put("courseCountQuery", "FAILED: " + e.getMessage());
                status.put("courseCount", -1);
            }
            
            try {
                long examCount = examRepository.count();
                status.put("examCountQuery", "SUCCESS");
                status.put("examCount", examCount);
            } catch (Exception e) {
                status.put("examCountQuery", "FAILED: " + e.getMessage());
                status.put("examCount", -1);
            }
            
            try {
                long resultCount = resultRepository.count();
                status.put("resultCountQuery", "SUCCESS");
                status.put("resultCount", resultCount);
            } catch (Exception e) {
                status.put("resultCountQuery", "FAILED: " + e.getMessage());
                status.put("resultCount", -1);
            }
            
            try {
                long enrollmentCount = enrollmentRepository.count();
                status.put("enrollmentCountQuery", "SUCCESS");
                status.put("enrollmentCount", enrollmentCount);
            } catch (Exception e) {
                status.put("enrollmentCountQuery", "FAILED: " + e.getMessage());
                status.put("enrollmentCount", -1);
            }
            
            status.put("overallStatus", "CONNECTED");
            
        } catch (Exception e) {
            logger.error("Database status check failed: {}", e.getMessage(), e);
            status.put("overallStatus", "FAILED");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * Check if we have any sample data and create some if needed (for testing)
     */
    public Map<String, Object> checkAndCreateSampleData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long courseCount = courseRepository.count();
            long examCount = examRepository.count();
            
            result.put("initialCourseCount", courseCount);
            result.put("initialExamCount", examCount);
            
            if (courseCount == 0) {
                logger.info("No courses found, checking if we can create a sample course");
                // We won't actually create data here, just report the status
                result.put("courseTableExists", "YES - but empty");
            } else {
                result.put("courseTableExists", "YES - with data");
            }
            
            if (examCount == 0) {
                logger.info("No exams found, checking if we can create a sample exam");
                result.put("examTableExists", "YES - but empty");
            } else {
                result.put("examTableExists", "YES - with data");
            }
            
            // Try to get a list of all courses to see if the query works
            try {
                var allCourses = courseRepository.findAll();
                result.put("courseQueryWorks", true);
                result.put("courseListSize", allCourses.size());
                
                if (!allCourses.isEmpty()) {
                    result.put("firstCourseName", allCourses.get(0).getName());
                }
            } catch (Exception e) {
                result.put("courseQueryWorks", false);
                result.put("courseQueryError", e.getMessage());
            }
            
            // Try to get a list of all exams to see if the query works
            try {
                var allExams = examRepository.findAll();
                result.put("examQueryWorks", true);
                result.put("examListSize", allExams.size());
                
                if (!allExams.isEmpty()) {
                    result.put("firstExamTitle", allExams.get(0).getTitle());
                }
            } catch (Exception e) {
                result.put("examQueryWorks", false);
                result.put("examQueryError", e.getMessage());
            }
            
            result.put("status", "SUCCESS");
            
        } catch (Exception e) {
            logger.error("Error checking sample data: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Create sample data if tables are empty (for testing/demo purposes)
     */
    public Map<String, Object> createSampleDataIfEmpty() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long courseCount = courseRepository.count();
            long examCount = examRepository.count();
            
            result.put("initialCourseCount", courseCount);
            result.put("initialExamCount", examCount);
            
            // Only create sample data if tables are empty
            if (courseCount == 0) {
                logger.info("Creating sample courses...");
                
                // Find an instructor user to assign courses to
                var instructors = userRepository.findAll().stream()
                    .filter(user -> "instructor".equalsIgnoreCase(user.getRole()))
                    .findFirst();
                
                if (instructors.isPresent()) {
                    User instructor = instructors.get();
                    
                    // Create sample courses
                    Course course1 = new Course();
                    course1.setName("Introduction to Programming");
                    course1.setDescription("Learn the basics of programming with hands-on examples");
                    course1.setInstructor(instructor);
                    course1.setVisibility(CourseVisibility.PUBLIC);
                    course1.setPricing(CoursePricing.FREE);
                    courseRepository.save(course1);
                    
                    Course course2 = new Course();
                    course2.setName("Advanced Java Development");
                    course2.setDescription("Master advanced Java concepts and frameworks");
                    course2.setInstructor(instructor);
                    course2.setVisibility(CourseVisibility.PUBLIC);
                    course2.setPricing(CoursePricing.FREE);
                    courseRepository.save(course2);
                    
                    Course course3 = new Course();
                    course3.setName("Web Development Fundamentals");
                    course3.setDescription("Build modern web applications from scratch");
                    course3.setInstructor(instructor);
                    course3.setVisibility(CourseVisibility.PUBLIC);
                    course3.setPricing(CoursePricing.FREE);
                    courseRepository.save(course3);
                    
                    result.put("coursesCreated", 3);
                    result.put("instructorUsed", instructor.getUsername());
                    
                    // Create sample exams for the courses
                    Exam exam1 = new Exam();
                    exam1.setTitle("Programming Basics Quiz");
                    exam1.setDescription("Test your understanding of programming fundamentals");
                    exam1.setCourse(course1);
                    exam1.setDuration(30);
                    exam1.setTotalMarks(100);
                    exam1.setIsactive(true);
                    examRepository.save(exam1);
                    
                    Exam exam2 = new Exam();
                    exam2.setTitle("Java Advanced Concepts");
                    exam2.setDescription("Advanced Java programming assessment");
                    exam2.setCourse(course2);
                    exam2.setDuration(45);
                    exam2.setTotalMarks(150);
                    exam2.setIsactive(true);
                    examRepository.save(exam2);
                    
                    result.put("examsCreated", 2);
                    
                } else {
                    // Create a sample instructor if none exists
                    User sampleInstructor = new User();
                    sampleInstructor.setUsername("sample_instructor");
                    sampleInstructor.setEmail("instructor@example.com");
                    sampleInstructor.setPassword("$2a$10$dummy.password.hash"); // Dummy hash
                    sampleInstructor.setRole("instructor");
                    sampleInstructor.setFullName("Sample Instructor");
                    sampleInstructor.setEmailVerified(true);
                    userRepository.save(sampleInstructor);
                    
                    result.put("instructorCreated", true);
                    result.put("coursesCreated", 0);
                    result.put("message", "Created sample instructor, please run again to create courses");
                }
            } else {
                result.put("coursesCreated", 0);
                result.put("message", "Courses already exist, no sample data created");
            }
            
            // Refresh counts
            long newCourseCount = courseRepository.count();
            long newExamCount = examRepository.count();
            
            result.put("finalCourseCount", newCourseCount);
            result.put("finalExamCount", newExamCount);
            result.put("status", "SUCCESS");
            
        } catch (Exception e) {
            logger.error("Error creating sample data: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}