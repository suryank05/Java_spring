package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.Review;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Repository.ReviewRepository;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Exception.ResourceNotFoundException;
import com.ExamPort.ExamPort.Exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Submit a new review
     */
    public Review submitReview(String username, String content, Integer rating) {
        logger.info("Submitting review for user: {}", username);
        
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // Check if user already has an active review
        if (reviewRepository.existsByUserAndIsActiveTrue(user)) {
            throw new ValidationException("You have already submitted a review. You can edit your existing review.");
        }
        
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5");
        }
        
        // Validate content
        if (content == null || content.trim().length() < 10) {
            throw new ValidationException("Review content must be at least 10 characters long");
        }
        
        if (content.trim().length() > 1000) {
            throw new ValidationException("Review content must not exceed 1000 characters");
        }
        
        // Create review
        Review review = new Review(user, content.trim(), rating);
        review.setIsApproved(true); // Auto-approve reviews
        review.setIsActive(true);
        
        Review savedReview = reviewRepository.save(review);
        logger.info("Review submitted successfully with ID: {}", savedReview.getId());
        
        return savedReview;
    }
    
    /**
     * Update an existing review
     */
    public Review updateReview(String username, String content, Integer rating) {
        logger.info("Updating review for user: {}", username);
        
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // Find user's existing review
        Review existingReview = reviewRepository.findByUserAndIsActiveTrue(user)
                .orElseThrow(() -> new ResourceNotFoundException("No active review found for user"));
        
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5");
        }
        
        // Validate content
        if (content == null || content.trim().length() < 10) {
            throw new ValidationException("Review content must be at least 10 characters long");
        }
        
        if (content.trim().length() > 1000) {
            throw new ValidationException("Review content must not exceed 1000 characters");
        }
        
        // Update review
        existingReview.setContent(content.trim());
        existingReview.setRating(rating);
        existingReview.setIsApproved(true); // Auto-approve updated reviews
        
        Review updatedReview = reviewRepository.save(existingReview);
        logger.info("Review updated successfully with ID: {}", updatedReview.getId());
        
        return updatedReview;
    }
    
    /**
     * Get user's review
     */
    public Optional<Review> getUserReview(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        return reviewRepository.findByUserAndIsActiveTrue(user);
    }
    
    /**
     * Delete user's review
     */
    public void deleteReview(String username) {
        logger.info("Deleting review for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        Review review = reviewRepository.findByUserAndIsActiveTrue(user)
                .orElseThrow(() -> new ResourceNotFoundException("No active review found for user"));
        
        review.setIsActive(false);
        reviewRepository.save(review);
        
        logger.info("Review deleted successfully for user: {}", username);
    }
    
    /**
     * Get all approved reviews for public display
     */
    public List<Review> getApprovedReviews() {
        return reviewRepository.findByIsApprovedTrueAndIsActiveTrueOrderByCreatedAtDesc();
    }
    
    /**
     * Get approved reviews with pagination
     */
    public Page<Review> getApprovedReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByIsApprovedTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable);
    }
    
    /**
     * Get recent approved reviews for landing page
     */
    public List<Review> getRecentApprovedReviews(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return reviewRepository.findRecentApprovedReviews(pageable);
    }
    
    /**
     * Get pending reviews for admin approval
     */
    public List<Review> getPendingReviews() {
        return reviewRepository.findByIsApprovedFalseAndIsActiveTrueOrderByCreatedAtDesc();
    }
    
    /**
     * Get pending reviews with pagination
     */
    public Page<Review> getPendingReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByIsApprovedFalseAndIsActiveTrueOrderByCreatedAtDesc(pageable);
    }
    
    /**
     * Approve a review (admin only)
     */
    public Review approveReview(Long reviewId) {
        logger.info("Approving review with ID: {}", reviewId);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));
        
        review.setIsApproved(true);
        Review approvedReview = reviewRepository.save(review);
        
        logger.info("Review approved successfully with ID: {}", reviewId);
        return approvedReview;
    }
    
    /**
     * Reject a review (admin only)
     */
    public void rejectReview(Long reviewId) {
        logger.info("Rejecting review with ID: {}", reviewId);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));
        
        review.setIsActive(false);
        reviewRepository.save(review);
        
        logger.info("Review rejected successfully with ID: {}", reviewId);
    }
    
    /**
     * Get review statistics
     */
    public Map<String, Object> getReviewStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalReviews = reviewRepository.countByIsActiveTrueAndIsApprovedTrue();
        long pendingReviews = reviewRepository.countByIsActiveTrueAndIsApprovedFalse();
        Double averageRating = reviewRepository.getAverageRating();
        List<Object[]> ratingDistribution = reviewRepository.getRatingDistribution();
        
        stats.put("totalReviews", totalReviews);
        stats.put("pendingReviews", pendingReviews);
        stats.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        stats.put("ratingDistribution", ratingDistribution);
        
        return stats;
    }
    
    /**
     * Search reviews by content
     */
    public List<Review> searchReviews(String searchTerm) {
        return reviewRepository.searchReviewsByContent(searchTerm);
    }
    
    /**
     * Get reviews by rating
     */
    public List<Review> getReviewsByRating(Integer rating) {
        return reviewRepository.findByRatingAndIsActiveTrueAndIsApprovedTrueOrderByCreatedAtDesc(rating);
    }
    
    /**
     * Get reviews by user role
     */
    public List<Review> getReviewsByUserRole(String role) {
        return reviewRepository.findByUserRole(role);
    }
}