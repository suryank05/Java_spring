package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.Result;
import com.ExamPort.ExamPort.Entity.Exam;
import com.ExamPort.ExamPort.Entity.User;

import com.ExamPort.ExamPort.Repository.ResultRepository;
import com.ExamPort.ExamPort.Repository.Exam_repo;
import com.ExamPort.ExamPort.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Exam_repo examRepository;
    @Autowired
    private ResultRepository resultRepository;
    

    @GetMapping
    public Map<String, Object> getDashboardStats() {
        logger.info("Dashboard stats requested");
        
        try {
            // 1. Total Users
            long totalUsers = userRepository.count();
            logger.debug("Total users: {}", totalUsers);

            // 2. Total Exams
            long totalExams = examRepository.count();
            logger.debug("Total exams: {}", totalExams);

            // 3. Total Attempts
            long totalAttempts = resultRepository.count();
            logger.debug("Total attempts: {}", totalAttempts);

            // 4. Average Score
            List<Result> results = resultRepository.findAll();
            double averageScore = results.isEmpty() ? 0.0 : results.stream().mapToDouble(Result::getScore).average().orElse(0.0);
            logger.debug("Average score calculated: {}", averageScore);

            
            // 5. Users By Role
            List<Object[]> usersByRoleRaw = userRepository.countUsersByRole();
            List<Map<String, Object>> usersByRole = new ArrayList<>();
            Map<String, String> roleColors = Map.of(
                "student", "#8B5CF6",
                "instructor", "#06B6D4",
                "admin", "#10B981"
            );
            for (Object[] row : usersByRoleRaw) {
                String role = (String) row[0];
                long count = (long) row[1];
                usersByRole.add(Map.of(
                    "name", capitalize(role) + "s",
                    "value", count,
                    "color", roleColors.getOrDefault(role, "#999999")
                ));
            }
            logger.debug("Users by role processed: {} categories", usersByRole.size());

            
            // Compose nested stats object
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalExams", totalExams);
            stats.put("completedExams", totalAttempts);
            stats.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
            stats.put("rank", 0);

            // Compose main response
            Map<String, Object> response = new HashMap<>();
            response.put("stats", stats);
            response.put("usersByRole", usersByRole);
            response.put("totalUsers", totalUsers);
            response.put("upcomingExams", new ArrayList<>());
            response.put("recentResults", new ArrayList<>());
            
            logger.info("Dashboard stats compiled successfully");
            return response;
        } catch (Exception e) {
            logger.error("Error generating dashboard stats", e);
            throw e;
        }
    }
    
    @GetMapping("/student")
    public Map<String, Object> getStudentDashboard(Authentication authentication) {
        String username = authentication.getName();
        logger.info("Fetching student dashboard data for user: {}", username);
        
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.warn("User not found: {}", username);
                return Map.of("error", "User not found");
            }
            
            return getEnhancedStudentDashboard(user);
        } catch (Exception e) {
            logger.error("Error fetching student dashboard data for user: {}", username, e);
            return Map.of("error", "Error fetching dashboard data");
        }
    }
    
    private Map<String, Object> getEnhancedStudentDashboard(User user) {
        Map<String, Object> dashboardData = new HashMap<>();
        
        try {
            // Get user's results
            List<Result> userResults = resultRepository.findByUserIdOrderByAttemptDateDesc(user.getId());
            
            // Get all exams that the user is allowed to take
            List<Exam> allExams = examRepository.findAll();
            List<Exam> allowedExams = allExams.stream()
                .filter(exam -> exam.getCourse() != null && 
                              exam.getCourse().getAllowedEmails() != null && 
                              exam.getCourse().getAllowedEmails().contains(user.getEmail()))
                .toList();
            
            // Get submitted exam IDs
            Set<Long> submittedExamIds = userResults.stream()
                .map(Result::getExamExamId)
                .collect(java.util.stream.Collectors.toSet());
            
            // Current time for filtering
            LocalDateTime now = LocalDateTime.now();
            
            // 1. Filter exams based on requirements
            List<Map<String, Object>> upcomingExams = new ArrayList<>();
            
            for (Exam exam : allowedExams) {
                // Skip if already submitted
                if (submittedExamIds.contains(exam.getExam_id())) {
                    continue;
                }
                
                Map<String, Object> examMap = mapExamForDashboard(exam);
                String examStatus = determineExamStatus(exam, now);
                examMap.put("examStatus", examStatus);
                
                // Add based on status
                if ("active".equals(examStatus)) {
                    // Active and not overdue - can be attempted
                    examMap.put("canAttempt", true);
                    examMap.put("statusMessage", "Available now");
                    upcomingExams.add(examMap);
                } else if ("scheduled".equals(examStatus)) {
                    // Scheduled for future - show when it will be available
                    examMap.put("canAttempt", false);
                    examMap.put("statusMessage", "Scheduled for " + formatDateTime(exam.getStartDate(), exam.getStartTime()));
                    upcomingExams.add(examMap);
                }
                // Skip overdue and completed exams
            }
            
            // Sort: active exams first, then scheduled
            upcomingExams.sort((a, b) -> {
                boolean aCanAttempt = (Boolean) a.get("canAttempt");
                boolean bCanAttempt = (Boolean) b.get("canAttempt");
                if (aCanAttempt && !bCanAttempt) return -1;
                if (!aCanAttempt && bCanAttempt) return 1;
                return 0;
            });
            
            // Limit to 10 exams
            if (upcomingExams.size() > 10) {
                upcomingExams = upcomingExams.subList(0, 10);
            }
            
            // 2. Recent Results (last 5 results)
            List<Map<String, Object>> recentResults = userResults.stream()
                .limit(5)
                .map(this::mapResultForDashboard)
                .collect(java.util.stream.Collectors.toList());
            
            // 3. Course Performance (group by course)
            Map<String, List<Result>> resultsByCourse = new HashMap<>();
            for (Result result : userResults) {
                try {
                    Exam exam = examRepository.findById(result.getExamExamId()).orElse(null);
                    if (exam != null && exam.getCourse() != null) {
                        String courseName = exam.getCourse().getName();
                        resultsByCourse.computeIfAbsent(courseName, k -> new ArrayList<>()).add(result);
                    }
                } catch (Exception e) {
                    logger.warn("Error processing result for course performance: {}", result.getId(), e);
                }
            }
            
            List<Map<String, Object>> performanceData = resultsByCourse.entrySet().stream()
                .map(entry -> {
                    String course = entry.getKey();
                    List<Result> courseResults = entry.getValue();
                    double avgScore = courseResults.stream()
                        .mapToDouble(Result::getScore)
                        .average()
                        .orElse(0.0);
                    Map<String, Object> courseMap = new HashMap<>();
                    courseMap.put("subject", course);
                    courseMap.put("score", Math.round(avgScore));
                    return courseMap;
                })
                .collect(java.util.stream.Collectors.toList());
            
            // 4. Stats
            int totalExams = allowedExams.size();
            int completedExams = userResults.size();
            double averageScore = userResults.isEmpty() ? 0.0 : 
                userResults.stream().mapToDouble(Result::getScore).average().orElse(0.0);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalExams", totalExams);
            stats.put("completedExams", completedExams);
            stats.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
            stats.put("rank", calculateUserRank(user.getId(), averageScore));
            
            dashboardData.put("upcomingExams", upcomingExams);
            dashboardData.put("recentResults", recentResults);
            dashboardData.put("performanceData", performanceData);
            dashboardData.put("stats", stats);
            
            logger.info("Student dashboard compiled for user: {} - {} upcoming exams, {} recent results", 
                       user.getUsername(), upcomingExams.size(), recentResults.size());
            
        } catch (Exception e) {
            logger.error("Error compiling student dashboard for user: {}", user.getUsername(), e);
        }
        
        return dashboardData;
    }
    
    private String determineExamStatus(Exam exam, LocalDateTime now) {
        try {
            // Check if exam has start date and time
            if (exam.getStartDate() != null && exam.getStartTime() != null) {
                String startDateTimeStr = exam.getStartDate() + " " + exam.getStartTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime startDateTime = LocalDateTime.parse(startDateTimeStr, formatter);
                
                // If current time is before start time, it's scheduled
                if (now.isBefore(startDateTime)) {
                    return "scheduled";
                }
            }
            
            // Check if exam has end date and time
            if (exam.getEndDate() != null && exam.getEndTime() != null) {
                String endDateTimeStr = exam.getEndDate() + " " + exam.getEndTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeStr, formatter);
                
                // If current time is after end time, it's overdue
                if (now.isAfter(endDateTime)) {
                    return "overdue";
                }
            }
            
            // If exam is active and within time bounds, it's active
            return exam.isIsactive() ? "active" : "inactive";
        } catch (Exception e) {
            logger.warn("Error determining exam status for: {}", exam.getTitle(), e);
            return exam.isIsactive() ? "active" : "inactive";
        }
    }
    
    private String formatDateTime(String date, String time) {
        try {
            if (date == null) return "Not set";
            
            // Parse date
            String[] dateParts = date.split("-");
            if (dateParts.length == 3) {
                String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int month = Integer.parseInt(dateParts[1]);
                String formattedDate = dateParts[2] + " " + months[month] + " " + dateParts[0];
                
                if (time != null) {
                    // Parse time
                    String[] timeParts = time.split(":");
                    if (timeParts.length == 2) {
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        String ampm = hour >= 12 ? "PM" : "AM";
                        hour = hour % 12;
                        if (hour == 0) hour = 12;
                        String formattedTime = String.format("%d:%02d %s", hour, minute, ampm);
                        return formattedDate + " at " + formattedTime;
                    }
                }
                
                return formattedDate;
            }
            return date;
        } catch (Exception e) {
            return date + (time != null ? " " + time : "");
        }
    }
    
    private Map<String, Object> mapExamForDashboard(Exam exam) {
        Map<String, Object> examMap = new HashMap<>();
        examMap.put("id", exam.getExam_id());
        examMap.put("exam_id", exam.getExam_id());
        examMap.put("title", exam.getTitle());
        examMap.put("date", exam.getStartDate());
        examMap.put("time", exam.getStartTime());
        examMap.put("duration", exam.getDuration());
        examMap.put("totalMarks", exam.getTotalMarks());
        examMap.put("questionsCount", exam.getQuestions() != null ? exam.getQuestions().size() : 0);
        examMap.put("instructor", exam.getCourse() != null && exam.getCourse().getInstructor() != null ? 
                   exam.getCourse().getInstructor().getFullName() : "Unknown");
        examMap.put("courseName", exam.getCourse() != null ? exam.getCourse().getName() : "Unknown Course");
        return examMap;
    }
    
    private Map<String, Object> mapResultForDashboard(Result result) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", result.getId());
        
        try {
            Exam exam = examRepository.findById(result.getExamExamId()).orElse(null);
            resultMap.put("title", exam != null ? exam.getTitle() : "Unknown Exam");
            resultMap.put("totalMarks", exam != null ? exam.getTotalMarks() : 100);
        } catch (Exception e) {
            resultMap.put("title", "Unknown Exam");
            resultMap.put("totalMarks", 100);
        }
        
        resultMap.put("score", result.getScore().intValue());
        resultMap.put("date", result.getAttemptDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        resultMap.put("grade", calculateGrade(result.getScore(), (Integer) resultMap.get("totalMarks")));
        
        return resultMap;
    }
    
    private String calculateGrade(Double score, Integer totalMarks) {
        if (score == null || totalMarks == null || totalMarks == 0) return "N/A";
        
        double percentage = (score / totalMarks) * 100;
        if (percentage >= 95) return "A+";
        if (percentage >= 90) return "A";
        if (percentage >= 85) return "B+";
        if (percentage >= 80) return "B";
        if (percentage >= 75) return "C+";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }
    
    private int calculateUserRank(Long userId, double averageScore) {
        try {
            // Get all users' average scores
            List<Object[]> userAverages = resultRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(Result::getUserId))
                .entrySet().stream()
                .map(entry -> new Object[]{
                    entry.getKey(),
                    entry.getValue().stream().mapToDouble(Result::getScore).average().orElse(0.0)
                })
                .sorted((a, b) -> Double.compare((Double) b[1], (Double) a[1]))
                .collect(java.util.stream.Collectors.toList());
            
            for (int i = 0; i < userAverages.size(); i++) {
                if (userAverages.get(i)[0].equals(userId)) {
                    return i + 1;
                }
            }
        } catch (Exception e) {
            logger.warn("Error calculating user rank", e);
        }
        return 0;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}