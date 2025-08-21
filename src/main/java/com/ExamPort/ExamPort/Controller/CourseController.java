package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.*;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Repository.EnrollmentRepository;
import com.ExamPort.ExamPort.Service.PaymentService;
import com.ExamPort.ExamPort.Service.ValidationService;
import com.ExamPort.ExamPort.Exception.ValidationException;
import com.ExamPort.ExamPort.Exception.CourseException;
import com.ExamPort.ExamPort.Exception.PaymentException;
import javax.transaction.Transactional;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    
    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ValidationService validationService;

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<?> createCourse(@RequestParam("name") String name,
                                          @RequestParam(value = "description", required = false) String description,
                                          @RequestParam("visibility") String visibilityStr,
                                          @RequestParam("pricing") String pricingStr,
                                          @RequestParam(value = "price", required = false) String priceStr,
                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                          Authentication authentication) {
        
        String username = authentication.getName();
        logger.info("Course creation request by instructor: {} for course: {}", username, name);
        
        try {
            // Find instructor
            Optional<User> instructorOpt = userRepository.findByUsername(username);
            if (instructorOpt.isEmpty()) {
                logger.warn("Course creation failed - Instructor not found: {}", username);
                return ResponseEntity.badRequest().body(Map.of("error", "Instructor not found"));
            }
            
            User instructor = instructorOpt.get();
            
            // Parse and validate visibility
            CourseVisibility visibility;
            try {
                visibility = CourseVisibility.valueOf(visibilityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid visibility value. Must be PRIVATE or PUBLIC"));
            }
            
            // Parse and validate pricing
            CoursePricing pricing;
            try {
                pricing = CoursePricing.valueOf(pricingStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid pricing value. Must be FREE or PAID"));
            }
            
            // Parse and validate price
            BigDecimal price = null;
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                try {
                    price = new BigDecimal(priceStr);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid price format"));
                }
            }
            
            // Process allowed emails for private courses
            List<String> allowedEmails = null;
            if (visibility == CourseVisibility.PRIVATE) {
                if (file == null || file.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email file is required for private courses"));
                }
                
                logger.debug("Processing email file for private course: {}", name);
                allowedEmails = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int lineNum = 1;
                    while ((line = reader.readLine()) != null) {
                        String email = line.trim();
                        if (!email.isEmpty()) {
                            // Basic validation
                            if (email.length() > 100) {
                                logger.warn("Email validation failed - Email too long at line {}: {}", lineNum, email);
                                return ResponseEntity.badRequest().body(Map.of("error", "Email too long at line " + lineNum + ": " + email));
                            }
                            if (!validationService.isValidEmail(email)) {
                                logger.warn("Email validation failed - Invalid format at line {}: {}", lineNum, email);
                                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format at line " + lineNum + ": " + email));
                            }
                            allowedEmails.add(email);
                        }
                        lineNum++;
                    }
                }
                logger.debug("Processed {} valid emails for private course: {}", allowedEmails.size(), name);
            }
            
            // Create course object
            Course course = new Course();
            course.setName(name);
            course.setDescription(description);
            course.setVisibility(visibility);
            course.setPricing(pricing);
            course.setPrice(price);
            course.setAllowedEmails(allowedEmails);
            course.setInstructor(instructor);
            
            // Validate course using validation service
            validationService.validateCourseCreation(course, instructor);
            
            // Save course
            Course savedCourse = courseRepository.save(course);
            
            // Auto-enroll students for private courses
            int autoEnrolledCount = 0;
            if (visibility == CourseVisibility.PRIVATE && allowedEmails != null && !allowedEmails.isEmpty()) {
                logger.info("Auto-enrolling students in private course: {}", name);
                for (String email : allowedEmails) {
                    try {
                        Optional<User> studentOpt = userRepository.findByEmail(email);
                        if (studentOpt.isPresent()) {
                            User student = studentOpt.get();
                            
                            // Check if student is not already enrolled
                            Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(student.getId(), savedCourse.getId());
                            if (existingEnrollment.isEmpty()) {
                                Enrollment enrollment = new Enrollment(student, savedCourse, EnrollmentStatus.ENROLLED);
                                enrollmentRepository.save(enrollment);
                                autoEnrolledCount++;
                                logger.debug("Auto-enrolled student: {} in private course: {}", email, name);
                            }
                        } else {
                            logger.debug("Student with email {} not found in system for auto-enrollment", email);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to auto-enroll student with email: {} in course: {} - {}", email, name, e.getMessage());
                    }
                }
                logger.info("Auto-enrolled {} students in private course: {}", autoEnrolledCount, name);
            }
            
            logger.info("Course created successfully: {} by instructor: {} with visibility: {}, pricing: {}", 
                       name, username, visibility, pricing);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedCourse.getId());
            response.put("name", savedCourse.getName());
            response.put("description", savedCourse.getDescription());
            response.put("visibility", savedCourse.getVisibility());
            response.put("pricing", savedCourse.getPricing());
            response.put("price", savedCourse.getPrice());
            response.put("instructor", Map.of(
                "id", instructor.getId(),
                "username", instructor.getUsername(),
                "fullName", instructor.getFullName() != null ? instructor.getFullName() : instructor.getUsername()
            ));
            response.put("message", "Course created successfully");
            if (visibility == CourseVisibility.PRIVATE && autoEnrolledCount > 0) {
                response.put("autoEnrolledStudents", autoEnrolledCount);
                response.put("enrollmentMessage", "Automatically enrolled " + autoEnrolledCount + " students");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (ValidationException e) {
            logger.warn("Course validation failed for instructor: {} - {}", username, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            if (e.hasFieldErrors()) {
                errorResponse.put("fieldErrors", e.getFieldErrors());
            }
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (CourseException e) {
            logger.warn("Course creation failed for instructor: {} - {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating course: {} by instructor: {}", name, username, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/instructor")
    public ResponseEntity<?> getCoursesForInstructor(Authentication authentication) {
        String username = authentication.getName();
        logger.info("Fetching courses for instructor: {}", username);
        
        try {
            Optional<User> instructorOpt = userRepository.findByUsername(username);
            if (instructorOpt.isEmpty()) {
                logger.warn("Get courses failed - Instructor not found: {}", username);
                return ResponseEntity.badRequest().body("Instructor not found");
            }
            
            User instructor = instructorOpt.get();
            List<Course> courses = courseRepository.findByInstructor_Id(instructor.getId());
            
            // Create response with enrollment counts and exam counts
            List<Map<String, Object>> coursesWithStats = new ArrayList<>();
            for (Course course : courses) {
                Map<String, Object> courseData = new HashMap<>();
                courseData.put("id", course.getId());
                courseData.put("name", course.getName());
                courseData.put("description", course.getDescription());
                courseData.put("visibility", course.getVisibility());
                courseData.put("pricing", course.getPricing());
                courseData.put("price", course.getPrice());
                courseData.put("instructor", Map.of(
                    "id", instructor.getId(),
                    "username", instructor.getUsername(),
                    "fullName", instructor.getFullName() != null ? instructor.getFullName() : instructor.getUsername()
                ));
                
                // Add enrollment count - handle private courses differently
                Long enrollmentCount = 0L;
                if (course.getVisibility() == CourseVisibility.PRIVATE) {
                    // For private courses, count actual enrollments but also show allowed emails count
                    enrollmentCount = enrollmentRepository.countEnrolledStudentsByCourseId(course.getId());
                    int allowedEmailsCount = course.getAllowedEmails() != null ? course.getAllowedEmails().size() : 0;
                    courseData.put("allowedEmailsCount", allowedEmailsCount);
                    logger.debug("Private course {} - Enrolled: {}, Allowed emails: {}", 
                               course.getName(), enrollmentCount, allowedEmailsCount);
                } else {
                    // For public courses, count actual enrollments
                    enrollmentCount = enrollmentRepository.countEnrolledStudentsByCourseId(course.getId());
                }
                courseData.put("enrollmentCount", enrollmentCount);
                
                // Add exam count (if exams relationship exists)
                int examCount = course.getExams() != null ? course.getExams().size() : 0;
                courseData.put("examCount", examCount);
                
                coursesWithStats.add(courseData);
            }
            
            logger.info("Retrieved {} courses for instructor: {}", courses.size(), username);
            return ResponseEntity.ok(coursesWithStats);
        } catch (Exception e) {
            logger.error("Error fetching courses for instructor: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching courses");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourse(@PathVariable Long id) {
        logger.info("Fetching course with ID: {}", id);
        
        try {
            Optional<Course> courseOpt = courseRepository.findById(id);
            if (courseOpt.isPresent()) {
                logger.debug("Course found: {}", courseOpt.get().getName());
                return ResponseEntity.ok(courseOpt.get());
            } else {
                logger.warn("Course not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching course with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error fetching course");
        }
    }

    @GetMapping("/public")
    public ResponseEntity<?> getPublicCourses() {
        logger.info("Fetching public courses");
        
        try {
            List<Course> publicCourses = courseRepository.findByVisibility(CourseVisibility.PUBLIC);
            
            // Create response with enrollment counts
            List<Map<String, Object>> coursesWithStats = new ArrayList<>();
            for (Course course : publicCourses) {
                Map<String, Object> courseData = new HashMap<>();
                courseData.put("id", course.getId());
                courseData.put("name", course.getName());
                courseData.put("description", course.getDescription());
                courseData.put("visibility", course.getVisibility());
                courseData.put("pricing", course.getPricing());
                courseData.put("price", course.getPrice());
                courseData.put("instructor", Map.of(
                    "id", course.getInstructor().getId(),
                    "username", course.getInstructor().getUsername(),
                    "fullName", course.getInstructor().getFullName() != null ? 
                        course.getInstructor().getFullName() : course.getInstructor().getUsername()
                ));
                
                // Add enrollment count
                Long enrollmentCount = enrollmentRepository.countEnrolledStudentsByCourseId(course.getId());
                courseData.put("enrollmentCount", enrollmentCount);
                
                coursesWithStats.add(courseData);
            }
            
            logger.info("Retrieved {} public courses", publicCourses.size());
            return ResponseEntity.ok(coursesWithStats);
        } catch (Exception e) {
            logger.error("Error fetching public courses", e);
            return ResponseEntity.internalServerError().body("Error fetching public courses");
        }
    }

    @PostMapping("/{courseId}/enroll")
    @Transactional
    public ResponseEntity<?> enrollInCourse(@PathVariable Long courseId, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Enrollment request for course: {} by student: {}", courseId, username);
        
        try {
            // Find student
            Optional<User> studentOpt = userRepository.findByUsername(username);
            if (studentOpt.isEmpty()) {
                logger.warn("Enrollment failed - Student not found: {}", username);
                return ResponseEntity.badRequest().body("Student not found");
            }
            
            User student = studentOpt.get();
            
            // Find course
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                logger.warn("Enrollment failed - Course not found: {}", courseId);
                return ResponseEntity.badRequest().body("Course not found");
            }
            
            Course course = courseOpt.get();
            
            // Validate course is public and free
            if (course.getVisibility() != CourseVisibility.PUBLIC) {
                return ResponseEntity.badRequest().body("Cannot enroll in private courses through this endpoint");
            }
            
            if (course.getPricing() != CoursePricing.FREE) {
                return ResponseEntity.badRequest().body("Cannot enroll in paid courses without payment");
            }
            
            // Check if already enrolled
            Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(student.getId(), courseId);
            if (existingEnrollment.isPresent()) {
                if (existingEnrollment.get().getStatus() == EnrollmentStatus.ENROLLED) {
                    return ResponseEntity.badRequest().body("Student is already enrolled in this course");
                }
            }
            
            // Create enrollment
            Enrollment enrollment = new Enrollment(student, course, EnrollmentStatus.ENROLLED);
            enrollmentRepository.save(enrollment);
            
            logger.info("Student: {} successfully enrolled in free course: {}", username, course.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully enrolled in course");
            response.put("courseId", courseId);
            response.put("courseName", course.getName());
            response.put("enrollmentId", enrollment.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error enrolling student: {} in course: {}", username, courseId, e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{courseId}/purchase")
    @Transactional
    public ResponseEntity<?> initiatePurchase(@PathVariable Long courseId, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Purchase initiation for course: {} by student: {}", courseId, username);
        
        try {
            // Find student
            Optional<User> studentOpt = userRepository.findByUsername(username);
            if (studentOpt.isEmpty()) {
                logger.warn("Purchase failed - Student not found: {}", username);
                return ResponseEntity.badRequest().body("Student not found");
            }
            
            User student = studentOpt.get();
            
            // Find course
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                logger.warn("Purchase failed - Course not found: {}", courseId);
                return ResponseEntity.badRequest().body("Course not found");
            }
            
            Course course = courseOpt.get();
            
            // Validate course is public and paid
            if (course.getVisibility() != CourseVisibility.PUBLIC) {
                return ResponseEntity.badRequest().body("Cannot purchase private courses");
            }
            
            if (course.getPricing() != CoursePricing.PAID) {
                return ResponseEntity.badRequest().body("This course is free, use enrollment endpoint instead");
            }
            
            // Create payment order
            PaymentService.PaymentOrderResponse paymentOrder = paymentService.createPaymentOrder(course, student);
            
            logger.info("Payment order created for student: {} course: {} order: {}", 
                       username, course.getName(), paymentOrder.getOrderId());
            
            return ResponseEntity.ok(paymentOrder);
            
        } catch (Exception e) {
            logger.error("Error initiating purchase for student: {} course: {}", username, courseId, e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/payment/verify")
    @Transactional
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Payment verification request for order: {} by user: {}", request.getOrderId(), username);
        
        try {
            // Validate request data
            if (request == null) {
                throw new ValidationException("Payment verification request is required");
            }
            
            if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
                throw new ValidationException("orderId", "Order ID is required");
            }
            
            if (request.getPaymentId() == null || request.getPaymentId().trim().isEmpty()) {
                throw new ValidationException("paymentId", "Payment ID is required");
            }
            
            if (request.getSignature() == null || request.getSignature().trim().isEmpty()) {
                throw new ValidationException("signature", "Payment signature is required");
            }
            
            if (request.getCourseId() == null) {
                throw new ValidationException("courseId", "Course ID is required");
            }
            
            // Get student from authentication context
            Optional<User> studentOpt = userRepository.findByUsername(username);
            if (studentOpt.isEmpty()) {
                throw new ValidationException("authentication", "Student not found");
            }
            
            User student = studentOpt.get();
            
            // Find and validate course
            Optional<Course> courseOpt = courseRepository.findById(request.getCourseId());
            if (courseOpt.isEmpty()) {
                throw new CourseException.CourseNotFoundException(request.getCourseId());
            }
            
            Course course = courseOpt.get();
            
            // Validate enrollment eligibility
            validationService.validateEnrollmentEligibility(course, student);
            
            // Verify course is paid
            if (course.getPricing() != CoursePricing.PAID || course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CourseException.CourseNotAvailableForPurchaseException("Course is not available for purchase");
            }
            
            // Verify payment signature
            boolean isValid = paymentService.verifyPaymentSignature(
                request.getOrderId(), 
                request.getPaymentId(), 
                request.getSignature()
            );
            
            if (!isValid) {
                logger.warn("Payment verification failed for order: {}", request.getOrderId());
                throw new PaymentException("Payment signature verification failed", true);
            }
            
            // Process successful payment
            paymentService.processSuccessfulPayment(
                request.getPaymentId(),
                request.getOrderId(),
                request.getCourseId(),
                student.getId()
            );
            
            logger.info("Payment verified and processed successfully for order: {}", request.getOrderId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified and enrollment completed successfully");
            response.put("orderId", request.getOrderId());
            response.put("paymentId", request.getPaymentId());
            response.put("courseId", request.getCourseId());
            response.put("courseName", course.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (ValidationException e) {
            logger.warn("Payment validation failed for order: {} - {}", request != null ? request.getOrderId() : "unknown", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            if (e.hasFieldErrors()) {
                errorResponse.put("fieldErrors", e.getFieldErrors());
            }
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (CourseException e) {
            logger.warn("Course error during payment verification for order: {} - {}", request != null ? request.getOrderId() : "unknown", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (PaymentException e) {
            logger.error("Payment error during verification for order: {} - {}", request != null ? request.getOrderId() : "unknown", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("retryable", e.isRetryable());
            return ResponseEntity.status(422).body(errorResponse); // 422 Unprocessable Entity for payment issues
        } catch (Exception e) {
            logger.error("Unexpected error verifying payment for order: {}", request != null ? request.getOrderId() : "unknown", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Payment verification failed: " + e.getMessage()));
        }
    }

    @GetMapping("/student/enrolled")
    public ResponseEntity<?> getEnrolledCourses(Authentication authentication) {
        String username = authentication.getName();
        logger.info("Fetching enrolled courses for student: {}", username);
        
        try {
            Optional<User> studentOpt = userRepository.findByUsername(username);
            if (studentOpt.isEmpty()) {
                logger.warn("Get enrolled courses failed - Student not found: {}", username);
                return ResponseEntity.badRequest().body("Student not found");
            }
            
            User student = studentOpt.get();
            List<Enrollment> enrollments = enrollmentRepository.findByStudentAndStatus(student, EnrollmentStatus.ENROLLED);
            
            List<Map<String, Object>> enrolledCourses = new ArrayList<>();
            for (Enrollment enrollment : enrollments) {
                Course course = enrollment.getCourse();
                Map<String, Object> courseData = new HashMap<>();
                courseData.put("id", course.getId());
                courseData.put("name", course.getName());
                courseData.put("description", course.getDescription());
                courseData.put("visibility", course.getVisibility());
                courseData.put("pricing", course.getPricing());
                courseData.put("enrollmentDate", enrollment.getEnrollmentDate());
                courseData.put("instructor", Map.of(
                    "username", course.getInstructor().getUsername(),
                    "fullName", course.getInstructor().getFullName() != null ? 
                        course.getInstructor().getFullName() : course.getInstructor().getUsername()
                ));
                
                enrolledCourses.add(courseData);
            }
            
            logger.info("Retrieved {} enrolled courses for student: {}", enrolledCourses.size(), username);
            return ResponseEntity.ok(enrolledCourses);
            
        } catch (Exception e) {
            logger.error("Error fetching enrolled courses for student: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching enrolled courses");
        }
    }
    @GetMapping("/{courseId}/access")
    public ResponseEntity<?> checkCourseAccess(@PathVariable Long courseId, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Checking course access for course: {} by user: {}", courseId, username);
        
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            User user = userOpt.get();
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Course course = courseOpt.get();
            
            // Check access based on course visibility and user enrollment
            boolean hasAccess = false;
            String accessType = "none";
            
            if (course.getVisibility() == CourseVisibility.PUBLIC) {
                // For public courses, check if user is enrolled
                Optional<Enrollment> enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(user.getId(), courseId);
                if (enrollment.isPresent() && enrollment.get().getStatus() == EnrollmentStatus.ENROLLED) {
                    hasAccess = true;
                    accessType = "enrolled";
                }
            } else if (course.getVisibility() == CourseVisibility.PRIVATE) {
                // For private courses, check if user's email is in allowed emails
                if (course.getAllowedEmails() != null && course.getAllowedEmails().contains(user.getEmail())) {
                    hasAccess = true;
                    accessType = "allowed";
                    
                    // Also check if they're enrolled
                    Optional<Enrollment> enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(user.getId(), courseId);
                    if (enrollment.isPresent() && enrollment.get().getStatus() == EnrollmentStatus.ENROLLED) {
                        accessType = "enrolled";
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasAccess", hasAccess);
            response.put("accessType", accessType);
            response.put("courseId", courseId);
            response.put("courseName", course.getName());
            response.put("visibility", course.getVisibility());
            response.put("pricing", course.getPricing());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking course access for course: {} user: {}", courseId, username, e);
            return ResponseEntity.internalServerError().body("Error checking course access");
        }
    }

    @PutMapping("/{courseId}")
    @Transactional
    public ResponseEntity<?> updateCourse(@PathVariable Long courseId,
                                          @RequestParam("name") String name,
                                          @RequestParam(value = "description", required = false) String description,
                                          @RequestParam("visibility") String visibilityStr,
                                          @RequestParam("pricing") String pricingStr,
                                          @RequestParam(value = "price", required = false) String priceStr,
                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                          Authentication authentication) {
        
        String username = authentication.getName();
        logger.info("Course update request by instructor: {} for course: {}", username, courseId);
        
        try {
            // Find instructor
            Optional<User> instructorOpt = userRepository.findByUsername(username);
            if (instructorOpt.isEmpty()) {
                logger.warn("Course update failed - Instructor not found: {}", username);
                return ResponseEntity.badRequest().body(Map.of("error", "Instructor not found"));
            }
            
            User instructor = instructorOpt.get();
            
            // Find course
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                logger.warn("Course update failed - Course not found: {}", courseId);
                return ResponseEntity.badRequest().body(Map.of("error", "Course not found"));
            }
            
            Course course = courseOpt.get();
            
            // Check if instructor owns this course
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                logger.warn("Course update failed - Instructor {} does not own course {}", username, courseId);
                return ResponseEntity.badRequest().body(Map.of("error", "You can only edit your own courses"));
            }
            
            // Parse and validate visibility
            CourseVisibility visibility;
            try {
                visibility = CourseVisibility.valueOf(visibilityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid visibility value. Must be PRIVATE or PUBLIC"));
            }
            
            // Parse and validate pricing
            CoursePricing pricing;
            try {
                pricing = CoursePricing.valueOf(pricingStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid pricing value. Must be FREE or PAID"));
            }
            
            // Parse and validate price
            BigDecimal price = null;
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                try {
                    price = new BigDecimal(priceStr);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid price format"));
                }
            }
            
            // Process allowed emails for private courses
            List<String> allowedEmails = course.getAllowedEmails(); // Keep existing if no new file
            if (visibility == CourseVisibility.PRIVATE && file != null && !file.isEmpty()) {
                logger.debug("Processing new email file for private course: {}", name);
                allowedEmails = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int lineNum = 1;
                    while ((line = reader.readLine()) != null) {
                        String email = line.trim();
                        if (!email.isEmpty()) {
                            if (email.length() > 100) {
                                logger.warn("Email validation failed - Email too long at line {}: {}", lineNum, email);
                                return ResponseEntity.badRequest().body(Map.of("error", "Email too long at line " + lineNum + ": " + email));
                            }
                            if (!validationService.isValidEmail(email)) {
                                logger.warn("Email validation failed - Invalid format at line {}: {}", lineNum, email);
                                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format at line " + lineNum + ": " + email));
                            }
                            allowedEmails.add(email);
                        }
                        lineNum++;
                    }
                }
                logger.debug("Processed {} valid emails for private course update: {}", allowedEmails.size(), name);
            }
            
            // Update course fields
            course.setName(name);
            course.setDescription(description);
            course.setVisibility(visibility);
            course.setPricing(pricing);
            course.setPrice(price);
            if (visibility == CourseVisibility.PRIVATE) {
                course.setAllowedEmails(allowedEmails);
            } else {
                course.setAllowedEmails(null); // Clear allowed emails for public courses
            }
            
            // Validate updated course
            Course originalCourse = courseOpt.get(); // Get the original course for comparison
            validationService.validateCourseUpdate(originalCourse, course, instructor);
            
            // Save updated course
            Course updatedCourse = courseRepository.save(course);
            
            // Auto-enroll new students for private courses if new emails were added
            int newAutoEnrolledCount = 0;
            if (visibility == CourseVisibility.PRIVATE && allowedEmails != null && !allowedEmails.isEmpty()) {
                logger.info("Auto-enrolling new students in updated private course: {}", name);
                for (String email : allowedEmails) {
                    try {
                        Optional<User> studentOpt = userRepository.findByEmail(email);
                        if (studentOpt.isPresent()) {
                            User student = studentOpt.get();
                            
                            // Check if student is not already enrolled
                            Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(student.getId(), updatedCourse.getId());
                            if (existingEnrollment.isEmpty()) {
                                Enrollment enrollment = new Enrollment(student, updatedCourse, EnrollmentStatus.ENROLLED);
                                enrollmentRepository.save(enrollment);
                                newAutoEnrolledCount++;
                                logger.debug("Auto-enrolled new student: {} in updated private course: {}", email, name);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to auto-enroll student with email: {} in updated course: {} - {}", email, name, e.getMessage());
                    }
                }
                if (newAutoEnrolledCount > 0) {
                    logger.info("Auto-enrolled {} new students in updated private course: {}", newAutoEnrolledCount, name);
                }
            }
            
            logger.info("Course updated successfully: {} by instructor: {}", name, username);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedCourse.getId());
            response.put("name", updatedCourse.getName());
            response.put("description", updatedCourse.getDescription());
            response.put("visibility", updatedCourse.getVisibility());
            response.put("pricing", updatedCourse.getPricing());
            response.put("price", updatedCourse.getPrice());
            response.put("instructor", Map.of(
                "id", instructor.getId(),
                "username", instructor.getUsername(),
                "fullName", instructor.getFullName() != null ? instructor.getFullName() : instructor.getUsername()
            ));
            response.put("message", "Course updated successfully");
            if (newAutoEnrolledCount > 0) {
                response.put("newAutoEnrolledStudents", newAutoEnrolledCount);
                response.put("enrollmentMessage", "Automatically enrolled " + newAutoEnrolledCount + " new students");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (ValidationException e) {
            logger.warn("Course update validation failed for instructor: {} - {}", username, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            if (e.hasFieldErrors()) {
                errorResponse.put("fieldErrors", e.getFieldErrors());
            }
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (CourseException e) {
            logger.warn("Course update failed for instructor: {} - {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating course: {} by instructor: {}", courseId, username, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{courseId}")
    @Transactional
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Course deletion request by instructor: {} for course: {}", username, courseId);
        
        try {
            // Find instructor
            Optional<User> instructorOpt = userRepository.findByUsername(username);
            if (instructorOpt.isEmpty()) {
                logger.warn("Course deletion failed - Instructor not found: {}", username);
                return ResponseEntity.badRequest().body(Map.of("error", "Instructor not found"));
            }
            
            User instructor = instructorOpt.get();
            
            // Find course
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                logger.warn("Course deletion failed - Course not found: {}", courseId);
                return ResponseEntity.badRequest().body(Map.of("error", "Course not found"));
            }
            
            Course course = courseOpt.get();
            
            // Check if instructor owns this course
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                logger.warn("Course deletion failed - Instructor {} does not own course {}", username, courseId);
                return ResponseEntity.badRequest().body(Map.of("error", "You can only delete your own courses"));
            }
            
            // Check if course has any exams
            if (course.getExams() != null && !course.getExams().isEmpty()) {
                logger.warn("Course deletion failed - Course {} has {} exams", courseId, course.getExams().size());
                return ResponseEntity.status(409).body(Map.of(
                    "error", "Cannot delete course that has exams. Please delete all exams first.",
                    "examCount", course.getExams().size()
                ));
            }
            
            // Check if course has any enrollments using EnrollmentRepository
            List<com.ExamPort.ExamPort.Entity.Enrollment> enrollments = enrollmentRepository.findByCourse_Id(courseId);
            if (!enrollments.isEmpty()) {
                logger.warn("Course deletion failed - Course {} has {} enrollments", courseId, enrollments.size());
                return ResponseEntity.status(409).body(Map.of(
                    "error", "Cannot delete course that has student enrollments. Please remove all enrollments first.",
                    "enrollmentCount", enrollments.size()
                ));
            }
            
            String courseName = course.getName();
            
            try {
                // Delete the course
                courseRepository.delete(course);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                logger.error("Data integrity violation when deleting course: {}", courseId, e);
                return ResponseEntity.status(409).body(Map.of(
                    "error", "Cannot delete course due to existing dependencies. Please ensure all related exams and enrollments are removed first."
                ));
            }
            
            logger.info("Course deleted successfully: {} (ID: {}) by instructor: {}", courseName, courseId, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Course deleted successfully");
            response.put("deletedCourseId", courseId);
            response.put("deletedCourseName", courseName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting course: {} by instructor: {}", courseId, username, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{courseId}/unenroll-all")
    @Transactional
    public ResponseEntity<?> unenrollAllStudents(@PathVariable Long courseId, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Unenroll all students request by instructor: {} for course: {}", username, courseId);
        
        try {
            // Find instructor
            Optional<User> instructorOpt = userRepository.findByUsername(username);
            if (instructorOpt.isEmpty()) {
                logger.warn("Unenroll all failed - Instructor not found: {}", username);
                return ResponseEntity.badRequest().body(Map.of("error", "Instructor not found"));
            }
            
            User instructor = instructorOpt.get();
            
            // Find course
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                logger.warn("Unenroll all failed - Course not found: {}", courseId);
                return ResponseEntity.badRequest().body(Map.of("error", "Course not found"));
            }
            
            Course course = courseOpt.get();
            
            // Check if instructor owns this course
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                logger.warn("Unenroll all failed - Instructor {} does not own course {}", username, courseId);
                return ResponseEntity.badRequest().body(Map.of("error", "You can only manage enrollments for your own courses"));
            }
            
            // Get all enrollments for this course
            List<com.ExamPort.ExamPort.Entity.Enrollment> enrollments = enrollmentRepository.findByCourse_Id(courseId);
            
            if (enrollments.isEmpty()) {
                logger.info("No enrollments found for course: {}", courseId);
                return ResponseEntity.ok(Map.of(
                    "message", "No students are currently enrolled in this course",
                    "unenrolledCount", 0
                ));
            }
            
            // Delete all enrollments
            enrollmentRepository.deleteAll(enrollments);
            
            logger.info("Successfully unenrolled {} students from course: {} by instructor: {}", 
                       enrollments.size(), courseId, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All students have been unenrolled successfully");
            response.put("unenrolledCount", enrollments.size());
            response.put("courseName", course.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error unenrolling all students from course: {} by instructor: {}", courseId, username, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
}