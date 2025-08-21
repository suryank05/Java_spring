package com.ExamPort.ExamPort.Repository;

import com.ExamPort.ExamPort.Entity.Review;
import com.ExamPort.ExamPort.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * Find all approved and active reviews
     */
    List<Review> findByIsApprovedTrueAndIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * Find all approved and active reviews with pagination
     */
    Page<Review> findByIsApprovedTrueAndIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find reviews by user
     */
    List<Review> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user);
    
    /**
     * Find reviews by user with pagination
     */
    Page<Review> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * Find pending reviews (not approved yet)
     */
    List<Review> findByIsApprovedFalseAndIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * Find pending reviews with pagination
     */
    Page<Review> findByIsApprovedFalseAndIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Check if user has already submitted a review
     */
    boolean existsByUserAndIsActiveTrue(User user);
    
    /**
     * Find user's active review
     */
    Optional<Review> findByUserAndIsActiveTrue(User user);
    
    /**
     * Count total reviews
     */
    long countByIsActiveTrueAndIsApprovedTrue();
    
    /**
     * Count pending reviews
     */
    long countByIsActiveTrueAndIsApprovedFalse();
    
    /**
     * Get average rating
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.isActive = true AND r.isApproved = true")
    Double getAverageRating();
    
    /**
     * Get rating distribution
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.isActive = true AND r.isApproved = true GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistribution();
    
    /**
     * Find recent approved reviews
     */
    @Query("SELECT r FROM Review r WHERE r.isActive = true AND r.isApproved = true ORDER BY r.createdAt DESC")
    List<Review> findRecentApprovedReviews(Pageable pageable);
    
    /**
     * Find reviews by rating
     */
    List<Review> findByRatingAndIsActiveTrueAndIsApprovedTrueOrderByCreatedAtDesc(Integer rating);
    
    /**
     * Search reviews by content
     */
    @Query("SELECT r FROM Review r WHERE r.isActive = true AND r.isApproved = true AND LOWER(r.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY r.createdAt DESC")
    List<Review> searchReviewsByContent(@Param("searchTerm") String searchTerm);
    
    /**
     * Find reviews by user role
     */
    @Query("SELECT r FROM Review r WHERE r.isActive = true AND r.isApproved = true AND r.user.role = :role ORDER BY r.createdAt DESC")
    List<Review> findByUserRole(@Param("role") String role);
}