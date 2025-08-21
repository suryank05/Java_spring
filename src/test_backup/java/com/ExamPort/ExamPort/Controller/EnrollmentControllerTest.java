package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.Enrollment;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Service.EnrollmentService;
import com.ExamPort.ExamPort.Service.JwtService;
import com.ExamPort.ExamPort.Service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EnrollmentController enrollmentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Course testCourse;
    private Enrollment testEnrollment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(enrollmentController).build();
        objectMapper = new ObjectMapper();

        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("student@test.com");
        testUser.setFullName("Test Student");
        testUser.setRole("STUDENT");

        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setName("Test Course");
        testCourse.setDescription("Test Description");
        testCourse.setPricing("FREE");

        testEnrollment = new Enrollment();
        testEnrollment.setId(1L);
        testEnrollment.setStudent(testUser);
        testEnrollment.setCourse(testCourse);
        testEnrollment.setStatus("ENROLLED");
        testEnrollment.setEnrollmentDate(LocalDateTime.now());
    }

    @Test
    void getMyEnrollments_Success() throws Exception {
        // Arrange
        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);

        List<Enrollment> enrollments = Arrays.asList(testEnrollment);
        Page<Enrollment> enrollmentPage = new PageImpl<>(enrollments, PageRequest.of(0, 10), 1);
        when(enrollmentService.getEnrollmentsByStudent(eq(1L), any(Pageable.class)))
                .thenReturn(enrollmentPage);

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/my-enrollments")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollments").isArray())
                .andExpect(jsonPath("$.enrollments[0].id").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getMyEnrollments_Unauthorized() throws Exception {
        // Arrange
        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/my-enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void getEnrollmentDetails_Success() throws Exception {
        // Arrange
        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);
        when(enrollmentService.getEnrollmentById(1L))
                .thenReturn(testEnrollment);

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ENROLLED"));
    }

    @Test
    void getEnrollmentDetails_NotFound() throws Exception {
        // Arrange
        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);
        when(enrollmentService.getEnrollmentById(999L))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/999")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Enrollment not found"));
    }

    @Test
    void checkEnrollment_IsEnrolled() throws Exception {
        // Arrange
        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);
        when(enrollmentService.isStudentEnrolledInCourse(1L, 1L))
                .thenReturn(true);
        when(enrollmentService.getEnrollmentByStudentAndCourse(1L, 1L))
                .thenReturn(testEnrollment);

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/check/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnrolled").value(true))
                .andExpect(jsonPath("$.enrollment.id").value(1));
    }

    @Test
    void checkEnrollment_NotEnrolled() throws Exception {
        // Arrange
        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);
        when(enrollmentService.isStudentEnrolledInCourse(1L, 1L))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/check/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnrolled").value(false))
                .andExpect(jsonPath("$.enrollment").isEmpty());
    }

    @Test
    void getInstructorEnrollmentStats_Success() throws Exception {
        // Arrange
        User instructor = new User();
        instructor.setId(2L);
        instructor.setEmail("instructor@test.com");
        instructor.setRole("INSTRUCTOR");

        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("instructor@test.com");
        when(userService.findByEmail("instructor@test.com"))
                .thenReturn(instructor);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCourses", 5);
        stats.put("totalEnrollments", 25);
        stats.put("totalRevenue", 50000);

        when(enrollmentService.getInstructorEnrollmentStats(2L))
                .thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/instructor/stats")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses").value(5))
                .andExpect(jsonPath("$.totalEnrollments").value(25))
                .andExpect(jsonPath("$.totalRevenue").value(50000));
    }

    @Test
    void getInstructorEnrollmentStats_AccessDenied() throws Exception {
        // Arrange
        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser); // Student role

        // Act & Assert
        mockMvc.perform(get("/api/enrollments/instructor/stats")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied. Instructor role required."));
    }

    @Test
    void cancelEnrollment_Success() throws Exception {
        // Arrange
        testCourse.setPricing("FREE");
        testEnrollment.setCourse(testCourse);

        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);
        when(enrollmentService.getEnrollmentById(1L))
                .thenReturn(testEnrollment);

        // Act & Assert
        mockMvc.perform(delete("/api/enrollments/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Enrollment cancelled successfully"));

        verify(enrollmentService).cancelEnrollment(1L);
    }

    @Test
    void cancelEnrollment_PaidCourse() throws Exception {
        // Arrange
        testCourse.setPricing("PAID");
        testCourse.setPrice(1000.0);
        testEnrollment.setCourse(testCourse);

        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);
        when(enrollmentService.getEnrollmentById(1L))
                .thenReturn(testEnrollment);

        // Act & Assert
        mockMvc.perform(delete("/api/enrollments/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot cancel enrollment for paid courses"));

        verify(enrollmentService, never()).cancelEnrollment(1L);
    }

    @Test
    void cancelEnrollment_AccessDenied() throws Exception {
        // Arrange
        User otherUser = new User();
        otherUser.setId(999L);
        testEnrollment.setStudent(otherUser);

        when(jwtService.extractTokenFromRequest(any(HttpServletRequest.class)))
                .thenReturn("valid-token");
        when(jwtService.extractUsername("valid-token"))
                .thenReturn("student@test.com");
        when(userService.findByEmail("student@test.com"))
                .thenReturn(testUser);
        when(enrollmentService.getEnrollmentById(1L))
                .thenReturn(testEnrollment);

        // Act & Assert
        mockMvc.perform(delete("/api/enrollments/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));

        verify(enrollmentService, never()).cancelEnrollment(1L);
    }
}