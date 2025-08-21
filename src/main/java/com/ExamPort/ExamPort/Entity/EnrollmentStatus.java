package com.ExamPort.ExamPort.Entity;

/**
 * Enum representing the status of a student's enrollment in a course
 */
public enum EnrollmentStatus {
    /**
     * Student is successfully enrolled and has access to the course
     */
    ENROLLED,
    
    /**
     * Payment is pending for paid courses
     */
    PAYMENT_PENDING,
    
    /**
     * Enrollment has been cancelled by the student or system
     */
    CANCELLED
}