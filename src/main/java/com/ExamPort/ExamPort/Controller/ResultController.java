package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.Result;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Entity.Exam;
import com.ExamPort.ExamPort.Repository.ResultRepository;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Repository.Exam_repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/results")
public class ResultController {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultController.class);
    
    @Autowired
    private ResultRepository resultRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private Exam_repo examRepository;
    
    // Get results for the authenticated user
    @GetMapping("/me")
    public ResponseEntity<?> getMyResults(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to /me endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching results for user: {}", username);
        
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.warn("User not found: {}", username);
                return ResponseEntity.status(404).body("User not found");
            }
            
            List<Result> results = resultRepository.findByUserIdOrderByAttemptDateDesc(user.getId());
            logger.info("Found {} results for user: {}", results.size(), username);
            
            List<Map<String, Object>> enhancedResults = results.stream()
                .map(this::enhanceResultData)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(enhancedResults);
            
        } catch (Exception e) {
            logger.error("Error fetching results for user: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching results");
        }
    }
    
    // Alias endpoint for my-results (for compatibility)
    @GetMapping("/my-results")
    public ResponseEntity<?> getMyResultsAlias(Principal principal) {
        return getMyResults(principal);
    }
    
    // Get results for a specific exam (instructor only)
    @GetMapping("/exam/{examId}")
    public ResponseEntity<?> getExamResults(@PathVariable Long examId, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to exam results endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching results for exam: {} by user: {}", examId, username);
        
        try {
            // TODO: Add instructor authorization check
            List<Result> results = resultRepository.findByExamExamIdOrderByScoreDesc(examId);
            logger.info("Found {} results for exam: {}", results.size(), examId);
            
            List<Map<String, Object>> enhancedResults = results.stream()
                .map(this::enhanceResultData)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(enhancedResults);
            
        } catch (Exception e) {
            logger.error("Error fetching results for exam: {}", examId, e);
            return ResponseEntity.internalServerError().body("Error fetching exam results");
        }
    }
    
    // Get specific result by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getResultById(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to result endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching result: {} for user: {}", id, username);
        
        try {
            Result result = resultRepository.findById(id).orElse(null);
            if (result == null) {
                logger.warn("Result not found: {}", id);
                return ResponseEntity.status(404).body("Result not found");
            }
            
            // Check if user owns this result or is instructor
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null || (!result.getUserId().equals(user.getId()) && !"ROLE_INSTRUCTOR".equals(user.getRole()))) {
                logger.warn("Unauthorized access to result: {} by user: {}", id, username);
                return ResponseEntity.status(403).body("Unauthorized");
            }
            
            Map<String, Object> enhancedResult = enhanceResultData(result);
            return ResponseEntity.ok(enhancedResult);
            
        } catch (Exception e) {
            logger.error("Error fetching result: {}", id, e);
            return ResponseEntity.internalServerError().body("Error fetching result");
        }
    }
    
    private Map<String, Object> enhanceResultData(Result result) {
        Map<String, Object> resultData = new HashMap<>();
        
        // Basic result info
        resultData.put("id", result.getId());
        resultData.put("score", result.getScore());
        resultData.put("passed", result.getPassed());
        resultData.put("timeTaken", result.getTimeTaken());
        resultData.put("attemptDate", result.getAttemptDate());
        resultData.put("feedback", result.getFeedback());
        resultData.put("userRank", result.getUserRank());
        resultData.put("answers", result.getAnswers());
        
        // Get exam information
        try {
            Exam exam = examRepository.findById(result.getExamExamId()).orElse(null);
            if (exam != null) {
                Map<String, Object> examInfo = new HashMap<>();
                examInfo.put("id", exam.getExam_id());
                examInfo.put("title", exam.getTitle());
                examInfo.put("description", exam.getDescription());
                examInfo.put("totalMarks", exam.getTotalMarks());
                examInfo.put("duration", exam.getDuration());
                
                // Add course information if available
                if (exam.getCourse() != null) {
                    Map<String, Object> courseInfo = new HashMap<>();
                    courseInfo.put("id", exam.getCourse().getId());
                    courseInfo.put("name", exam.getCourse().getName());
                    examInfo.put("course", courseInfo);
                }
                
                resultData.put("exam", examInfo);
                
                // Calculate percentage
                if (exam.getTotalMarks() > 0) {
                    double percentage = (result.getScore() / exam.getTotalMarks()) * 100;
                    resultData.put("percentage", Math.round(percentage * 100.0) / 100.0);
                    
                    // Calculate grade
                    String grade = calculateGrade(percentage);
                    resultData.put("grade", grade);
                }
            }
        } catch (Exception e) {
            logger.warn("Error fetching exam info for result: {}", result.getId(), e);
        }
        
        // Get user information
        try {
            User user = userRepository.findById(result.getUserId()).orElse(null);
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("fullName", user.getFullName());
                userInfo.put("email", user.getEmail());
                resultData.put("user", userInfo);
            }
        } catch (Exception e) {
            logger.warn("Error fetching user info for result: {}", result.getId(), e);
        }
        
        // Format time taken
        if (result.getTimeTaken() != null) {
            int minutes = result.getTimeTaken() / 60;
            int seconds = result.getTimeTaken() % 60;
            resultData.put("timeTakenFormatted", String.format("%d:%02d", minutes, seconds));
        }
        
        // Format attempt date
        if (result.getAttemptDate() != null) {
            resultData.put("date", result.getAttemptDate().toString());
        }
        
        return resultData;
    }
    
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        else if (percentage >= 80) return "A";
        else if (percentage >= 70) return "B+";
        else if (percentage >= 60) return "B";
        else if (percentage >= 50) return "C";
        else return "F";
    }
}