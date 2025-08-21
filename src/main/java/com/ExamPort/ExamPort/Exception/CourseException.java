package com.ExamPort.ExamPort.Exception;

public class CourseException extends RuntimeException {
    
    public CourseException(String message) {
        super(message);
    }
    
    public CourseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // Specific course-related exceptions
    public static class CourseNotFoundException extends CourseException {
        public CourseNotFoundException(Long courseId) {
            super("Course not found with ID: " + courseId);
        }
    }
    
    public static class CourseAccessDeniedException extends CourseException {
        public CourseAccessDeniedException(String message) {
            super(message);
        }
    }
    
    public static class InvalidCourseDataException extends CourseException {
        public InvalidCourseDataException(String message) {
            super(message);
        }
    }
    
    public static class CourseAlreadyExistsException extends CourseException {
        public CourseAlreadyExistsException(String courseName) {
            super("Course already exists with name: " + courseName);
        }
    }
    
    public static class InvalidPricingException extends CourseException {
        public InvalidPricingException(String message) {
            super(message);
        }
    }
    
    public static class CourseNotAvailableForPurchaseException extends CourseException {
        public CourseNotAvailableForPurchaseException(String message) {
            super(message);
        }
    }
}