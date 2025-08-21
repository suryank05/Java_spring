package com.ExamPort.ExamPort.Repository;

import com.ExamPort.ExamPort.Entity.ContactMessage;
import com.ExamPort.ExamPort.Entity.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    
    // Find by status
    Page<ContactMessage> findByStatus(ContactMessageStatus status, Pageable pageable);
    
    // Find by email containing (case insensitive)
    Page<ContactMessage> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    
    // Find by date range
    Page<ContactMessage> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Complex search query
    @Query("SELECT cm FROM ContactMessage cm WHERE " +
           "(:email IS NULL OR LOWER(cm.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:status IS NULL OR cm.status = :status) AND " +
           "(:startDate IS NULL OR cm.submittedAt >= :startDate) AND " +
           "(:endDate IS NULL OR cm.submittedAt <= :endDate) AND " +
           "(:referenceNumber IS NULL OR LOWER(cm.referenceNumber) LIKE LOWER(CONCAT('%', :referenceNumber, '%'))) AND " +
           "(:searchTerm IS NULL OR LOWER(cm.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(cm.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(cm.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ContactMessage> findWithFilters(
        @Param("email") String email,
        @Param("status") ContactMessageStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("referenceNumber") String referenceNumber,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    // Count by status
    long countByStatus(ContactMessageStatus status);
    
    // Find recent messages
    List<ContactMessage> findTop10ByOrderBySubmittedAtDesc();
    
    // Check if reference number exists
    boolean existsByReferenceNumber(String referenceNumber);
    
    // Find by reference number
    Optional<ContactMessage> findByReferenceNumber(String referenceNumber);
}
