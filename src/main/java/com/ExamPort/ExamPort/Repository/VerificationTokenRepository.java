package com.ExamPort.ExamPort.Repository;

import com.ExamPort.ExamPort.Entity.VerificationToken;
import com.ExamPort.ExamPort.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    
    Optional<VerificationToken> findByToken(String token);
    
    Optional<VerificationToken> findByTokenAndTokenType(String token, String tokenType);
    
    Optional<VerificationToken> findByUserAndTokenTypeAndUsedFalse(User user, String tokenType);
    
    @Query("SELECT vt FROM VerificationToken vt WHERE vt.user = :user AND vt.tokenType = :tokenType AND vt.used = false ORDER BY vt.createdAt DESC")
    Optional<VerificationToken> findLatestValidTokenByUserAndType(@Param("user") User user, @Param("tokenType") String tokenType);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiryTime < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE VerificationToken vt SET vt.used = true WHERE vt.user = :user AND vt.tokenType = :tokenType")
    void markAllUserTokensAsUsed(@Param("user") User user, @Param("tokenType") String tokenType);
    
    @Query("SELECT MAX(vt.id) FROM VerificationToken vt")
    Long findMaxId();
    
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken vt WHERE vt.user = :user")
    int deleteByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE vt.user = :user")
    long countByUser(@Param("user") User user);
    
    boolean existsByUserAndTokenTypeAndUsedFalse(User user, String tokenType);
}