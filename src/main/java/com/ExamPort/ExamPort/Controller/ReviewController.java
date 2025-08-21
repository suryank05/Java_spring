package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.Review;
import com.ExamPort.ExamPort.Service.ReviewService;
import com.ExamPort.ExamPort.Exception.ResourceNotFoundException;
import com.ExamPort.ExamPort.Exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    
    @Autowired
    private ReviewService reviewService;
    
    /**
     * Submit a new review
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('ROLE_STUDENT') or hasRole('ROLE_INSTRUCTOR')")
    public ResponseEntity<?> submitReview(@Valid @RequestBody ReviewRequest request, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            String username = principal.getName();
            logger.info("Review submission request from user: {}", username);
            
            Review review = reviewService.submitReview(username, request.getContent(), request.getRating());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review submitted successfully!");
            response.put("review", review);
            
            return ResponseEntity.ok(response);
            
        } catch (ValidationException e) {
            logger.warn("Validation error in review submission: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error submitting review: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to submit review"));
        }
    }
    
    /**
     * Update existing review
     */
    @PutMapping("/update")
    @PreAuthorize("hasRole('ROLE_STUDENT') or hasRole('ROLE_INSTRUCTOR')")
    public ResponseEntity<?> updateReview(@Valid @RequestBody ReviewRequest request, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            String username = principal.getName();
            logger.info("Review update request from user: {}", username);
            
            Review review = reviewService.updateReview(username, request.getContent(), request.getRating());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review updated successfully!");
            response.put("review", review);
            
            return ResponseEntity.ok(response);
            
        } catch (ResourceNotFoundException e) {
            logger.warn("Review not found for update: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (ValidationException e) {
            logger.warn("Validation error in review update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating review: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to update review"));
        }
    }
    
    /**
     * Get user's own review
     */
    @GetMapping("/my-review")
    @PreAuthorize("hasRole('ROLE_STUDENT') or hasRole('ROLE_INSTRUCTOR')")
    public ResponseEntity<?> getMyReview(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            String username = principal.getName();
            Optional<Review> review = reviewService.getUserReview(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasReview", review.isPresent());
            if (review.isPresent()) {
                response.put("review", review.get());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user review: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to fetch review"));
        }
    }
    
    /**
     * Delete user's review
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ROLE_STUDENT') or hasRole('ROLE_INSTRUCTOR')")
    public ResponseEntity<?> deleteReview(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            String username = principal.getName();
            reviewService.deleteReview(username);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Review deleted successfully"));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting review: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to delete review"));
        }
    }
    
    /**
     * Get all approved reviews (public endpoint)
     */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Review> reviews = reviewService.getApprovedReviews(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews.getContent());
            response.put("totalElements", reviews.getTotalElements());
            response.put("totalPages", reviews.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching public reviews: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to fetch reviews"));
        }
    }
    
    /**
     * Get recent reviews for landing page
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentReviews(@RequestParam(defaultValue = "6") int limit) {
        try {
            List<Review> reviews = reviewService.getRecentApprovedReviews(limit);
            
            return ResponseEntity.ok(Map.of("success", true, "reviews", reviews));
            
        } catch (Exception e) {
            logger.error("Error fetching recent reviews: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to fetch recent reviews"));
        }
    }
    
    /**
     * Get review statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getReviewStatistics() {
        try {
            Map<String, Object> stats = reviewService.getReviewStatistics();
            return ResponseEntity.ok(Map.of("success", true, "statistics", stats));
            
        } catch (Exception e) {
            logger.error("Error fetching review statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to fetch statistics"));
        }
    }
    
    /**
     * Get pending reviews (admin only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Review> reviews = reviewService.getPendingReviews(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews.getContent());
            response.put("totalElements", reviews.getTotalElements());
            response.put("totalPages", reviews.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching pending reviews: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to fetch pending reviews"));
        }
    }
    
    /**
     * Approve review (admin only)
     */
    @PostMapping("/{reviewId}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> approveReview(@PathVariable Long reviewId) {
        try {
            Review review = reviewService.approveReview(reviewId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Review approved successfully", "review", review));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving review: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to approve review"));
        }
    }
    
    /**
     * Reject review (admin only)
     */
    @PostMapping("/{reviewId}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> rejectReview(@PathVariable Long reviewId) {
        try {
            reviewService.rejectReview(reviewId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Review rejected successfully"));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting review: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to reject review"));
        }
    }
    
    /**
     * Search reviews
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchReviews(@RequestParam String query) {
        try {
            List<Review> reviews = reviewService.searchReviews(query);
            return ResponseEntity.ok(Map.of("success", true, "reviews", reviews));
            
        } catch (Exception e) {
            logger.error("Error searching reviews: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to search reviews"));
        }
    }
    
    /**
     * Get reviews by rating
     */
    @GetMapping("/rating/{rating}")
    public ResponseEntity<?> getReviewsByRating(@PathVariable Integer rating) {
        try {
            List<Review> reviews = reviewService.getReviewsByRating(rating);
            return ResponseEntity.ok(Map.of("success", true, "reviews", reviews));
            
        } catch (Exception e) {
            logger.error("Error fetching reviews by rating: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to fetch reviews"));
        }
    }
    
    /**
     * Test endpoint to verify review system is working
     */
    @GetMapping("/test")
    public ResponseEntity<?> testReviewSystem() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review system is working");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in review test endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Review system test failed"));
        }
    }
    
    // Request DTO class
    public static class ReviewRequest {
        private String content;
        private Integer rating;
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Integer getRating() {
            return rating;
        }
        
        public void setRating(Integer rating) {
            this.rating = rating;
        }
    }
}