package com.ExamPort.ExamPort.Repository;

import com.ExamPort.ExamPort.Entity.Enrollment;
import com.ExamPort.ExamPort.Entity.EnrollmentStatus;
import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing student enrollments in courses
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    /**
     * Find enrollment by student and course
     * @param student The student user
     * @param course The course
     * @return Optional enrollment if exists
     */
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);
    
    /**
     * Find enrollment by student ID and course ID
     * @param studentId The student's user ID
     * @param courseId The course ID
     * @return Optional enrollment if exists
     */
    Optional<Enrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);
    
    /**
     * Find all enrollments for a specific student
     * @param student The student user
     * @return List of enrollments for the student
     */
    List<Enrollment> findByStudent(User student);
    
    /**
     * Find all enrollments for a specific student by ID
     * @param studentId The student's user ID
     * @return List of enrollments for the student
     */
    List<Enrollment> findByStudent_Id(Long studentId);
    
    /**
     * Find all enrollments for a specific student by ID with pagination
     * @param studentId The student's user ID
     * @param pageable Pagination information
     * @return Page of enrollments for the student
     */
    Page<Enrollment> findByStudent_Id(Long studentId, Pageable pageable);
    
    /**
     * Find all enrollments for a specific course
     * @param course The course
     * @return List of enrollments for the course
     */
    List<Enrollment> findByCourse(Course course);
    
    /**
     * Find all enrollments for a specific course by ID
     * @param courseId The course ID
     * @return List of enrollments for the course
     */
    List<Enrollment> findByCourse_Id(Long courseId);
    
    /**
     * Find all enrollments for a specific course by ID with pagination
     * @param courseId The course ID
     * @param pageable Pagination information
     * @return Page of enrollments for the course
     */
    Page<Enrollment> findByCourse_Id(Long courseId, Pageable pageable);
    
    /**
     * Find enrollments by status
     * @param status The enrollment status
     * @return List of enrollments with the specified status
     */
    List<Enrollment> findByStatus(EnrollmentStatus status);
    
    /**
     * Find enrollments by student and status
     * @param student The student user
     * @param status The enrollment status
     * @return List of enrollments for the student with specified status
     */
    List<Enrollment> findByStudentAndStatus(User student, EnrollmentStatus status);
    
    /**
     * Find enrollment by payment transaction ID
     * @param paymentTransactionId The Razorpay transaction ID
     * @return Optional enrollment if exists
     */
    Optional<Enrollment> findByPaymentTransactionId(String paymentTransactionId);
    
    /**
     * Check if a student is enrolled in a course (regardless of status)
     * @param studentId The student's user ID
     * @param courseId The course ID
     * @return true if enrollment exists, false otherwise
     */
    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);
    
    /**
     * Check if a student is successfully enrolled in a course (ENROLLED status only)
     * @param studentId The student's user ID
     * @param courseId The course ID
     * @return true if student is enrolled with ENROLLED status
     */
    @Query("SELECT COUNT(e) > 0 FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId AND e.status = 'ENROLLED'")
    boolean isStudentEnrolledInCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    /**
     * Get count of enrolled students for a course
     * @param courseId The course ID
     * @return Number of students enrolled in the course
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.status = 'ENROLLED'")
    Long countEnrolledStudentsByCourseId(@Param("courseId") Long courseId);
    
    /**
     * Get count of enrolled students for multiple courses
     * @param courseIds List of course IDs
     * @return List of enrollment counts
     */
    @Query("SELECT e.course.id, COUNT(e) FROM Enrollment e WHERE e.course.id IN :courseIds AND e.status = 'ENROLLED' GROUP BY e.course.id")
    List<Object[]> countEnrolledStudentsByMultipleCourseIds(@Param("courseIds") List<Long> courseIds);
    
    /**
     * Find all enrollments for courses taught by a specific instructor
     * @param instructorId The instructor's user ID
     * @return List of enrollments for instructor's courses
     */
    @Query("SELECT e FROM Enrollment e WHERE e.course.instructor.id = :instructorId")
    List<Enrollment> findEnrollmentsByInstructorId(@Param("instructorId") Long instructorId);
    
    /**
     * Get enrollment statistics for an instructor's courses
     * @param instructorId The instructor's user ID
     * @return Array containing [total_enrollments, enrolled_count, payment_pending_count]
     */
    @Query("SELECT COUNT(e), " +
           "SUM(CASE WHEN e.status = 'ENROLLED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.status = 'PAYMENT_PENDING' THEN 1 ELSE 0 END) " +
           "FROM Enrollment e WHERE e.course.instructor.id = :instructorId")
    Object[] getEnrollmentStatsByInstructorId(@Param("instructorId") Long instructorId);
    
    // Method aliases for compatibility with EnrollmentService
    default boolean existsByStudentIdAndCourseId(Long studentId, Long courseId) {
        return existsByStudent_IdAndCourse_Id(studentId, courseId);
    }
    
    default Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId) {
        return findByStudent_IdAndCourse_Id(studentId, courseId);
    }
    
    default List<Enrollment> findByStudentId(Long studentId) {
        return findByStudent_Id(studentId);
    }
    
    default Page<Enrollment> findByStudentId(Long studentId, Pageable pageable) {
        return findByStudent_Id(studentId, pageable);
    }
    
    default List<Enrollment> findByCourseId(Long courseId) {
        return findByCourse_Id(courseId);
    }
    
    default Page<Enrollment> findByCourseId(Long courseId, Pageable pageable) {
        return findByCourse_Id(courseId, pageable);
    }
    
    default long countByCourseId(Long courseId) {
        return countEnrolledStudentsByCourseId(courseId);
    }
    
    /**
     * Find enrollments by student username with pagination
     * @param username The student's username
     * @param pageable Pagination information
     * @return Page of enrollments for the student
     */
    Page<Enrollment> findByStudentUsername(String username, Pageable pageable);
    
    /**
     * Check if user is enrolled in course by username
     * @param username The student's username
     * @param courseId The course ID
     * @return true if enrollment exists
     */
    boolean existsByStudentUsernameAndCourseId(String username, Long courseId);
    
    /**
     * Find enrollment by student username and course ID
     * @param username The student's username
     * @param courseId The course ID
     * @return Optional enrollment if exists
     */
    Optional<Enrollment> findByStudentUsernameAndCourseId(String username, Long courseId);
    
    /**
     * Delete all enrollments for a specific student
     * @param student The student user
     * @return Number of deleted enrollments
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM Enrollment e WHERE e.student = :student")
    int deleteByStudent(@Param("student") User student);
    
    /**
     * Count enrollments for a specific student
     * @param student The student user
     * @return Number of enrollments
     */
    long countByStudent(User student);
}