package com.ExamPort.ExamPort.Exception;

/**
 * Custom exception for enrollment-related errors
 */
public class EnrollmentException extends RuntimeException {
    
    private final String errorCode;
    
    public EnrollmentException(String message) {
        super(message);
        this.errorCode = "ENROLLMENT_ERROR";
    }
    
    public EnrollmentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public EnrollmentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ENROLLMENT_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // Static factory methods for common enrollment errors
    public static EnrollmentException alreadyEnrolled() {
        return new EnrollmentException("Student is already enrolled in this course", "ALREADY_ENROLLED");
    }
    
    public static EnrollmentException courseNotFound() {
        return new EnrollmentException("Course not found or not available for enrollment", "COURSE_NOT_FOUND");
    }
    
    public static EnrollmentException courseNotPublic() {
        return new EnrollmentException("Cannot enroll in private courses through public enrollment", "COURSE_NOT_PUBLIC");
    }
    
    public static EnrollmentException courseRequiresPayment() {
        return new EnrollmentException("This course requires payment. Please use the purchase flow.", "PAYMENT_REQUIRED");
    }
    
    public static EnrollmentException enrollmentClosed() {
        return new EnrollmentException("Enrollment for this course is currently closed", "ENROLLMENT_CLOSED");
    }
    
    public static EnrollmentException capacityFull() {
        return new EnrollmentException("Course has reached maximum enrollment capacity", "CAPACITY_FULL");
    }
    
    public static EnrollmentException prerequisiteNotMet() {
        return new EnrollmentException("Prerequisites for this course have not been met", "PREREQUISITE_NOT_MET");
    }
    
    public static EnrollmentException accessDenied() {
        return new EnrollmentException("You don't have access to enroll in this course", "ACCESS_DENIED");
    }
}