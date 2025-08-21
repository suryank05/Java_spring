package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.Enrollment;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import com.ExamPort.ExamPort.Repository.EnrollmentRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User student;
    private User instructor;
    private Course freeCourse;
    private Course paidCourse;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(1L);
        instructor.setUsername("instructor1");
        instructor.setEmail("instructor@example.com");
        instructor.setRole("INSTRUCTOR");
        instructor.setFullName("Test Instructor");

        student = new User();
        student.setId(2L);
        student.setUsername("student1");
        student.setEmail("student@example.com");
        student.setRole("STUDENT");
        student.setFullName("Test Student");

        freeCourse = new Course();
        freeCourse.setId(1L);
        freeCourse.setName("Free Course");
        freeCourse.setDescription("A free course");
        freeCourse.setVisibility("PUBLIC");
        freeCourse.setPricing("FREE");
        freeCourse.setInstructor(instructor);

        paidCourse = new Course();
        paidCourse.setId(2L);
        paidCourse.setName("Paid Course");
        paidCourse.setDescription("A paid course");
        paidCourse.setVisibility("PUBLIC");
        paidCourse.setPricing("PAID");
        paidCourse.setPrice(new BigDecimal("99.99"));
        paidCourse.setInstructor(instructor);

        enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setCourse(freeCourse);
        enrollment.setStatus("ENROLLED");
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void enrollStudent_ValidEnrollment_ShouldReturnEnrollment() {
        // Given
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), freeCourse.getId()))
            .thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        // When
        Enrollment result = enrollmentService.enrollStudent(student, freeCourse);

        // Then
        assertNotNull(result);
        assertEquals(student, result.getStudent());
        assertEquals(freeCourse, result.getCourse());
        assertEquals("ENROLLED", result.getStatus());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void enrollStudent_AlreadyEnrolled_ShouldThrowException() {
        // Given
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), freeCourse.getId()))
            .thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> enrollmentService.enrollStudent(student, freeCourse));
        
        assertEquals("Student is already enrolled in this course", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enrollStudentWithPayment_ValidEnrollment_ShouldReturnEnrollment() {
        // Given
        String paymentTransactionId = "pay_123456789";
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), paidCourse.getId()))
            .thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        // When
        Enrollment result = enrollmentService.enrollStudentWithPayment(student, paidCourse, paymentTransactionId);

        // Then
        assertNotNull(result);
        assertEquals(student, result.getStudent());
        assertEquals(paidCourse, result.getCourse());
        assertEquals("ENROLLED", result.getStatus());
        verify(enrollmentRepository).save(argThat(e -> 
            e.getPaymentTransactionId().equals(paymentTransactionId)));
    }

    @Test
    void enrollStudentWithPayment_AlreadyEnrolled_ShouldThrowException() {
        // Given
        String paymentTransactionId = "pay_123456789";
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), paidCourse.getId()))
            .thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> enrollmentService.enrollStudentWithPayment(student, paidCourse, paymentTransactionId));
        
        assertEquals("Student is already enrolled in this course", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void isStudentEnrolledInCourse_StudentEnrolled_ShouldReturnTrue() {
        // Given
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), freeCourse.getId()))
            .thenReturn(true);

        // When
        boolean result = enrollmentService.isStudentEnrolledInCourse(student.getId(), freeCourse.getId());

        // Then
        assertTrue(result);
    }

    @Test
    void isStudentEnrolledInCourse_StudentNotEnrolled_ShouldReturnFalse() {
        // Given
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), freeCourse.getId()))
            .thenReturn(false);

        // When
        boolean result = enrollmentService.isStudentEnrolledInCourse(student.getId(), freeCourse.getId());

        // Then
        assertFalse(result);
    }

    @Test
    void getEnrollmentsByStudent_WithPagination_ShouldReturnPagedResults() {
        // Given
        List<Enrollment> enrollments = Arrays.asList(enrollment);
        Page<Enrollment> page = new PageImpl<>(enrollments);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(enrollmentRepository.findByStudentId(student.getId(), pageable)).thenReturn(page);

        // When
        Page<Enrollment> result = enrollmentService.getEnrollmentsByStudent(student.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(enrollment, result.getContent().get(0));
    }

    @Test
    void getEnrollmentsByStudent_WithoutPagination_ShouldReturnList() {
        // Given
        List<Enrollment> enrollments = Arrays.asList(enrollment);
        when(enrollmentRepository.findByStudentId(student.getId())).thenReturn(enrollments);

        // When
        List<Enrollment> result = enrollmentService.getEnrollmentsByStudent(student.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(enrollment, result.get(0));
    }

    @Test
    void getEnrollmentsByCourse_WithPagination_ShouldReturnPagedResults() {
        // Given
        List<Enrollment> enrollments = Arrays.asList(enrollment);
        Page<Enrollment> page = new PageImpl<>(enrollments);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(enrollmentRepository.findByCourseId(freeCourse.getId(), pageable)).thenReturn(page);

        // When
        Page<Enrollment> result = enrollmentService.getEnrollmentsByCourse(freeCourse.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(enrollment, result.getContent().get(0));
    }

    @Test
    void getEnrollmentCountByCourse_ShouldReturnCount() {
        // Given
        long expectedCount = 5L;
        when(enrollmentRepository.countByCourseId(freeCourse.getId())).thenReturn(expectedCount);

        // When
        long result = enrollmentService.getEnrollmentCountByCourse(freeCourse.getId());

        // Then
        assertEquals(expectedCount, result);
    }

    @Test
    void getEnrollmentById_ExistingEnrollment_ShouldReturnEnrollment() {
        // Given
        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        // When
        Enrollment result = enrollmentService.getEnrollmentById(enrollment.getId());

        // Then
        assertNotNull(result);
        assertEquals(enrollment, result);
    }

    @Test
    void getEnrollmentById_NonExistingEnrollment_ShouldReturnNull() {
        // Given
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Enrollment result = enrollmentService.getEnrollmentById(999L);

        // Then
        assertNull(result);
    }

    @Test
    void getEnrollmentByStudentAndCourse_ExistingEnrollment_ShouldReturnEnrollment() {
        // Given
        when(enrollmentRepository.findByStudentIdAndCourseId(student.getId(), freeCourse.getId()))
            .thenReturn(Optional.of(enrollment));

        // When
        Enrollment result = enrollmentService.getEnrollmentByStudentAndCourse(student.getId(), freeCourse.getId());

        // Then
        assertNotNull(result);
        assertEquals(enrollment, result);
    }

    @Test
    void getEnrollmentByStudentAndCourse_NonExistingEnrollment_ShouldReturnNull() {
        // Given
        when(enrollmentRepository.findByStudentIdAndCourseId(student.getId(), freeCourse.getId()))
            .thenReturn(Optional.empty());

        // When
        Enrollment result = enrollmentService.getEnrollmentByStudentAndCourse(student.getId(), freeCourse.getId());

        // Then
        assertNull(result);
    }

    @Test
    void cancelEnrollment_ExistingEnrollment_ShouldUpdateStatus() {
        // Given
        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        // When
        enrollmentService.cancelEnrollment(enrollment.getId());

        // Then
        verify(enrollmentRepository).save(argThat(e -> 
            "CANCELLED".equals(e.getStatus()) && e.getUpdatedAt() != null));
    }

    @Test
    void cancelEnrollment_NonExistingEnrollment_ShouldNotThrowException() {
        // Given
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> enrollmentService.cancelEnrollment(999L));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void updateEnrollmentStatus_ExistingEnrollment_ShouldUpdateStatus() {
        // Given
        String newStatus = "COMPLETED";
        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        // When
        Enrollment result = enrollmentService.updateEnrollmentStatus(enrollment.getId(), newStatus);

        // Then
        assertNotNull(result);
        verify(enrollmentRepository).save(argThat(e -> 
            newStatus.equals(e.getStatus()) && e.getUpdatedAt() != null));
    }

    @Test
    void updateEnrollmentStatus_NonExistingEnrollment_ShouldReturnNull() {
        // Given
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Enrollment result = enrollmentService.updateEnrollmentStatus(999L, "COMPLETED");

        // Then
        assertNull(result);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void getInstructorEnrollmentStats_ShouldReturnStats() {
        // Given
        List<Course> instructorCourses = Arrays.asList(freeCourse, paidCourse);
        when(courseRepository.findByInstructorId(instructor.getId())).thenReturn(instructorCourses);
        when(enrollmentRepository.countByCourseId(freeCourse.getId())).thenReturn(10L);
        when(enrollmentRepository.countByCourseId(paidCourse.getId())).thenReturn(5L);

        // When
        Map<String, Object> result = enrollmentService.getInstructorEnrollmentStats(instructor.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.get("totalCourses"));
        assertEquals(15L, result.get("totalEnrollments"));
        assertEquals(499L, result.get("totalRevenue")); // 5 enrollments * 99.99
        
        @SuppressWarnings("unchecked")
        Map<String, Object> courseStats = (Map<String, Object>) result.get("courseStats");
        assertNotNull(courseStats);
        assertTrue(courseStats.containsKey(freeCourse.getId().toString()));
        assertTrue(courseStats.containsKey(paidCourse.getId().toString()));
    }

    @Test
    void getInstructorEnrollmentStats_NoCoursesOrEnrollments_ShouldReturnZeroStats() {
        // Given
        when(courseRepository.findByInstructorId(instructor.getId())).thenReturn(Arrays.asList());

        // When
        Map<String, Object> result = enrollmentService.getInstructorEnrollmentStats(instructor.getId());

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("totalCourses"));
        assertEquals(0L, result.get("totalEnrollments"));
        assertEquals(0L, result.get("totalRevenue"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> courseStats = (Map<String, Object>) result.get("courseStats");
        assertNotNull(courseStats);
        assertTrue(courseStats.isEmpty());
    }
}