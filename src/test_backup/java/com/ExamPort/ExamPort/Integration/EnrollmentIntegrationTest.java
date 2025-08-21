package com.ExamPort.ExamPort.Integration;

import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.Enrollment;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import com.ExamPort.ExamPort.Repository.EnrollmentRepository;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class EnrollmentIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testStudent;
    private User testInstructor;
    private Course testCourse;
    private String studentToken;
    private String instructorToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();

        // Create test users
        testStudent = new User();
        testStudent.setEmail("student@test.com");
        testStudent.setFullName("Test Student");
        testStudent.setRole("STUDENT");
        testStudent.setPassword("password");
        testStudent.setCreatedAt(LocalDateTime.now());
        testStudent.setUpdatedAt(LocalDateTime.now());
        testStudent = userRepository.save(testStudent);

        testInstructor = new User();
        testInstructor.setEmail("instructor@test.com");
        testInstructor.setFullName("Test Instructor");
        testInstructor.setRole("INSTRUCTOR");
        testInstructor.setPassword("password");
        testInstructor.setCreatedAt(LocalDateTime.now());
        testInstructor.setUpdatedAt(LocalDateTime.now());
        testInstructor = userRepository.save(testInstructor);

        // Create test course
        testCourse = new Course();
        testCourse.setName("Test Course");
        testCourse.setDescription("Test Description");
        testCourse.setInstructor(testInstructor);
        testCourse.setVisibility("PUBLIC");
        testCourse.setPricing("FREE");
        testCourse.setCreatedAt(LocalDateTime.now());
        testCourse.setUpdatedAt(LocalDateTime.now());
        testCourse = courseRepository.save(testCourse);

        // Generate tokens
        studentToken = jwtService.generateToken(testStudent.getEmail());
        instructorToken = jwtService.generateToken(testInstructor.getEmail());
    }

    @Test
    void testCompleteEnrollmentFlow() throws Exception {
        // 1. Check initial enrollment status (should be false)
        mockMvc.perform(get("/api/enrollments/check/" + testCourse.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnrolled").value(false))
                .andExpect(jsonPath("$.enrollment").isEmpty());

        // 2. Enroll in free course
        mockMvc.perform(post("/api/courses/" + testCourse.getId() + "/enroll")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully enrolled in the course"))
                .andExpect(jsonPath("$.enrollmentId").exists());

        // 3. Check enrollment status after enrollment (should be true)
        mockMvc.perform(get("/api/enrollments/check/" + testCourse.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnrolled").value(true))
                .andExpect(jsonPath("$.enrollment.id").exists())
                .andExpect(jsonPath("$.enrollment.status").value("ENROLLED"));

        // 4. Get student's enrollments
        mockMvc.perform(get("/api/enrollments/my-enrollments")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.enrollments").isArray())
                .andExpect(jsonPath("$.enrollments", hasSize(1)))
                .andExpect(jsonPath("$.enrollments[0].course.name").value("Test Course"))
                .andExpect(jsonPath("$.totalItems").value(1));

        // 5. Try to enroll again (should fail)
        mockMvc.perform(post("/api/courses/" + testCourse.getId() + "/enroll")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Already enrolled in this course"));
    }

    @Test
    void testPaidCourseEnrollmentFlow() throws Exception {
        // Update course to be paid
        testCourse.setPricing("PAID");
        testCourse.setPrice(1000.0);
        courseRepository.save(testCourse);

        // 1. Try to enroll directly in paid course (should fail)
        mockMvc.perform(post("/api/courses/" + testCourse.getId() + "/enroll")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("This is a paid course. Please use the payment flow."));

        // 2. Initiate purchase
        mockMvc.perform(post("/api/courses/" + testCourse.getId() + "/purchase")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.amount").value(100000)) // 1000 * 100 paise
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.courseName").value("Test Course"));

        // 3. Verify payment and create enrollment
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("razorpay_order_id", "order_test123");
        paymentData.put("razorpay_payment_id", "pay_test123");
        paymentData.put("razorpay_signature", "signature_test123");
        paymentData.put("courseId", testCourse.getId());

        mockMvc.perform(post("/api/courses/payment/verify")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment verified and enrollment created successfully"))
                .andExpect(jsonPath("$.enrollmentId").exists())
                .andExpect(jsonPath("$.paymentTransactionId").value("pay_test123"));

        // 4. Verify enrollment was created
        mockMvc.perform(get("/api/enrollments/check/" + testCourse.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnrolled").value(true))
                .andExpect(jsonPath("$.enrollment.paymentTransactionId").value("pay_test123"));
    }

    @Test
    void testInstructorEnrollmentStats() throws Exception {
        // Create multiple courses for instructor
        Course course2 = new Course();
        course2.setName("Test Course 2");
        course2.setDescription("Test Description 2");
        course2.setInstructor(testInstructor);
        course2.setVisibility("PUBLIC");
        course2.setPricing("PAID");
        course2.setPrice(2000.0);
        course2.setCreatedAt(LocalDateTime.now());
        course2.setUpdatedAt(LocalDateTime.now());
        course2 = courseRepository.save(course2);

        // Create enrollments
        Enrollment enrollment1 = new Enrollment();
        enrollment1.setStudent(testStudent);
        enrollment1.setCourse(testCourse);
        enrollment1.setStatus("ENROLLED");
        enrollment1.setEnrollmentDate(LocalDateTime.now());
        enrollment1.setCreatedAt(LocalDateTime.now());
        enrollment1.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment1);

        // Create another student and enrollment
        User student2 = new User();
        student2.setEmail("student2@test.com");
        student2.setFullName("Test Student 2");
        student2.setRole("STUDENT");
        student2.setPassword("password");
        student2.setCreatedAt(LocalDateTime.now());
        student2.setUpdatedAt(LocalDateTime.now());
        student2 = userRepository.save(student2);

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudent(student2);
        enrollment2.setCourse(course2);
        enrollment2.setStatus("ENROLLED");
        enrollment2.setEnrollmentDate(LocalDateTime.now());
        enrollment2.setPaymentTransactionId("pay_test456");
        enrollment2.setCreatedAt(LocalDateTime.now());
        enrollment2.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment2);

        // Get instructor stats
        mockMvc.perform(get("/api/enrollments/instructor/stats")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses").value(2))
                .andExpect(jsonPath("$.totalEnrollments").value(2))
                .andExpect(jsonPath("$.totalRevenue").value(2000)) // Only paid course revenue
                .andExpect(jsonPath("$.courseStats").exists());
    }

    @Test
    void testCancelFreeEnrollment() throws Exception {
        // Create enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(testStudent);
        enrollment.setCourse(testCourse);
        enrollment.setStatus("ENROLLED");
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        // Cancel enrollment
        mockMvc.perform(delete("/api/enrollments/" + enrollment.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Enrollment cancelled successfully"));

        // Verify enrollment status changed
        mockMvc.perform(get("/api/enrollments/" + enrollment.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testCannotCancelPaidEnrollment() throws Exception {
        // Update course to be paid
        testCourse.setPricing("PAID");
        testCourse.setPrice(1000.0);
        courseRepository.save(testCourse);

        // Create paid enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(testStudent);
        enrollment.setCourse(testCourse);
        enrollment.setStatus("ENROLLED");
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setPaymentTransactionId("pay_test123");
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        // Try to cancel paid enrollment (should fail)
        mockMvc.perform(delete("/api/enrollments/" + enrollment.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot cancel enrollment for paid courses"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // Try to access enrollments without token
        mockMvc.perform(get("/api/enrollments/my-enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));

        // Try to access with invalid token
        mockMvc.perform(get("/api/enrollments/my-enrollments")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessControlForEnrollmentDetails() throws Exception {
        // Create enrollment for test student
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(testStudent);
        enrollment.setCourse(testCourse);
        enrollment.setStatus("ENROLLED");
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        // Create another student
        User otherStudent = new User();
        otherStudent.setEmail("other@test.com");
        otherStudent.setFullName("Other Student");
        otherStudent.setRole("STUDENT");
        otherStudent.setPassword("password");
        otherStudent.setCreatedAt(LocalDateTime.now());
        otherStudent.setUpdatedAt(LocalDateTime.now());
        otherStudent = userRepository.save(otherStudent);

        String otherStudentToken = jwtService.generateToken(otherStudent.getEmail());

        // Try to access enrollment details with different student (should fail)
        mockMvc.perform(get("/api/enrollments/" + enrollment.getId())
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));

        // Access with correct student (should succeed)
        mockMvc.perform(get("/api/enrollments/" + enrollment.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(enrollment.getId()));
    }
}