package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.Enrollment;
import com.ExamPort.ExamPort.Entity.EnrollmentStatus;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Repository.EnrollmentRepository;
import com.ExamPort.ExamPort.Repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Enroll a student in a course
     */
    public Enrollment enrollStudent(User student, Course course) {
        // Check if already enrolled
        if (isStudentEnrolledInCourse(student.getId(), course.getId())) {
            throw new RuntimeException("Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        return enrollmentRepository.save(enrollment);
    }

    /**
     * Enroll a student in a course with payment transaction ID
     */
    public Enrollment enrollStudentWithPayment(User student, Course course, String paymentTransactionId) {
        // Check if already enrolled
        if (isStudentEnrolledInCourse(student.getId(), course.getId())) {
            throw new RuntimeException("Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setPaymentTransactionId(paymentTransactionId);
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        return enrollmentRepository.save(enrollment);
    }

    /**
     * Check if a student is enrolled in a course
     */
    public boolean isStudentEnrolledInCourse(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Get enrollments by student with pagination
     */
    public Page<Enrollment> getEnrollmentsByStudent(Long studentId, Pageable pageable) {
        return enrollmentRepository.findByStudentId(studentId, pageable);
    }

    /**
     * Get enrollments by student (all)
     */
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    /**
     * Get enrollments by course with pagination
     */
    public Page<Enrollment> getEnrollmentsByCourse(Long courseId, Pageable pageable) {
        return enrollmentRepository.findByCourseId(courseId, pageable);
    }

    /**
     * Get enrollments by course (all)
     */
    public List<Enrollment> getEnrollmentsByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    /**
     * Get enrollment count by course
     */
    public long getEnrollmentCountByCourse(Long courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    /**
     * Get enrollment by ID
     */
    public Enrollment getEnrollmentById(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId).orElse(null);
    }

    /**
     * Get enrollment by student and course
     */
    public Enrollment getEnrollmentByStudentAndCourse(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
    }

    /**
     * Cancel enrollment
     */
    public void cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = getEnrollmentById(enrollmentId);
        if (enrollment != null) {
            enrollment.setStatus(EnrollmentStatus.CANCELLED);
            enrollment.setUpdatedAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        }
    }

    /**
     * Get enrollment statistics for instructor
     */
    public Map<String, Object> getInstructorEnrollmentStats(Long instructorId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get all courses by instructor
        List<Course> instructorCourses = courseRepository.findByInstructorId(instructorId);
        
        long totalEnrollments = 0;
        long totalRevenue = 0;
        Map<String, Object> courseStats = new HashMap<>();
        
        for (Course course : instructorCourses) {
            long courseEnrollments = getEnrollmentCountByCourse(course.getId());
            totalEnrollments += courseEnrollments;
            
            // Calculate revenue for paid courses
            if (course.getPricing() != null && course.getPricing().name().equals("PAID") && course.getPrice() != null) {
                totalRevenue += courseEnrollments * course.getPrice().longValue();
            }
            
            Map<String, Object> courseStat = new HashMap<>();
            courseStat.put("courseName", course.getName());
            courseStat.put("enrollments", courseEnrollments);
            courseStat.put("pricing", course.getPricing() != null ? course.getPricing().name() : "FREE");
            courseStat.put("price", course.getPrice());
            courseStat.put("revenue", course.getPricing() != null && course.getPricing().name().equals("PAID") && course.getPrice() != null ? 
                          courseEnrollments * course.getPrice().longValue() : 0);
            
            courseStats.put(course.getId().toString(), courseStat);
        }
        
        stats.put("totalCourses", instructorCourses.size());
        stats.put("totalEnrollments", totalEnrollments);
        stats.put("totalRevenue", totalRevenue);
        stats.put("courseStats", courseStats);
        
        return stats;
    }

    /**
     * Update enrollment status
     */
    public Enrollment updateEnrollmentStatus(Long enrollmentId, EnrollmentStatus status) {
        Enrollment enrollment = getEnrollmentById(enrollmentId);
        if (enrollment != null) {
            enrollment.setStatus(status);
            enrollment.setUpdatedAt(LocalDateTime.now());
            return enrollmentRepository.save(enrollment);
        }
        return null;
    }

    /**
     * Get enrollments by username with pagination
     */
    public Page<Enrollment> getEnrollmentsByUsername(String username, Pageable pageable) {
        return enrollmentRepository.findByStudentUsername(username, pageable);
    }

    /**
     * Check if user is enrolled in course by username
     */
    public boolean isUserEnrolledInCourse(String username, Long courseId) {
        return enrollmentRepository.existsByStudentUsernameAndCourseId(username, courseId);
    }

    /**
     * Get enrollment by username and course ID
     */
    public Enrollment getEnrollmentByUsernameAndCourseId(String username, Long courseId) {
        return enrollmentRepository.findByStudentUsernameAndCourseId(username, courseId).orElse(null);
    }

    /**
     * Get instructor statistics by username
     */
    public Map<String, Object> getInstructorStats(String username) {
        // Get all courses by instructor username
        List<Course> instructorCourses = courseRepository.findByInstructorUsername(username);
        
        Map<String, Object> stats = new HashMap<>();
        
        long totalEnrollments = 0;
        long totalRevenue = 0;
        Map<String, Object> courseStats = new HashMap<>();
        
        for (Course course : instructorCourses) {
            long courseEnrollments = getEnrollmentCountByCourse(course.getId());
            totalEnrollments += courseEnrollments;
            
            // Calculate revenue for paid courses
            if (course.getPricing() != null && course.getPricing().name().equals("PAID") && course.getPrice() != null) {
                totalRevenue += courseEnrollments * course.getPrice().longValue();
            }
            
            Map<String, Object> courseStat = new HashMap<>();
            courseStat.put("courseName", course.getName());
            courseStat.put("enrollments", courseEnrollments);
            courseStat.put("pricing", course.getPricing() != null ? course.getPricing().name() : "FREE");
            courseStat.put("price", course.getPrice());
            courseStat.put("revenue", course.getPricing() != null && course.getPricing().name().equals("PAID") && course.getPrice() != null ? 
                          courseEnrollments * course.getPrice().longValue() : 0);
            
            courseStats.put(course.getId().toString(), courseStat);
        }
        
        stats.put("totalCourses", instructorCourses.size());
        stats.put("totalEnrollments", totalEnrollments);
        stats.put("totalRevenue", totalRevenue);
        stats.put("courseStats", courseStats);
        
        return stats;
    }

    /**
     * Get course enrollments for instructor
     */
    public Page<Enrollment> getCourseEnrollments(Long courseId, String instructorUsername, Pageable pageable) {
        logger.info("Getting course enrollments for course: {} by instructor: {}", courseId, instructorUsername);
        
        // First verify that the instructor owns this course
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            logger.warn("Course not found: {}", courseId);
            throw new RuntimeException("Course not found");
        }
        
        if (course.getInstructor() == null) {
            logger.warn("Course {} has no instructor assigned", courseId);
            throw new RuntimeException("Course has no instructor assigned");
        }
        
        if (!course.getInstructor().getUsername().equals(instructorUsername)) {
            logger.warn("Access denied: Instructor {} does not own course {}", instructorUsername, courseId);
            throw new RuntimeException("Access denied - you do not own this course");
        }
        
        logger.info("Fetching enrollments for course: {} owned by instructor: {}", courseId, instructorUsername);
        Page<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId, pageable);
        logger.info("Found {} enrollments for course: {}", enrollments.getTotalElements(), courseId);
        
        return enrollments;
    }
}