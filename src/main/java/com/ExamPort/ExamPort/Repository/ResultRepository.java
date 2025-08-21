package com.ExamPort.ExamPort.Repository;

import com.ExamPort.ExamPort.Entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {
    
    // Find results by user ID
    List<Result> findByUserId(Long userId);
    
    // Find results by exam ID
    List<Result> findByExamExamId(Long examId);
    
    // Find result by user and exam (to check if already attempted)
    Optional<Result> findByUserIdAndExamExamId(Long userId, Long examId);
    
    // Find results by user ID ordered by attempt date
    List<Result> findByUserIdOrderByAttemptDateDesc(Long userId);
    
    // Find results by exam ID ordered by score
    List<Result> findByExamExamIdOrderByScoreDesc(Long examId);
    
    // Count total attempts for an exam
    @Query("SELECT COUNT(r) FROM Result r WHERE r.examExamId = :examId")
    Long countAttemptsByExamId(@Param("examId") Long examId);
    
    // Get average score for an exam
    @Query("SELECT AVG(r.score) FROM Result r WHERE r.examExamId = :examId")
    Double getAverageScoreByExamId(@Param("examId") Long examId);
    
    // Delete all results for a specific user
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM Result r WHERE r.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
    
    // Count results for a specific user
    long countByUserId(Long userId);
}