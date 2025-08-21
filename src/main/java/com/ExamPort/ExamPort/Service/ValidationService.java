package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Entity.CourseVisibility;
import com.ExamPort.ExamPort.Entity.CoursePricing;
import com.ExamPort.ExamPort.Exception.ValidationException;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    @Autowired
    private CourseRepository courseRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Validate course creation
     */
    public void validateCourseCreation(Course course, User instructor) throws ValidationException {
        // Basic validation
        if (course.getName() == null || course.getName().trim().isEmpty()) {
            throw new ValidationException("name", "Course name is required");
        }

        if (course.getName().length() < 3 || course.getName().length() > 100) {
            throw new ValidationException("name", "Course name must be between 3 and 100 characters");
        }

        // Check for duplicate course name for the same instructor
        if (courseRepository.existsByNameAndInstructorId(course.getName(), instructor.getId())) {
            throw new ValidationException("name", "A course with this name already exists");
        }

        // Validate visibility
        if (course.getVisibility() == null) {
            throw new ValidationException("visibility", "Course visibility is required");
        }

        // Validate pricing
        if (course.getPricing() == null) {
            throw new ValidationException("pricing", "Course pricing is required");
        }

        // Validate price for paid courses
        if (course.getPricing() == CoursePricing.PAID) {
            if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("price", "Price is required for paid courses and must be greater than 0");
            }
        }

        // Validate allowed emails for private courses
        if (course.getVisibility() == CourseVisibility.PRIVATE) {
            if (course.getAllowedEmails() == null || course.getAllowedEmails().isEmpty()) {
                throw new ValidationException("allowedEmails", "Allowed emails are required for private courses");
            }
        }
    }

    /**
     * Validate enrollment eligibility
     */
    public void validateEnrollmentEligibility(Course course, User student) throws ValidationException {
        if (course == null) {
            throw new ValidationException("Course not found");
        }

        if (student == null) {
            throw new ValidationException("Student not found");
        }

        // Additional validation can be added here
    }

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate course update
     */
    public void validateCourseUpdate(Course existingCourse, Course updatedCourse, User instructor) throws ValidationException {
        // Basic validation (without duplicate name check)
        if (updatedCourse.getName() == null || updatedCourse.getName().trim().isEmpty()) {
            throw new ValidationException("name", "Course name is required");
        }

        if (updatedCourse.getName().length() < 3 || updatedCourse.getName().length() > 100) {
            throw new ValidationException("name", "Course name must be between 3 and 100 characters");
        }

        // Check for duplicate course name for the same instructor, but exclude the current course
        if (!existingCourse.getName().equals(updatedCourse.getName()) && 
            courseRepository.existsByNameAndInstructorId(updatedCourse.getName(), instructor.getId())) {
            throw new ValidationException("name", "A course with this name already exists");
        }

        // Validate visibility
        if (updatedCourse.getVisibility() == null) {
            throw new ValidationException("visibility", "Course visibility is required");
        }

        // Validate pricing
        if (updatedCourse.getPricing() == null) {
            throw new ValidationException("pricing", "Course pricing is required");
        }

        // Validate price for paid courses
        if (updatedCourse.getPricing() == CoursePricing.PAID) {
            if (updatedCourse.getPrice() == null || updatedCourse.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("price", "Price is required for paid courses and must be greater than 0");
            }
        }

        // Validate allowed emails for private courses
        if (updatedCourse.getVisibility() == CourseVisibility.PRIVATE) {
            if (updatedCourse.getAllowedEmails() == null || updatedCourse.getAllowedEmails().isEmpty()) {
                throw new ValidationException("allowedEmails", "Allowed emails are required for private courses");
            }
        }
    }
}