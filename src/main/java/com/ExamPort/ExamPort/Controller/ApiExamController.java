package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.Exam;
import com.ExamPort.ExamPort.Entity.Question;
import com.ExamPort.ExamPort.Entity.ExamOption;
import com.ExamPort.ExamPort.Service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/exams")
public class ApiExamController {

    private static final Logger logger = LoggerFactory.getLogger(ApiExamController.class);

    @Autowired
    private TaskService task;

    @Autowired
    private com.ExamPort.ExamPort.Repository.UserRepository userRepository;

    @Autowired
    private com.ExamPort.ExamPort.Repository.Exam_repo examRepo;

    @Autowired
    private com.ExamPort.ExamPort.Repository.QuestionRepository questionRepo;

    @Autowired
    private com.ExamPort.ExamPort.Repository.ResultRepository resultRepository;

    @Autowired
    private javax.persistence.EntityManager entityManager;

    @Autowired
    private com.ExamPort.ExamPort.Service.EnrollmentService enrollmentService;

    @Autowired
    private com.ExamPort.ExamPort.Service.EmailService emailService;

    @PostMapping
    public void addExam(@RequestBody Exam e) {
        logger.info("Adding new exam: {}", e.getTitle());
        task.AddExam(e);
        logger.info("Exam added successfully: {}", e.getTitle());
    }

    // Enhanced endpoint: Get exams for authenticated instructor with detailed info
    @GetMapping("/instructor")
    public List<Map<String, Object>> getExamsForInstructor(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        logger.info("Fetching exams for instructor: {}", username);
        
        try {
            com.ExamPort.ExamPort.Entity.User instructor = userRepository.findByUsername(username).orElse(null);
            if (instructor == null) {
                logger.warn("Instructor not found: {}", username);
                return List.of();
            }

            List<Exam> exams = examRepo.findByCourse_Instructor_Id(instructor.getId());
            logger.info("Found {} exams for instructor: {}", exams.size(), username);
            
            return exams.stream().map(this::enhanceExamData).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching exams for instructor: {}", username, e);
            return List.of();
        }
    }

    private Map<String, Object> enhanceExamData(Exam exam) {
        Map<String, Object> examData = new HashMap<>();
        
        // Basic exam info
        examData.put("exam_id", exam.getExam_id());
        examData.put("title", exam.getTitle() != null ? exam.getTitle() : "Untitled Exam");
        examData.put("description", exam.getDescription() != null ? exam.getDescription() : "No description");
        examData.put("duration", exam.getDuration());
        examData.put("startDate", exam.getStartDate());
        examData.put("startTime", exam.getStartTime());
        examData.put("endDate", exam.getEndDate());
        examData.put("endTime", exam.getEndTime());
        examData.put("instructions", exam.getInstructions());
        examData.put("isactive", exam.isIsactive());
        examData.put("createdAt", exam.getCreatedAt());

        // Enhanced computed fields
        // 1. Number of questions with formatted display
        int questionCount = exam.getQuestions() != null ? exam.getQuestions().size() : 0;
        examData.put("questionCount", questionCount);
        examData.put("questionsDisplay", questionCount + " question" + (questionCount != 1 ? "s" : ""));

        // 2. Total marks (calculate from questions if not set)
        int totalMarks = exam.getTotalMarks();
        if (totalMarks == 0 && exam.getQuestions() != null) {
            totalMarks = exam.getQuestions().stream()
                .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 1)
                .sum();
        }
        examData.put("totalMarks", totalMarks);
        examData.put("marksDisplay", totalMarks + " mark" + (totalMarks != 1 ? "s" : ""));

        // 3. Number of students (from course enrollment and allowed emails) with formatted display
        int studentCount = 0;
        if (exam.getCourse() != null) {
            if ("PUBLIC".equals(exam.getCourse().getVisibility())) {
                // For public courses, count enrolled students
                studentCount = (int) enrollmentService.getEnrollmentCountByCourse(exam.getCourse().getId());
            } else if ("PRIVATE".equals(exam.getCourse().getVisibility()) && exam.getCourse().getAllowedEmails() != null) {
                // For private courses, count allowed emails (backward compatibility)
                studentCount = exam.getCourse().getAllowedEmails().size();
            }
        }
        examData.put("studentCount", studentCount);
        examData.put("studentsDisplay", studentCount + " student" + (studentCount != 1 ? "s" : ""));

        // 4. Course information with instructor details
        if (exam.getCourse() != null) {
            Map<String, Object> courseInfo = new HashMap<>();
            courseInfo.put("id", exam.getCourse().getId());
            courseInfo.put("name", exam.getCourse().getName());
            
            // Add instructor information
            if (exam.getCourse().getInstructor() != null) {
                Map<String, Object> instructorInfo = new HashMap<>();
                instructorInfo.put("id", exam.getCourse().getInstructor().getId());
                instructorInfo.put("username", exam.getCourse().getInstructor().getUsername());
                instructorInfo.put("fullName", exam.getCourse().getInstructor().getFullName());
                instructorInfo.put("email", exam.getCourse().getInstructor().getEmail());
                courseInfo.put("instructor", instructorInfo);
                
                // Add instructor name directly to exam data for easy access
                examData.put("instructor", exam.getCourse().getInstructor().getFullName() != null ? 
                    exam.getCourse().getInstructor().getFullName() : exam.getCourse().getInstructor().getUsername());
                examData.put("instructorName", exam.getCourse().getInstructor().getFullName() != null ? 
                    exam.getCourse().getInstructor().getFullName() : exam.getCourse().getInstructor().getUsername());
            } else {
                examData.put("instructor", "Unknown");
                examData.put("instructorName", "Unknown");
            }
            
            examData.put("course", courseInfo);
            examData.put("courseName", exam.getCourse().getName());
        } else {
            examData.put("instructor", "Unknown");
            examData.put("instructorName", "Unknown");
            examData.put("courseName", "No Course");
        }

        // 5. Duration display
        examData.put("durationDisplay", exam.getDuration() + " minute" + (exam.getDuration() != 1 ? "s" : ""));

