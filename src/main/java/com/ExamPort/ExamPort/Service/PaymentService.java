package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.*;
import com.ExamPort.ExamPort.Repository.EnrollmentRepository;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service for handling Razorpay payment integration
 * Manages payment order creation, verification, and enrollment processing
 */
@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;
    
    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EmailService emailService;
    
    private RazorpayClient razorpayClient;
    
    /**
     * Initialize Razorpay client
     */
    private RazorpayClient getRazorpayClient() throws RazorpayException {
        if (razorpayClient == null) {
            if (razorpayKeyId.isEmpty() || razorpayKeySecret.isEmpty()) {
                throw new RazorpayException("Razorpay credentials not configured");
            }
            razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        }
        return razorpayClient;
    }
    
    /**
     * Create a payment order for course purchase
     * @param course The course to purchase
     * @param student The student making the purchase
     * @return PaymentOrderResponse containing order details
     */
    public PaymentOrderResponse createPaymentOrder(Course course, User student) {
        try {
            logger.info("Creating payment order for course: {} by student: {}", course.getName(), student.getUsername());
            
            // Validate course is paid and public
            if (course.getVisibility() != CourseVisibility.PUBLIC || course.getPricing() != CoursePricing.PAID) {
                throw new IllegalArgumentException("Payment order can only be created for public paid courses");
            }
            
            // Check if student is already enrolled
            Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(student.getId(), course.getId());
            if (existingEnrollment.isPresent() && existingEnrollment.get().getStatus() == EnrollmentStatus.ENROLLED) {
                throw new IllegalArgumentException("Student is already enrolled in this course");
            }
            
            // Create Razorpay order
            RazorpayClient client = getRazorpayClient();
            JSONObject orderRequest = new JSONObject();
            
            // Convert price to paise (Razorpay uses smallest currency unit)
            int amountInPaise = course.getPrice().multiply(new BigDecimal("100")).intValue();
            
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "course_" + course.getId() + "_student_" + student.getId() + "_" + System.currentTimeMillis());
            
            // Add notes for tracking
            JSONObject notes = new JSONObject();
            notes.put("course_id", course.getId());
            notes.put("student_id", student.getId());
            notes.put("course_name", course.getName());
            notes.put("student_email", student.getEmail());
            orderRequest.put("notes", notes);
            
            Order order = client.orders.create(orderRequest);
            
            // Create or update enrollment with payment pending status
            Enrollment enrollment;
            if (existingEnrollment.isPresent()) {
                enrollment = existingEnrollment.get();
                enrollment.setStatus(EnrollmentStatus.PAYMENT_PENDING);
            } else {
                enrollment = new Enrollment(student, course, EnrollmentStatus.PAYMENT_PENDING);
            }
            
            enrollmentRepository.save(enrollment);
            
            logger.info("Payment order created successfully: {} for course: {}", order.get("id"), course.getName());
            
            return new PaymentOrderResponse(
                order.get("id").toString(),
                amountInPaise,
                "INR",
                course.getName(),
                course.getDescription(),
                student.getEmail(),
                student.getFullName() != null ? student.getFullName() : student.getUsername()
            );
            
        } catch (RazorpayException e) {
            logger.error("Error creating payment order for course: {} by student: {}", course.getName(), student.getUsername(), e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage());
        }
    }
    
    /**
     * Verify payment signature from Razorpay
     * @param orderId Razorpay order ID
     * @param paymentId Razorpay payment ID
     * @param signature Razorpay signature
     * @return true if signature is valid
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            logger.info("Verifying payment signature for order: {} payment: {}", orderId, paymentId);
            
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            
            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            
            logger.info("Payment signature verification result: {} for order: {}", isValid, orderId);
            return isValid;
            
        } catch (RazorpayException e) {
            logger.error("Error verifying payment signature for order: {}", orderId, e);
            return false;
        }
    }
    
    /**
     * Process successful payment and complete enrollment
     * @param paymentId Razorpay payment ID
     * @param orderId Razorpay order ID
     * @param courseId Course ID
     * @param studentId Student ID
     */
    @Transactional
    public void processSuccessfulPayment(String paymentId, String orderId, Long courseId, Long studentId) {
        try {
            logger.info("Processing successful payment: {} for course: {} student: {}", paymentId, courseId, studentId);
            
            // Find the enrollment
            Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
            if (enrollmentOpt.isEmpty()) {
                throw new IllegalArgumentException("Enrollment not found for student: " + studentId + " and course: " + courseId);
            }
            
            Enrollment enrollment = enrollmentOpt.get();
            
            // Update enrollment status
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setPaymentTransactionId(paymentId);
            
            enrollmentRepository.save(enrollment);
            
            // Send payment receipt email
            try {
                Course course = enrollment.getCourse();
                User student = enrollment.getStudent();
                logger.info("Attempting to send payment receipt email to: {} for course: {} with transaction: {}", 
                           student.getEmail(), course.getName(), paymentId);
                
                emailService.sendPaymentReceiptEmail(student, course, paymentId, orderId, course.getPrice());
                logger.info("Payment receipt email sent successfully to student: {}", student.getEmail());
            } catch (Exception emailError) {
                logger.error("Failed to send payment receipt email for payment: {} to email: {} - Error: {}", 
                           paymentId, enrollment.getStudent().getEmail(), emailError.getMessage(), emailError);
                // Don't fail the payment processing if email fails
            }
            
            logger.info("Successfully processed payment and enrolled student: {} in course: {}", studentId, courseId);
            
        } catch (Exception e) {
            logger.error("Error processing successful payment: {} for course: {} student: {}", paymentId, courseId, studentId, e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }
    
    /**
     * Get Razorpay public key for frontend
     * @return Razorpay key ID (public key)
     */
    public String getRazorpayKeyId() {
        if (razorpayKeyId.isEmpty()) {
            logger.warn("Razorpay key ID not configured");
            return null;
        }
        return razorpayKeyId;
    }
    
    /**
     * Check if a student has access to a course
     * @param studentId Student ID
     * @param courseId Course ID
     * @return true if student has access
     */
    public boolean hasStudentAccessToCourse(Long studentId, Long courseId) {
        try {
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                return false;
            }
            
            Course course = courseOpt.get();
            
            // For private courses, check allowed emails
            if (course.getVisibility() == CourseVisibility.PRIVATE) {
                Optional<User> studentOpt = userRepository.findById(studentId);
                if (studentOpt.isEmpty()) {
                    return false;
                }
                
                String studentEmail = studentOpt.get().getEmail();
                return course.getAllowedEmails() != null && course.getAllowedEmails().contains(studentEmail);
            }
            
            // For public courses
            if (course.getVisibility() == CourseVisibility.PUBLIC) {
                // Free courses are accessible to all
                if (course.getPricing() == CoursePricing.FREE) {
                    return true;
                }
                
                // Paid courses require enrollment with ENROLLED status
                if (course.getPricing() == CoursePricing.PAID) {
                    return enrollmentRepository.isStudentEnrolledInCourse(studentId, courseId);
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking student access for student: {} course: {}", studentId, courseId, e);
            return false;
        }
    }
    
    /**
     * Response class for payment order creation
     */
    public static class PaymentOrderResponse {
        private String orderId;
        private int amount;
        private String currency;
        private String courseName;
        private String courseDescription;
        private String studentEmail;
        private String studentName;
        
        public PaymentOrderResponse(String orderId, int amount, String currency, String courseName, 
                                  String courseDescription, String studentEmail, String studentName) {
            this.orderId = orderId;
            this.amount = amount;
            this.currency = currency;
            this.courseName = courseName;
            this.courseDescription = courseDescription;
            this.studentEmail = studentEmail;
            this.studentName = studentName;
        }
        
        // Getters
        public String getOrderId() { return orderId; }
        public int getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getCourseName() { return courseName; }
        public String getCourseDescription() { return courseDescription; }
        public String getStudentEmail() { return studentEmail; }
        public String getStudentName() { return studentName; }
    }
}