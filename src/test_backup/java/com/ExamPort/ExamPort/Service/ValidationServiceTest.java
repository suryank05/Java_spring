package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Exception.ValidationException;
import com.ExamPort.ExamPort.Exception.CourseException;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ValidationService validationService;

    private User instructor;
    private User student;
    private Course validCourse;

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

        validCourse = new Course();
        validCourse.setId(1L);
        validCourse.setName("Test Course");
        validCourse.setDescription("This is a test course description that meets minimum length requirements");
        validCourse.setVisibility("PUBLIC");
        validCourse.setPricing("FREE");
        validCourse.setInstructor(instructor);
    }

    @Test
    void validateCourseCreation_ValidCourse_ShouldPass() {
        // Given
        when(courseRepository.existsByNameAndInstructorId(anyString(), anyLong())).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> validationService.validateCourseCreation(validCourse, instructor));
    }

    @Test
    void validateCourseCreation_NullInstructor_ShouldThrowException() {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, null));
        
        assertEquals("Instructor is required", exception.getMessage());
    }

    @Test
    void validateCourseCreation_NonInstructorRole_ShouldThrowException() {
        // Given
        instructor.setRole("STUDENT");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("instructor"));
        assertEquals("Only instructors can create courses", exception.getFieldErrors().get("instructor"));
    }

    @Test
    void validateCourseCreation_EmptyCourseName_ShouldThrowException() {
        // Given
        validCourse.setName("");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("name"));
        assertEquals("Course name is required", exception.getFieldErrors().get("name"));
    }

    @Test
    void validateCourseCreation_CourseNameTooShort_ShouldThrowException() {
        // Given
        validCourse.setName("AB");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("name"));
        assertTrue(exception.getFieldErrors().get("name").contains("at least 3 characters"));
    }

    @Test
    void validateCourseCreation_CourseNameTooLong_ShouldThrowException() {
        // Given
        validCourse.setName("A".repeat(101));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("name"));
        assertTrue(exception.getFieldErrors().get("name").contains("cannot exceed 100 characters"));
    }

    @Test
    void validateCourseCreation_DuplicateCourseName_ShouldThrowException() {
        // Given
        when(courseRepository.existsByNameAndInstructorId(validCourse.getName(), instructor.getId()))
            .thenReturn(true);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("name"));
        assertEquals("You already have a course with this name", exception.getFieldErrors().get("name"));
    }

    @Test
    void validateCourseCreation_EmptyDescription_ShouldThrowException() {
        // Given
        validCourse.setDescription("");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("description"));
        assertEquals("Course description is required", exception.getFieldErrors().get("description"));
    }

    @Test
    void validateCourseCreation_DescriptionTooShort_ShouldThrowException() {
        // Given
        validCourse.setDescription("Short");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("description"));
        assertTrue(exception.getFieldErrors().get("description").contains("at least 10 characters"));
    }

    @Test
    void validateCourseCreation_InvalidVisibility_ShouldThrowException() {
        // Given
        validCourse.setVisibility("INVALID");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("visibility"));
        assertTrue(exception.getFieldErrors().get("visibility").contains("PUBLIC or PRIVATE"));
    }

    @Test
    void validateCourseCreation_InvalidPricing_ShouldThrowException() {
        // Given
        validCourse.setPricing("INVALID");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("pricing"));
        assertTrue(exception.getFieldErrors().get("pricing").contains("FREE or PAID"));
    }

    @Test
    void validateCourseCreation_PaidCourseWithoutPrice_ShouldThrowException() {
        // Given
        validCourse.setPricing("PAID");
        validCourse.setPrice(null);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("price"));
        assertEquals("Price is required for paid courses", exception.getFieldErrors().get("price"));
    }

    @Test
    void validateCourseCreation_PaidCourseWithValidPrice_ShouldPass() {
        // Given
        validCourse.setPricing("PAID");
        validCourse.setPrice(new BigDecimal("99.99"));
        when(courseRepository.existsByNameAndInstructorId(anyString(), anyLong())).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> validationService.validateCourseCreation(validCourse, instructor));
    }

    @Test
    void validateCourseCreation_PriceTooLow_ShouldThrowException() {
        // Given
        validCourse.setPricing("PAID");
        validCourse.setPrice(new BigDecimal("0.50"));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("price"));
        assertTrue(exception.getFieldErrors().get("price").contains("at least ₹1.00"));
    }

    @Test
    void validateCourseCreation_PriceTooHigh_ShouldThrowException() {
        // Given
        validCourse.setPricing("PAID");
        validCourse.setPrice(new BigDecimal("100001.00"));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("price"));
        assertTrue(exception.getFieldErrors().get("price").contains("cannot exceed ₹100,000"));
    }

    @Test
    void validateCourseCreation_FreeCourseWithPrice_ShouldThrowException() {
        // Given
        validCourse.setPricing("FREE");
        validCourse.setPrice(new BigDecimal("10.00"));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("price"));
        assertEquals("Free courses cannot have a price", exception.getFieldErrors().get("price"));
    }

    @Test
    void validateCourseCreation_PrivateCourseWithoutAllowedEmails_ShouldThrowException() {
        // Given
        validCourse.setVisibility("PRIVATE");
        validCourse.setAllowedEmails(null);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("allowedEmails"));
        assertEquals("Allowed emails are required for private courses", exception.getFieldErrors().get("allowedEmails"));
    }

    @Test
    void validateCourseCreation_PrivateCourseWithInvalidEmail_ShouldThrowException() {
        // Given
        validCourse.setVisibility("PRIVATE");
        validCourse.setAllowedEmails("valid@example.com,invalid-email");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateCourseCreation(validCourse, instructor));
        
        assertTrue(exception.getFieldErrors().containsKey("allowedEmails"));
        assertTrue(exception.getFieldErrors().get("allowedEmails").contains("Invalid email format"));
    }

    @Test
    void validateEnrollmentEligibility_ValidEnrollment_ShouldPass() {
        // When & Then
        assertDoesNotThrow(() -> validationService.validateEnrollmentEligibility(validCourse, student));
    }

    @Test
    void validateEnrollmentEligibility_NullCourse_ShouldThrowException() {
        // When & Then
        assertThrows(CourseException.CourseNotFoundException.class, 
            () -> validationService.validateEnrollmentEligibility(null, student));
    }

    @Test
    void validateEnrollmentEligibility_NullStudent_ShouldThrowException() {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateEnrollmentEligibility(validCourse, null));
        
        assertTrue(exception.getFieldErrors().containsKey("student"));
        assertEquals("Student information is required", exception.getFieldErrors().get("student"));
    }

    @Test
    void validateEnrollmentEligibility_NonStudentRole_ShouldThrowException() {
        // Given
        student.setRole("INSTRUCTOR");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validateEnrollmentEligibility(validCourse, student));
        
        assertTrue(exception.getFieldErrors().containsKey("student"));
        assertEquals("Only students can enroll in courses", exception.getFieldErrors().get("student"));
    }

    @Test
    void validateEnrollmentEligibility_PrivateCourseWithoutAccess_ShouldThrowException() {
        // Given
        validCourse.setVisibility("PRIVATE");
        validCourse.setAllowedEmails("other@example.com");

        // When & Then
        CourseException.CourseAccessDeniedException exception = assertThrows(
            CourseException.CourseAccessDeniedException.class, 
            () -> validationService.validateEnrollmentEligibility(validCourse, student));
        
        assertEquals("You don't have access to this private course", exception.getMessage());
    }

    @Test
    void validateEnrollmentEligibility_PrivateCourseWithAccess_ShouldPass() {
        // Given
        validCourse.setVisibility("PRIVATE");
        validCourse.setAllowedEmails("student@example.com,other@example.com");

        // When & Then
        assertDoesNotThrow(() -> validationService.validateEnrollmentEligibility(validCourse, student));
    }

    @Test
    void validatePaymentData_ValidData_ShouldPass() {
        // Given
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("razorpay_order_id", "order_123");
        paymentData.put("razorpay_payment_id", "pay_123");
        paymentData.put("razorpay_signature", "signature_123");
        paymentData.put("courseId", "1");

        // When & Then
        assertDoesNotThrow(() -> validationService.validatePaymentData(paymentData));
    }

    @Test
    void validatePaymentData_NullData_ShouldThrowException() {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validatePaymentData(null));
        
        assertEquals("Payment data is required", exception.getMessage());
    }

    @Test
    void validatePaymentData_EmptyData_ShouldThrowException() {
        // Given
        Map<String, Object> paymentData = new HashMap<>();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validatePaymentData(paymentData));
        
        assertEquals("Payment data is required", exception.getMessage());
    }

    @Test
    void validatePaymentData_MissingRequiredFields_ShouldThrowException() {
        // Given
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("razorpay_order_id", "order_123");
        // Missing other required fields

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validatePaymentData(paymentData));
        
        assertTrue(exception.hasFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("razorpay_payment_id"));
        assertTrue(exception.getFieldErrors().containsKey("razorpay_signature"));
        assertTrue(exception.getFieldErrors().containsKey("courseId"));
    }

    @Test
    void validatePaymentData_InvalidCourseId_ShouldThrowException() {
        // Given
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("razorpay_order_id", "order_123");
        paymentData.put("razorpay_payment_id", "pay_123");
        paymentData.put("razorpay_signature", "signature_123");
        paymentData.put("courseId", "invalid");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validationService.validatePaymentData(paymentData));
        
        assertTrue(exception.getFieldErrors().containsKey("courseId"));
        assertEquals("Invalid course ID format", exception.getFieldErrors().get("courseId"));
    }

    @Test
    void isValidEmail_ValidEmail_ShouldReturnTrue() {
        assertTrue(validationService.isValidEmail("test@example.com"));
        assertTrue(validationService.isValidEmail("user.name+tag@domain.co.uk"));
        assertTrue(validationService.isValidEmail("test123@test-domain.com"));
    }

    @Test
    void isValidEmail_InvalidEmail_ShouldReturnFalse() {
        assertFalse(validationService.isValidEmail("invalid-email"));
        assertFalse(validationService.isValidEmail("@domain.com"));
        assertFalse(validationService.isValidEmail("user@"));
        assertFalse(validationService.isValidEmail(""));
        assertFalse(validationService.isValidEmail(null));
    }

    @Test
    void isValidPrice_ValidPrice_ShouldReturnTrue() {
        assertTrue(validationService.isValidPrice(new BigDecimal("10.00")));
        assertTrue(validationService.isValidPrice(new BigDecimal("0.00")));
        assertTrue(validationService.isValidPrice(new BigDecimal("99999.99")));
    }

    @Test
    void isValidPrice_InvalidPrice_ShouldReturnFalse() {
        assertFalse(validationService.isValidPrice(new BigDecimal("-10.00")));
        assertFalse(validationService.isValidPrice(new BigDecimal("100001.00")));
        assertFalse(validationService.isValidPrice(null));
    }
}