package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Service.EnrollmentService;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Repository.Exam_repo;
import com.ExamPort.ExamPort.Repository.ResultRepository;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Entity.Exam;
import com.ExamPort.ExamPort.Entity.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private Exam_repo examRepository;
    
    @Autowired
    private ResultRepository resultRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to student dashboard endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching dashboard data for student: {}", username);
        
        try {
            // Get student user
            Optional<User> studentOpt = userRepository.findByUsername(username);
            if (studentOpt.isEmpty()) {
                logger.warn("Student not found: {}", username);
                return ResponseEntity.status(404).body("Student not found");
            }
            
            User student = studentOpt.get();
            
            // Get student enrollments count
            long totalEnrollments = enrollmentService.getEnrollmentsByUsername(username, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
            
            // Get upcoming exams for the student
            List<Map<String, Object>> upcomingExams = getUpcomingExamsForStudent(student);
            
            // Get recent results for the student
            List<Map<String, Object>> recentResults = getRecentResultsForStudent(student);
            
            // Calculate statistics
            Map<String, Object> stats = calculateStudentStats(student, upcomingExams, recentResults);
            stats.put("totalCourses", totalEnrollments);
            stats.put("completedCourses", 0); // Can be calculated based on enrollment status
            
            // Get performance data (mock for now)
            List<Map<String, Object>> performanceData = getPerformanceData(student);
            
            // Create dashboard response
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("upcomingExams", upcomingExams);
            dashboardData.put("recentResults", recentResults);
            dashboardData.put("stats", stats);
            dashboardData.put("performanceData", performanceData);
            
            logger.info("Dashboard data retrieved successfully for student: {}", username);
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            logger.error("Error fetching dashboard data for student: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching dashboard data");
        }
    }
    
    private List<Map<String, Object>> getUpcomingExamsForStudent(User student) {
        List<Map<String, Object>> upcomingExams = new ArrayList<>();
        
        try {
            // Get all exams that the student has access to
            List<Exam> allExams = examRepository.findAll();
            
            for (Exam exam : allExams) {
                // Check if student has access to this exam (through course enrollment)
                if (hasAccessToExam(student, exam)) {
                    // Check if student hasn't submitted this exam yet
                    boolean hasSubmitted = resultRepository.findByUserIdAndExamExamId(student.getId(), exam.getExam_id()).isPresent();
                    
                    if (!hasSubmitted) {
                        Map<String, Object> examData = new HashMap<>();
                        examData.put("id", exam.getExam_id());
                        examData.put("exam_id", exam.getExam_id());
                        examData.put("title", exam.getTitle() != null ? exam.getTitle() : "Untitled Exam");
                        examData.put("date", exam.getStartDate() != null ? exam.getStartDate() : "TBD");
                        examData.put("time", exam.getStartTime() != null ? exam.getStartTime() : "TBD");
                        examData.put("duration", exam.getDuration());
                        examData.put("instructor", exam.getCourse() != null && exam.getCourse().getInstructor() != null ? 
                                   exam.getCourse().getInstructor().getFullName() : "Unknown");
                        examData.put("totalMarks", exam.getTotalMarks());
                        examData.put("courseName", exam.getCourse() != null ? exam.getCourse().getName() : "Unknown Course");
                        examData.put("canAttempt", exam.isIsactive());
                        examData.put("statusMessage", exam.isIsactive() ? "Ready to attempt" : "Not yet available");
                        
                        upcomingExams.add(examData);
                    }
                }
            }
            
            logger.info("Found {} upcoming exams for student: {}", upcomingExams.size(), student.getUsername());
            
        } catch (Exception e) {
            logger.error("Error fetching upcoming exams for student: {}", student.getUsername(), e);
        }
        
        return upcomingExams;
    }
    
    private List<Map<String, Object>> getRecentResultsForStudent(User student) {
        List<Map<String, Object>> recentResults = new ArrayList<>();
        
        try {
            // Get recent results for the student
            List<Result> results = resultRepository.findByUserIdOrderByAttemptDateDesc(student.getId());
            
            // Limit to recent 5 results
            int limit = Math.min(5, results.size());
            for (int i = 0; i < limit; i++) {
                Result result = results.get(i);
                
                // Get exam details
                Optional<Exam> examOpt = examRepository.findById(result.getExamExamId());
                if (examOpt.isPresent()) {
                    Exam exam = examOpt.get();
                    
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("id", result.getId());
                    resultData.put("title", exam.getTitle() != null ? exam.getTitle() : "Untitled Exam");
                    resultData.put("score", result.getScore());
                    resultData.put("totalMarks", exam.getTotalMarks());
                    resultData.put("date", result.getAttemptDate() != null ? result.getAttemptDate().toString() : "Unknown");
                    resultData.put("grade", calculateGrade(result.getScore(), exam.getTotalMarks()));
                    
                    recentResults.add(resultData);
                }
            }
            
            logger.info("Found {} recent results for student: {}", recentResults.size(), student.getUsername());
            
        } catch (Exception e) {
            logger.error("Error fetching recent results for student: {}", student.getUsername(), e);
        }
        
        return recentResults;
    }
    
    private Map<String, Object> calculateStudentStats(User student, List<Map<String, Object>> upcomingExams, List<Map<String, Object>> recentResults) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get all results for the student
            List<Result> allResults = resultRepository.findByUserId(student.getId());
            
            int totalExams = allResults.size() + upcomingExams.size();
            int completedExams = allResults.size();
            
            // Calculate average score
            double averageScore = 0.0;
            if (!allResults.isEmpty()) {
                double totalScore = allResults.stream().mapToDouble(Result::getScore).sum();
                averageScore = totalScore / allResults.size();
            }
            
            // Mock rank calculation (can be improved with actual ranking logic)
            int rank = Math.max(1, (int) (Math.random() * 100));
            
            stats.put("totalExams", totalExams);
            stats.put("completedExams", completedExams);
            stats.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
            stats.put("rank", rank);
            
            logger.info("Calculated stats for student {}: {} total exams, {} completed, avg score: {}", 
                       student.getUsername(), totalExams, completedExams, averageScore);
            
        } catch (Exception e) {
            logger.error("Error calculating stats for student: {}", student.getUsername(), e);
            // Return default stats
            stats.put("totalExams", 0);
            stats.put("completedExams", 0);
            stats.put("averageScore", 0.0);
            stats.put("rank", 1);
        }
        
        return stats;
    }
    
    private List<Map<String, Object>> getPerformanceData(User student) {
        List<Map<String, Object>> performanceData = new ArrayList<>();
        
        // Mock performance data (can be improved with actual subject-wise performance)
        String[] subjects = {"Math", "Physics", "Chemistry", "Biology", "English"};
        for (String subject : subjects) {
            Map<String, Object> subjectData = new HashMap<>();
            subjectData.put("subject", subject);
            subjectData.put("score", 70 + (int) (Math.random() * 30)); // Random score between 70-100
            performanceData.add(subjectData);
        }
        
        return performanceData;
    }
    
    private boolean hasAccessToExam(User student, Exam exam) {
        if (exam.getCourse() == null) {
            return false;
        }
        
        // Check if student is enrolled in the course
        return enrollmentService.isUserEnrolledInCourse(student.getUsername(), exam.getCourse().getId());
    }
    
    private String calculateGrade(double score, int totalMarks) {
        if (totalMarks == 0) return "N/A";
        
        double percentage = (score / totalMarks) * 100;
        
        if (percentage >= 90) return "A+";
        else if (percentage >= 80) return "A";
        else if (percentage >= 70) return "B+";
        else if (percentage >= 60) return "B";
        else if (percentage >= 50) return "C";
        else return "F";
    }
}