        // 6. Date and time formatting
        Map<String, Object> dateTimeInfo = formatDateTime(exam);
        examData.putAll(dateTimeInfo);

        // 7. Time remaining calculation
        Map<String, Object> timeInfo = calculateTimeRemaining(exam);
        examData.putAll(timeInfo);

        // 8. Exam status and actions
        String status = determineExamStatus(exam);
        examData.put("status", status);

        // 9. Action buttons based on status
        Map<String, Object> actions = determineActions(exam, status);
        examData.put("actions", actions);

        // 10. Include questions data for exam interface
        if (exam.getQuestions() != null) {
            examData.put("questions", exam.getQuestions());
        } else {
            examData.put("questions", List.of());
        }

        logger.debug("Enhanced exam data for: {} - {} questions, {} students, status: {}", 
                    exam.getTitle(), questionCount, studentCount, status);
        
        return examData;
    }

    private Map<String, Object> formatDateTime(Exam exam) {
        Map<String, Object> dateTimeInfo = new HashMap<>();
        
        try {
            // Format start date and time
            if (exam.getStartDate() != null && exam.getStartTime() != null) {
                dateTimeInfo.put("startDateTime", exam.getStartDate() + " at " + exam.getStartTime());
                dateTimeInfo.put("startDateFormatted", formatDate(exam.getStartDate()));
                dateTimeInfo.put("startTimeFormatted", formatTime(exam.getStartTime()));
            }

            // Format end date and time (due date)
            if (exam.getEndDate() != null && exam.getEndTime() != null) {
                dateTimeInfo.put("endDateTime", exam.getEndDate() + " at " + exam.getEndTime());
                dateTimeInfo.put("endDateFormatted", formatDate(exam.getEndDate()));
                dateTimeInfo.put("endTimeFormatted", formatTime(exam.getEndTime()));
                dateTimeInfo.put("dueDateTime", "Due: " + formatDate(exam.getEndDate()) + " at " + formatTime(exam.getEndTime()));
            }
        } catch (Exception e) {
            logger.warn("Error formatting date/time for exam: {}", exam.getTitle(), e);
            dateTimeInfo.put("startDateTime", "Not set");
            dateTimeInfo.put("endDateTime", "Not set");
            dateTimeInfo.put("dueDateTime", "Due date not set");
        }
        
        return dateTimeInfo;
    }

    private String formatDate(String dateStr) {
        try {
            // Convert from yyyy-MM-dd to more readable format
            String[] parts = dateStr.split("-");
            if (parts.length == 3) {
                String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int month = Integer.parseInt(parts[1]);
                return parts[2] + " " + months[month] + " " + parts[0];
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatTime(String timeStr) {
        try {
            // Convert from HH:mm to 12-hour format
            String[] parts = timeStr.split(":");
            if (parts.length == 2) {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                String ampm = hour >= 12 ? "PM" : "AM";
                hour = hour % 12;
                if (hour == 0) hour = 12;
                return String.format("%d:%02d %s", hour, minute, ampm);
            }
            return timeStr;
        } catch (Exception e) {
            return timeStr;
        }
    }

    private Map<String, Object> determineActions(Exam exam, String status) {
        Map<String, Object> actions = new HashMap<>();
        
        // Determine available actions based on exam status
        actions.put("canView", true);
        actions.put("canEdit", !status.equals("completed"));
        actions.put("canDelete", true);
        actions.put("canStart", status.equals("active"));
        actions.put("canPublish", status.equals("inactive"));
        actions.put("canUnpublish", status.equals("active"));

        // Action button text and styles
        if (status.equals("active")) {
            actions.put("primaryAction", Map.of(
                "text", "Start Exam",
                "type", "start",
                "style", "primary",
                "enabled", true
            ));
        } else if (status.equals("upcoming")) {
            actions.put("primaryAction", Map.of(
                "text", "View Details",
                "type", "view",
                "style", "secondary",
                "enabled", true
            ));
        } else if (status.equals("completed")) {
            actions.put("primaryAction", Map.of(
                "text", "View Results",
                "type", "results",
                "style", "secondary",
                "enabled", true
            ));
        } else {
            actions.put("primaryAction", Map.of(
                "text", "Publish",
                "type", "publish",
                "style", "primary",
                "enabled", true
            ));
        }
        
        return actions;
    }

    private Map<String, Object> calculateTimeRemaining(Exam exam) {
        Map<String, Object> timeInfo = new HashMap<>();
        
        try {
            if (exam.getEndDate() != null && exam.getEndTime() != null) {
                // Parse end date and time
                String endDateTimeStr = exam.getEndDate() + " " + exam.getEndTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeStr, formatter);
                LocalDateTime now = LocalDateTime.now();

                if (endDateTime.isAfter(now)) {
                    long totalMinutes = ChronoUnit.MINUTES.between(now, endDateTime);
                    long days = totalMinutes / (24 * 60);
                    long hours = (totalMinutes % (24 * 60)) / 60;
                    long minutes = totalMinutes % 60;

                    // Format countdown display
                    String countdownDisplay;
                    if (days > 0) {
                        countdownDisplay = days + "d " + hours + "h " + minutes + "m";
                    } else if (hours > 0) {
                        countdownDisplay = hours + "h " + minutes + "m";
                    } else {
                        countdownDisplay = minutes + "m";
                    }

                    timeInfo.put("timeRemaining", countdownDisplay);
                    timeInfo.put("countdownDisplay", countdownDisplay);
                    timeInfo.put("totalMinutesRemaining", totalMinutes);
                    timeInfo.put("daysRemaining", days);
                    timeInfo.put("hoursRemaining", hours);
                    timeInfo.put("minutesRemaining", minutes);
                    timeInfo.put("isExpired", false);
                    timeInfo.put("isUrgent", totalMinutes <= 60); // Less than 1 hour
                    timeInfo.put("countdownColor", totalMinutes <= 60 ? "red" : (totalMinutes <= 180 ? "orange" : "green"));
                } else {
                    timeInfo.put("timeRemaining", "Expired");
                    timeInfo.put("countdownDisplay", "Expired");
                    timeInfo.put("totalMinutesRemaining", 0);
                    timeInfo.put("daysRemaining", 0);
                    timeInfo.put("hoursRemaining", 0);
                    timeInfo.put("minutesRemaining", 0);
                    timeInfo.put("isExpired", true);
                    timeInfo.put("isUrgent", false);
                    timeInfo.put("countdownColor", "gray");
                }
            } else {
                timeInfo.put("timeRemaining", "No due date");
                timeInfo.put("countdownDisplay", "No due date");
                timeInfo.put("totalMinutesRemaining", 0);
                timeInfo.put("daysRemaining", 0);
                timeInfo.put("hoursRemaining", 0);
                timeInfo.put("minutesRemaining", 0);
                timeInfo.put("isExpired", false);
                timeInfo.put("isUrgent", false);
                timeInfo.put("countdownColor", "gray");
            }
        } catch (Exception e) {
            logger.warn("Error calculating time remaining for exam: {}", exam.getTitle(), e);
            timeInfo.put("timeRemaining", "Invalid date");
            timeInfo.put("countdownDisplay", "Invalid date");
            timeInfo.put("totalMinutesRemaining", 0);
            timeInfo.put("daysRemaining", 0);
            timeInfo.put("hoursRemaining", 0);
            timeInfo.put("minutesRemaining", 0);
            timeInfo.put("isExpired", false);
            timeInfo.put("isUrgent", false);
            timeInfo.put("countdownColor", "gray");
        }
        
        return timeInfo;
    }

    private String determineExamStatus(Exam exam) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            if (exam.getStartDate() != null && exam.getStartTime() != null) {
                String startDateTimeStr = exam.getStartDate() + " " + exam.getStartTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime startDateTime = LocalDateTime.parse(startDateTimeStr, formatter);
                
                if (now.isBefore(startDateTime)) {
                    return "upcoming";
                }
            }

            if (exam.getEndDate() != null && exam.getEndTime() != null) {
                String endDateTimeStr = exam.getEndDate() + " " + exam.getEndTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeStr, formatter);
                
                if (now.isAfter(endDateTime)) {
                    return "completed";
                }
            }

            return exam.isIsactive() ? "active" : "inactive";
        } catch (Exception e) {
            logger.warn("Error determining exam status for: {}", exam.getTitle(), e);
            return exam.isIsactive() ? "active" : "inactive";
        }
    }

    @GetMapping
    public List<Exam> getExam() {
        logger.info("Fetching all exams");
        return task.GetExam();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getExamById(@PathVariable Long id) {
        logger.info("Fetching exam by ID: {}", id);
        
        try {
            Exam exam = examRepo.findById(id).orElse(null);
            if (exam == null) {
                logger.warn("Exam not found with ID: {}", id);
                return Map.of("error", "Exam not found");
            }

            // Force loading of questions if they're lazy loaded
            if (exam.getQuestions() != null) {
                exam.getQuestions().size(); // This triggers lazy loading
                logger.info("Found exam: {} with {} questions", exam.getTitle(), exam.getQuestions().size());
                
                // Log each question for debugging
                for (int i = 0; i < exam.getQuestions().size(); i++) {
                    Question q = exam.getQuestions().get(i);
                    logger.info("Question {}: ID={}, Text={}, Type={}, Options={}, CorrectOptions={}", 
                               i+1, q.getQue_id(), q.getQuestion(), q.getType(), 
                               q.getOptions() != null ? q.getOptions().size() : 0,
                               q.getCorrect_options());
                }
            } else {
                logger.warn("Exam {} has null questions list", exam.getTitle());
            }

            Map<String, Object> enhancedData = enhanceExamData(exam);
            
            // Add completed students information
            List<Map<String, Object>> completedStudents = getCompletedStudents(id);
            enhancedData.put("completedStudents", completedStudents);
            enhancedData.put("completedStudentsCount", completedStudents.size());

            logger.info("Returning enhanced exam data with {} fields and {} completed students", 
                       enhancedData.size(), completedStudents.size());
            
            return enhancedData;
        } catch (Exception e) {
            logger.error("Error fetching exam by ID: {}", id, e);
            return Map.of("error", "Error fetching exam: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> getCompletedStudents(Long examId) {
        try {
            // Get all results for this exam
            List<com.ExamPort.ExamPort.Entity.Result> results = resultRepository.findByExamExamId(examId);
            List<Map<String, Object>> completedStudents = new java.util.ArrayList<>();

            for (com.ExamPort.ExamPort.Entity.Result result : results) {
                // Get user information
                com.ExamPort.ExamPort.Entity.User user = userRepository.findById(result.getUserId()).orElse(null);
                if (user != null) {
                    Map<String, Object> studentInfo = new HashMap<>();
                    studentInfo.put("userId", user.getId());
                    studentInfo.put("name", user.getFullName() != null ? user.getFullName() : user.getUsername());
                    studentInfo.put("email", user.getEmail());
                    studentInfo.put("username", user.getUsername());
                    studentInfo.put("score", result.getScore());

                    // Get exam details for total marks
                    Exam exam = examRepo.findById(examId).orElse(null);
                    int totalMarks = exam != null ? exam.getTotalMarks() : 0;
                    if (totalMarks == 0 && exam != null && exam.getQuestions() != null) {
                        totalMarks = exam.getQuestions().stream()
                            .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 1)
                            .sum();
                    }

                    studentInfo.put("totalMarks", totalMarks);
                    studentInfo.put("percentage", calculatePercentage(result.getScore(), exam));
                    studentInfo.put("passed", result.getPassed());
                    studentInfo.put("timeTaken", result.getTimeTaken());
                    studentInfo.put("attemptDate", result.getAttemptDate());
                    studentInfo.put("feedback", result.getFeedback());

                    completedStudents.add(studentInfo);
                }
            }

            // Sort by attempt date (most recent first)
            completedStudents.sort((a, b) -> {
                java.time.LocalDateTime dateA = (java.time.LocalDateTime) a.get("attemptDate");
                java.time.LocalDateTime dateB = (java.time.LocalDateTime) b.get("attemptDate");
                return dateB.compareTo(dateA);
            });

            logger.info("Found {} completed students for exam {}", completedStudents.size(), examId);
            return completedStudents;
        } catch (Exception e) {
            logger.error("Error fetching completed students for exam: {}", examId, e);
            return new java.util.ArrayList<>();
        }
    }

    private double calculatePercentage(double score, com.ExamPort.ExamPort.Entity.Exam exam) {
        if (exam == null) return 0.0;
        
        double totalMarks = exam.getTotalMarks();
        if (totalMarks == 0 && exam.getQuestions() != null) {
            totalMarks = exam.getQuestions().stream()
                .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 1)
                .sum();
        }
        
        return totalMarks > 0 ? Math.round((score / totalMarks) * 100.0 * 100.0) / 100.0 : 0.0;
    }

    @DeleteMapping("/{id}")
    public void deleteExam(@PathVariable Long id) {
        logger.info("Deleting exam with ID: {}", id);
        task.deleteExam(id);
        logger.info("Exam deleted successfully with ID: {}", id);
    }

    /**
     * Check if a student has access to a specific exam
     */
    @GetMapping("/{examId}/access")
    public Map<String, Object> checkExamAccess(@PathVariable Long examId, 
                                              org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        logger.info("Checking exam access for user: {} and exam: {}", username, examId);
        
        try {
            // Get user by username
            com.ExamPort.ExamPort.Entity.User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.warn("User not found: {}", username);
                return Map.of("hasAccess", false, "reason", "User not found");
            }

            // Get exam
            Exam exam = examRepo.findById(examId).orElse(null);
            if (exam == null) {
                logger.warn("Exam not found: {}", examId);
                return Map.of("hasAccess", false, "reason", "Exam not found");
            }

            // Check access
            boolean hasAccess = isExamAccessibleToStudent(exam, user);
            String reason = "";
            if (!hasAccess) {
                if (exam.getCourse() == null) {
                    reason = "Exam has no associated course";
                } else if ("PRIVATE".equals(exam.getCourse().getVisibility())) {
                    reason = "You are not in the allowed emails list for this private course";
                } else if ("PUBLIC".equals(exam.getCourse().getVisibility())) {
                    reason = "You are not enrolled in this course";
                } else {
                    reason = "Course visibility is not properly configured";
                }
            }

            // Check if already submitted
            boolean hasSubmitted = resultRepository.findByUserIdAndExamExamId(user.getId(), examId).isPresent();

            Map<String, Object> response = new HashMap<>();
            response.put("hasAccess", hasAccess);
            response.put("reason", reason);
            response.put("hasSubmitted", hasSubmitted);
            response.put("examTitle", exam.getTitle());
            response.put("courseId", exam.getCourse() != null ? exam.getCourse().getId() : null);
            response.put("courseName", exam.getCourse() != null ? exam.getCourse().getName() : null);
            response.put("courseVisibility", exam.getCourse() != null ? exam.getCourse().getVisibility() : null);

            logger.info("Exam access check result for user {} and exam {}: hasAccess={}, hasSubmitted={}", 
                       username, examId, hasAccess, hasSubmitted);
            
            return response;
        } catch (Exception e) {
            logger.error("Error checking exam access for user: {} and exam: {}", username, examId, e);
            return Map.of("hasAccess", false, "reason", "Error checking access: " + e.getMessage());
        }
    }

    @GetMapping("/allowed/{email}")
    public List<Map<String, Object>> getAllowedExams(@PathVariable String email) {
        logger.info("Fetching allowed exams for email: {}", email);
        
        try {
            // Get user by email
            com.ExamPort.ExamPort.Entity.User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                logger.warn("User not found with email: {}", email);
                return List.of();
            }

            Long userId = user.getId();
            List<Exam> allExams = task.GetExam();
            List<Exam> allowedExams = allExams.stream()
                .filter(exam -> isExamAccessibleToStudent(exam, user))
                .toList();

            logger.info("Found {} allowed exams for email: {} (user ID: {})", allowedExams.size(), email, userId);

            // Log detailed information about exam access
            if (allowedExams.isEmpty()) {
                logger.info("No exams found for user {}. Checking enrollment and course access:", email);
                
                // Check enrolled courses
                List<com.ExamPort.ExamPort.Entity.Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(userId);
                logger.info("User {} has {} enrollments", email, enrollments.size());
                
                for (com.ExamPort.ExamPort.Entity.Enrollment enrollment : enrollments) {
                    com.ExamPort.ExamPort.Entity.Course course = enrollment.getCourse();
                    logger.info("Enrolled in course: {} (ID: {}, Visibility: {})", 
                               course.getName(), course.getId(), course.getVisibility());
                    
                    // Check if course has exams
                    List<Exam> courseExams = allExams.stream()
                        .filter(exam -> exam.getCourse() != null && exam.getCourse().getId().equals(course.getId()))
                        .toList();
                    logger.info("Course {} has {} exams", course.getName(), courseExams.size());
                }

                // Check private course access
                List<Exam> privateExams = allExams.stream()
                    .filter(exam -> exam.getCourse() != null && 
                                  "PRIVATE".equals(exam.getCourse().getVisibility()) &&
                                  exam.getCourse().getAllowedEmails() != null &&
                                  exam.getCourse().getAllowedEmails().contains(email))
                    .toList();
                logger.info("User {} has access to {} private course exams", email, privateExams.size());
            }

            return allowedExams.stream()
                .map(exam -> enhanceExamDataForStudent(exam, userId))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching allowed exams for email: {}", email, e);
            return List.of();
        }
    }

    /**
     * Get exams for enrolled courses (alternative endpoint for debugging)
     */
    @GetMapping("/student/enrolled")
    public List<Map<String, Object>> getExamsForEnrolledCourses(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        logger.info("Fetching exams for enrolled courses for student: {}", username);
        
        try {
            // Get user by username
            com.ExamPort.ExamPort.Entity.User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.warn("User not found: {}", username);
                return List.of();
            }

            // Get enrolled courses
            List<com.ExamPort.ExamPort.Entity.Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(user.getId());
            logger.info("Found {} enrollments for student: {}", enrollments.size(), username);

            List<Exam> enrolledExams = new ArrayList<>();
            for (com.ExamPort.ExamPort.Entity.Enrollment enrollment : enrollments) {
                com.ExamPort.ExamPort.Entity.Course course = enrollment.getCourse();
                logger.info("Processing course: {} (ID: {})", course.getName(), course.getId());
                
                // Get exams for this course
                List<Exam> courseExams = examRepo.findByCourse_Id(course.getId());
                logger.info("Found {} exams for course: {}", courseExams.size(), course.getName());
                
                enrolledExams.addAll(courseExams);
            }

            logger.info("Total exams from enrolled courses: {}", enrolledExams.size());
            
            return enrolledExams.stream()
                .map(exam -> enhanceExamDataForStudent(exam, user.getId()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching exams for enrolled courses for student: {}", username, e);
            return List.of();
        }
    }

    /**
     * Get exams for a specific course
     */
    @GetMapping("/course/{courseId}")
    public List<Map<String, Object>> getExamsByCourse(@PathVariable Long courseId) {
        logger.info("Fetching exams for course: {}", courseId);
        
        try {
            List<Exam> courseExams = examRepo.findByCourse_Id(courseId);
            logger.info("Found {} exams for course: {}", courseExams.size(), courseId);
            
            return courseExams.stream()
                .map(this::enhanceExamData)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching exams for course: {}", courseId, e);
            return List.of();
        }
    }

    /**
     * Determines if an exam is accessible to a student based on course enrollment and access rules
     */
    private boolean isExamAccessibleToStudent(Exam exam, com.ExamPort.ExamPort.Entity.User user) {
        if (exam.getCourse() == null) {
            logger.warn("Exam {} has no associated course", exam.getTitle());
            return false;
        }

        com.ExamPort.ExamPort.Entity.Course course = exam.getCourse();
        String email = user.getEmail();
        Long userId = user.getId();

        // Check course visibility and access
        if ("PRIVATE".equals(course.getVisibility())) {
            // For private courses, check if user is in allowed emails list (backward compatibility)
            boolean isInAllowedEmails = course.getAllowedEmails() != null && course.getAllowedEmails().contains(email);
            if (isInAllowedEmails) {
                logger.info("User {} has access to private course {} via allowed emails", email, course.getName());
                return true;
            } else {
                logger.info("User {} does not have access to private course {} - not in allowed emails", email, course.getName());
                return false;
            }
        } else if ("PUBLIC".equals(course.getVisibility())) {
            // For public courses, check enrollment status
            boolean isEnrolled = enrollmentService.isStudentEnrolledInCourse(userId, course.getId());
            if (isEnrolled) {
                logger.info("User {} has access to public course {} via enrollment", email, course.getName());
                return true;
            } else {
                logger.info("User {} does not have access to public course {} - not enrolled", email, course.getName());
                return false;
            }
        }

        logger.warn("Course {} has unknown visibility: {}", course.getName(), course.getVisibility());
        return false;
    }

    private Map<String, Object> enhanceExamDataForStudent(Exam exam, Long userId) {
        Map<String, Object> examData = enhanceExamData(exam);
        
        // Create a mutable copy of the exam data
        Map<String, Object> mutableExamData = new HashMap<>(examData);

        // Check if student has already submitted this exam
        boolean hasSubmitted = false;
        if (userId != null) {
            hasSubmitted = resultRepository.findByUserIdAndExamExamId(userId, exam.getExam_id()).isPresent();
            logger.info("Checking submission for user {} and exam {}: hasSubmitted = {}", 
                       userId, exam.getExam_id(), hasSubmitted);
        }

        mutableExamData.put("hasSubmitted", hasSubmitted);
        mutableExamData.put("isSubmitted", hasSubmitted);
        mutableExamData.put("isAttempted", hasSubmitted);

        // Determine exam status for student - this overrides the general status
        String studentExamStatus = determineStudentExamStatus(exam, hasSubmitted);
        mutableExamData.put("status", studentExamStatus);

        // Override isExpired flag based on student status
        if ("missed".equals(studentExamStatus)) {
            mutableExamData.put("isExpired", true);
        } else if ("completed".equals(studentExamStatus) || "active".equals(studentExamStatus) || "upcoming".equals(studentExamStatus)) {
            mutableExamData.put("isExpired", false);
        }

        // Update actions based on submission status and exam status
        @SuppressWarnings("unchecked")
        Map<String, Object> actions = (Map<String, Object>) mutableExamData.get("actions");
        if (actions != null) {
            // Create mutable copy of actions
            Map<String, Object> mutableActions = new HashMap<>(actions);
            
            if (hasSubmitted) {
                mutableActions.put("canStart", false);
                @SuppressWarnings("unchecked")
                Map<String, Object> primaryAction = (Map<String, Object>) mutableActions.get("primaryAction");
                if (primaryAction != null) {
                    // Create mutable copy of primary action
                    Map<String, Object> mutablePrimaryAction = new HashMap<>(primaryAction);
                    mutablePrimaryAction.put("text", "Submitted");
                    mutablePrimaryAction.put("type", "submitted");
                    mutablePrimaryAction.put("style", "secondary");
                    mutablePrimaryAction.put("enabled", false);
                    mutableActions.put("primaryAction", mutablePrimaryAction);
                }
            } else if ("missed".equals(studentExamStatus)) {
                mutableActions.put("canStart", false);
                @SuppressWarnings("unchecked")
                Map<String, Object> primaryAction = (Map<String, Object>) mutableActions.get("primaryAction");
                if (primaryAction != null) {
                    // Create mutable copy of primary action
                    Map<String, Object> mutablePrimaryAction = new HashMap<>(primaryAction);
                    mutablePrimaryAction.put("text", "Missed");
                    mutablePrimaryAction.put("type", "missed");
                    mutablePrimaryAction.put("style", "danger");
                    mutablePrimaryAction.put("enabled", false);
                    mutableActions.put("primaryAction", mutablePrimaryAction);
                }
            }
            
            mutableExamData.put("actions", mutableActions);
        }

        logger.info("Exam {} - hasSubmitted: {}, status: {} for user: {}, endDate: {}, endTime: {}", 
                   exam.getTitle(), hasSubmitted, studentExamStatus, userId, exam.getEndDate(), exam.getEndTime());
        
        return mutableExamData;
    }

    private String determineStudentExamStatus(Exam exam, boolean hasSubmitted) {
        try {
            LocalDateTime now = LocalDateTime.now();
            logger.info("Determining status for exam: {}, hasSubmitted: {}, current time: {}", 
                       exam.getTitle(), hasSubmitted, now);

            // If already submitted, it's completed
            if (hasSubmitted) {
                logger.info("Exam {} marked as completed - student has submitted", exam.getTitle());
                return "completed";
            }

            // Check if exam has start date and time
            if (exam.getStartDate() != null && exam.getStartTime() != null) {
                String startDateTimeStr = exam.getStartDate() + " " + exam.getStartTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime startDateTime = LocalDateTime.parse(startDateTimeStr, formatter);
                
                // If current time is before start time, it's upcoming
                if (now.isBefore(startDateTime)) {
                    return "upcoming";
                }
            }

            // Check if exam has end date and time
            if (exam.getEndDate() != null && exam.getEndTime() != null) {
                String endDateTimeStr = exam.getEndDate() + " " + exam.getEndTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeStr, formatter);
                
                logger.info("Exam {} end time: {}, current time: {}, is after end: {}", 
                           exam.getTitle(), endDateTime, now, now.isAfter(endDateTime));
                
                // If current time is after end time and not submitted, it's missed
                if (now.isAfter(endDateTime)) {
                    logger.info("Exam {} marked as missed - current time is after end time", exam.getTitle());
                    return "missed";
                }
            }

            // If exam is active and within time bounds, it's active
            return exam.isIsactive() ? "active" : "inactive";
        } catch (Exception e) {
            logger.error("Error determining student exam status for: {} - {}", exam.getTitle(), e.getMessage(), e);
            
            // Fallback: if there's an error parsing dates, check if exam is generally expired
            try {
                if (exam.getEndDate() != null) {
                    // Simple date comparison without time
                    LocalDateTime now = LocalDateTime.now();
                    String[] dateParts = exam.getEndDate().split("-");
                    if (dateParts.length == 3) {
                        int year = Integer.parseInt(dateParts[0]);
                        int month = Integer.parseInt(dateParts[1]);
                        int day = Integer.parseInt(dateParts[2]);
                        LocalDateTime examEndDate = LocalDateTime.of(year, month, day, 23, 59);
                        
                        if (now.isAfter(examEndDate) && !hasSubmitted) {
                            logger.info("Exam {} marked as missed via fallback logic", exam.getTitle());
                            return "missed";
                        }
                    }
                }
            } catch (Exception fallbackError) {
                logger.error("Fallback date parsing also failed for exam: {}", exam.getTitle(), fallbackError);
            }
            
            return exam.isIsactive() ? "active" : "inactive";
        }
    }

    // Submit exam endpoint
    @PostMapping("/{id}/submit")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> submitExam(@PathVariable Long id, @RequestBody Map<String, Object> requestBody, 
                                         org.springframework.security.core.Authentication authentication) {
        logger.info("Submitting exam: {} with request body: {}", id, requestBody);
        
        // Extract answers and time taken from the request body
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) requestBody.get("answers");
        if (answers == null) {
            answers = new HashMap<>();
        }
        
        Integer timeTaken = (Integer) requestBody.get("timeTaken");
        if (timeTaken == null) {
            timeTaken = 0;
        }

        logger.info("Extracted {} answers for exam {} with time taken: {} seconds", 
                   answers.size(), id, timeTaken);

        try {
            // Get exam
            Exam exam = examRepo.findById(id).orElse(null);
            if (exam == null) {
                logger.warn("Exam not found for submission: {}", id);
                return Map.of("error", "Exam not found");
            }

            // Get user
            String username = authentication.getName();
            com.ExamPort.ExamPort.Entity.User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.warn("User not found for submission: {}", username);
                return Map.of("error", "User not found");
            }

            // Check if user already submitted this exam
            if (resultRepository.findByUserIdAndExamExamId(user.getId(), id).isPresent()) {
                logger.warn("User {} already submitted exam {}", username, id);
                return Map.of("error", "Exam already submitted");
            }

            // Log the submitted answers
            logger.info("Submitted answers for exam {}: {}", exam.getTitle(), answers);

            // Calculate score
            double score = calculateScore(exam, answers);
            double totalMarks = exam.getTotalMarks() > 0 ? exam.getTotalMarks() : 
                (exam.getQuestions() != null ? exam.getQuestions().stream()
                    .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 1)
                    .sum() : 0);

            // Determine if passed (60% threshold)
            boolean passed = totalMarks > 0 && (score / totalMarks) >= 0.6;

            // Convert answers to JSON string
            String answersJson = convertAnswersToJson(answers);

            // Create and save result
            com.ExamPort.ExamPort.Entity.Result result = new com.ExamPort.ExamPort.Entity.Result();
            
            // Generate a unique ID (timestamp + user ID + exam ID)
            Long resultId = System.currentTimeMillis() + (user.getId() * 1000) + id;
            result.setId(resultId);
            result.setAnswers(answersJson);
            result.setExamExamId(id);
            result.setUserId(user.getId());
            result.setScore(score);
            result.setTimeTaken(timeTaken);
            result.setPassed(passed);
            result.setAttemptDate(LocalDateTime.now());

            // Generate feedback
            String feedback = generateFeedback(score, totalMarks, passed);
            result.setFeedback(feedback);

            // Save result using native SQL to avoid Hibernate issues
            try {
                String sql = "INSERT INTO result (id, answers, attempt_date, exam_exam_id, feedback, passed, score, time_taken, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                int rowsAffected = entityManager.createNativeQuery(sql)
                    .setParameter(1, resultId)
                    .setParameter(2, answersJson)
                    .setParameter(3, result.getAttemptDate())
                    .setParameter(4, id)
                    .setParameter(5, feedback)
                    .setParameter(6, passed)
                    .setParameter(7, score)
                    .setParameter(8, timeTaken)
                    .setParameter(9, user.getId())
                    .executeUpdate();

                if (rowsAffected > 0) {
                    logger.info("Successfully saved result with ID: {}", resultId);
                    result.setId(resultId); // Set the ID for response
                } else {
                    throw new RuntimeException("Failed to insert result");
                }
            } catch (Exception e) {
                logger.error("Error saving result with ID {}, trying alternative ID", resultId, e);
                
                // Try with a different ID if there's a conflict
                resultId = System.currentTimeMillis() + (user.getId() * 10000) + (id * 100);
                try {
                    String sql = "INSERT INTO result (id, answers, attempt_date, exam_exam_id, feedback, passed, score, time_taken, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    int rowsAffected = entityManager.createNativeQuery(sql)
                        .setParameter(1, resultId)
                        .setParameter(2, answersJson)
                        .setParameter(3, result.getAttemptDate())
                        .setParameter(4, id)
                        .setParameter(5, feedback)
                        .setParameter(6, passed)
                        .setParameter(7, score)
                        .setParameter(8, timeTaken)
                        .setParameter(9, user.getId())
                        .executeUpdate();

                    if (rowsAffected > 0) {
                        logger.info("Successfully saved result with alternative ID: {}", resultId);
                        result.setId(resultId);
                    } else {
                        throw new RuntimeException("Failed to insert result with alternative ID");
                    }
                } catch (Exception e2) {
                    logger.error("Failed to save result even with alternative ID", e2);
                    throw new RuntimeException("Failed to save exam result", e2);
                }
            }

            logger.info("Saved exam result: {} for user: {} with score: {}/{}", 
                       result.getId(), username, score, totalMarks);

            // Calculate completion stats
            int totalQuestions = exam.getQuestions() != null ? exam.getQuestions().size() : 0;
            int answeredQuestions = (int) answers.values().stream()
                .filter(answer -> answer != null && !answer.trim().isEmpty())
                .count();
            double completionPercentage = totalQuestions > 0 ? (double) answeredQuestions / totalQuestions * 100 : 0;
            double scorePercentage = totalMarks > 0 ? (score / totalMarks) * 100 : 0;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Exam submitted successfully");
            response.put("result_id", result.getId());
            response.put("exam_id", id);
            response.put("exam_title", exam.getTitle());
            response.put("score", score);
            response.put("total_marks", totalMarks);
            response.put("percentage", Math.round(scorePercentage * 100.0) / 100.0);
            response.put("passed", passed);
            response.put("total_questions", totalQuestions);
            response.put("answered_questions", answeredQuestions);
            response.put("completion_percentage", Math.round(completionPercentage));
            response.put("time_taken", timeTaken);
            response.put("feedback", feedback);
            response.put("submitted_at", result.getAttemptDate().toString());

            logger.info("Exam {} submitted successfully by {}. Score: {}/{} ({}%), Passed: {}", 
                       exam.getTitle(), username, score, totalMarks, Math.round(scorePercentage), passed);

            // Send email notification to student
            try {
                logger.info("Sending exam result email to student: {}", user.getEmail());
                emailService.sendExamResultNotificationHtml(user, exam, result, answers);
                logger.info("Exam result email sent successfully to: {}", user.getEmail());
            } catch (Exception emailError) {
                logger.error("Failed to send exam result email to: {} - {}", user.getEmail(), emailError.getMessage());
                // Don't fail the exam submission if email fails
            }

            return response;
        } catch (Exception e) {
            logger.error("Error submitting exam: {}", id, e);
            return Map.of("error", "Error submitting exam: " + e.getMessage());
        }
    }

    private double calculateScore(Exam exam, Map<String, String> answers) {
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        logger.info("Starting score calculation for exam: {} with {} questions", 
                   exam.getTitle(), exam.getQuestions().size());

        for (com.ExamPort.ExamPort.Entity.Question question : exam.getQuestions()) {
            String userAnswer = answers.get(String.valueOf(question.getQue_id()));
            logger.debug("Processing question {}: User answer: '{}'", question.getQue_id(), userAnswer);

            if (userAnswer == null || userAnswer.trim().isEmpty()) {
                logger.debug("Question {}: No answer provided", question.getQue_id());
                continue; // No answer provided
            }

            double questionScore = 0.0;
            int questionMarks = question.getMarks() != null ? question.getMarks() : 1;

            if ("mcq".equals(question.getType()) || "multiple".equals(question.getType())) {
                // For MCQ questions, check against correct options
                if (question.getCorrect_options() != null && !question.getCorrect_options().isEmpty()) {
                    logger.debug("Question {}: Correct options indices: {}", 
                               question.getQue_id(), question.getCorrect_options());

                    if ("mcq".equals(question.getType())) {
                        // Single choice - try multiple comparison methods
                        int correctOptionIndex = question.getCorrect_options().get(0);
                        if (question.getOptions() != null && correctOptionIndex < question.getOptions().size()) {
                            String correctAnswer = question.getOptions().get(correctOptionIndex).getAvailableOption();
                            String correctOptionId = String.valueOf(question.getOptions().get(correctOptionIndex).getOption_id());
                            String correctIndexStr = String.valueOf(correctOptionIndex);

                            logger.debug("Question {}: Correct answer text: '{}', Option ID: '{}', Index: '{}'", 
                                       question.getQue_id(), correctAnswer, correctOptionId, correctIndexStr);

                            // Try different comparison methods
                            boolean isCorrectByText = correctAnswer != null && correctAnswer.equals(userAnswer.trim());
                            boolean isCorrectByIndex = correctIndexStr.equals(userAnswer.trim());
                            boolean isCorrectByOptionId = correctOptionId.equals(userAnswer.trim());

                            logger.debug("Question {}: Comparison results - byText: {}, byIndex: {}, byOptionId: {}", 
                                       question.getQue_id(), isCorrectByText, isCorrectByIndex, isCorrectByOptionId);

                            if (isCorrectByText || isCorrectByIndex || isCorrectByOptionId) {
                                questionScore = questionMarks;
                                logger.info("Question {}: CORRECT! Awarded {} marks", question.getQue_id(), questionMarks);
                            } else {
                                logger.info("Question {}: INCORRECT. User: '{}', Correct: '{}'", 
                                           question.getQue_id(), userAnswer, correctAnswer);
                            }
                        }
                    } else {
                        // Multiple choice - partial scoring
                        String[] userAnswers = userAnswer.split(",");
                        List<String> userAnswersList = java.util.Arrays.stream(userAnswers)
                            .map(String::trim)
                            .collect(java.util.stream.Collectors.toList());

                        List<String> correctAnswers = question.getCorrect_options().stream()
                            .filter(index -> question.getOptions() != null && index < question.getOptions().size())
                            .map(index -> question.getOptions().get(index).getAvailableOption())
                            .collect(java.util.stream.Collectors.toList());

                        logger.debug("Question {}: User answers: {}, Correct answers: {}", 
                                   question.getQue_id(), userAnswersList, correctAnswers);

                        // Calculate partial score based on correct selections
                        int correctSelections = 0;
                        int incorrectSelections = 0;

                        for (String userAns : userAnswersList) {
                            if (correctAnswers.contains(userAns)) {
                                correctSelections++;
                            } else {
                                incorrectSelections++;
                            }
                        }

                        // Partial scoring: (correct - incorrect) / total_correct * marks
                        if (correctAnswers.size() > 0) {
                            double partialScore = Math.max(0, correctSelections - incorrectSelections);
                            questionScore = (partialScore / correctAnswers.size()) * questionMarks;
                        }

                        logger.info("Question {}: Multiple choice - Correct: {}, Incorrect: {}, Score: {}/{}", 
                                   question.getQue_id(), correctSelections, incorrectSelections, questionScore, questionMarks);
                    }
                }
            } else {
                // For text questions, give full marks if answered (manual grading needed)
                questionScore = questionMarks;
                logger.info("Question {}: Text question - Awarded full marks: {}", question.getQue_id(), questionMarks);
            }

            totalScore += questionScore;
            logger.debug("Question {}: Final score: {}/{}, Running total: {}", 
                        question.getQue_id(), questionScore, questionMarks, totalScore);
        }

        logger.info("Final score calculation completed: {}/{} total marks", totalScore, exam.getTotalMarks());
        return Math.round(totalScore * 100.0) / 100.0;
    }

    private String convertAnswersToJson(Map<String, String> answers) {
        try {
            // Simple JSON conversion
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : answers.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue().replace("\"", "\\\"")).append("\"");
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.warn("Error converting answers to JSON", e);
            return answers.toString();
        }
    }

    private String generateFeedback(double score, double totalMarks, boolean passed) {
        if (totalMarks == 0) {
            return "Exam completed. Manual review required.";
        }

        double percentage = (score / totalMarks) * 100;
        StringBuilder feedback = new StringBuilder();
        
        feedback.append(String.format("You scored %.1f out of %.1f marks (%.1f%%). ", score, totalMarks, percentage));

        if (passed) {
            if (percentage >= 90) {
                feedback.append("Excellent work! Outstanding performance.");
            } else if (percentage >= 80) {
                feedback.append("Great job! Very good performance.");
            } else if (percentage >= 70) {
                feedback.append("Good work! Solid performance.");
            } else {
                feedback.append("You passed! Keep up the good work.");
            }
        } else {
            feedback.append("You did not meet the passing criteria (60%). Please review the material and try again.");
        }

        return feedback.toString();
    }

    @PutMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> updateExam(@PathVariable Long id, @RequestBody Exam examData, 
                                         org.springframework.security.core.Authentication authentication) {
        logger.info("Updating exam with ID: {} by user: {}", id, authentication.getName());
        
        try {
            // Get existing exam
            Exam existingExam = examRepo.findById(id).orElse(null);
            if (existingExam == null) {
                logger.warn("Exam not found for update: {}", id);
                return Map.of("error", "Exam not found");
            }

            // Verify user has permission to update this exam
            String username = authentication.getName();
            com.ExamPort.ExamPort.Entity.User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.warn("User not found: {}", username);
                return Map.of("error", "User not found");
            }

            // Check if user is the instructor of the course this exam belongs to
            if (existingExam.getCourse() != null && existingExam.getCourse().getInstructor() != null &&
                !existingExam.getCourse().getInstructor().getId().equals(user.getId())) {
                logger.warn("User {} does not have permission to update exam {}", username, id);
                return Map.of("error", "Permission denied");
            }

            // Set the ID to ensure we're updating the existing exam
            examData.setExam_id(id);

            // Preserve the course if not provided
            if (examData.getCourse() == null) {
                examData.setCourse(existingExam.getCourse());
            }

            // Update the exam using TaskService
            Exam savedExam = task.updateExam(examData);

            logger.info("Successfully updated exam: {} with {} questions", 
                       savedExam.getTitle(), savedExam.getQuestions() != null ? savedExam.getQuestions().size() : 0);

            // Return enhanced exam data
            Map<String, Object> response = enhanceExamData(savedExam);
            response.put("success", true);
            response.put("message", "Exam updated successfully");

            return response;
        } catch (Exception e) {
            logger.error("Error updating exam: {}", id, e);
            return Map.of("error", "Error updating exam: " + e.getMessage(), "success", false);
        }
    }
}