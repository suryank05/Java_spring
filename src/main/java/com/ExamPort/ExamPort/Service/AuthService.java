package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Constants.TokenType;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Entity.VerificationToken;
import com.ExamPort.ExamPort.Repository.UserRepository;
import com.ExamPort.ExamPort.Repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VerificationTokenRepository tokenRepository;
    
    @Autowired
    private EmailService emailService;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Generate and send email verification token
     */
    @Transactional
    public boolean sendEmailVerification(User user) {
        try {
            // Invalidate any existing email verification tokens
            tokenRepository.markAllUserTokensAsUsed(user, TokenType.EMAIL_VERIFICATION);
            
            // Generate new verification token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(24); // 24 hours expiry
            
            VerificationToken verificationToken = new VerificationToken(
                token, user, expiryTime, TokenType.EMAIL_VERIFICATION
            );
            
            saveTokenWithFallback(verificationToken);
            
            // Send verification email
            emailService.sendVerificationEmail(user.getEmail(), token);
            
            logger.info("Email verification token sent to user: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send email verification for user: {}", user.getEmail(), e);
            return false;
        }
    }
    
    /**
     * Verify email using token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        try {
            logger.info("Starting email verification for token: {}", token);
            
            Optional<VerificationToken> tokenOpt = tokenRepository.findByTokenAndTokenType(
                token, TokenType.EMAIL_VERIFICATION
            );
            
            if (tokenOpt.isEmpty()) {
                logger.warn("Email verification failed - Token not found: {}", token);
                return false;
            }
            
            VerificationToken verificationToken = tokenOpt.get();
            User user = verificationToken.getUser();
            
            logger.info("Token found for user: {}, isUsed: {}, isExpired: {}, expiryTime: {}", 
                       user.getEmail(), verificationToken.isUsed(), verificationToken.isExpired(), verificationToken.getExpiryTime());
            
            // Check if user is already verified
            if (user.isEmailVerified()) {
                logger.info("User {} is already verified, marking token as used and returning success", user.getEmail());
                verificationToken.setUsed(true);
                saveTokenWithFallback(verificationToken);
                return true;
            }
            
            if (!verificationToken.isValid()) {
                logger.warn("Email verification failed - Token invalid or expired for user: {}, isUsed: {}, isExpired: {}", 
                           user.getEmail(), verificationToken.isUsed(), verificationToken.isExpired());
                return false;
            }
            
            // Mark user as verified
            user.setEmailVerified(true);
            userRepository.save(user);
            logger.info("User {} marked as email verified in database", user.getEmail());
            
            // Mark token as used
            verificationToken.setUsed(true);
            saveTokenWithFallback(verificationToken);
            logger.info("Token marked as used for user: {}", user.getEmail());
            
            logger.info("Email verified successfully for user: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Error during email verification for token: {}", token, e);
            return false;
        }
    }
    
    /**
     * Send password reset token
     */
    @Transactional
    public boolean sendPasswordResetToken(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                logger.warn("Password reset requested for non-existent email: {}", email);
                return false; // Return false to indicate email doesn't exist
            }
            
            User user = userOpt.get();
            
            // Invalidate any existing password reset tokens
            tokenRepository.markAllUserTokensAsUsed(user, TokenType.PASSWORD_RESET);
            
            // Generate new reset token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(30); // 30 minutes expiry
            
            VerificationToken resetToken = new VerificationToken(
                token, user, expiryTime, TokenType.PASSWORD_RESET
            );
            
            saveTokenWithFallback(resetToken);
            
            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            
            logger.info("Password reset token sent to user: {}", email);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send password reset token for email: {}", email, e);
            return false;
        }
    }
    
    /**
     * Reset password using token
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        try {
            Optional<VerificationToken> tokenOpt = tokenRepository.findByTokenAndTokenType(
                token, TokenType.PASSWORD_RESET
            );
            
            if (tokenOpt.isEmpty()) {
                logger.warn("Password reset failed - Token not found: {}", token);
                return false;
            }
            
            VerificationToken resetToken = tokenOpt.get();
            
            if (!resetToken.isValid()) {
                logger.warn("Password reset failed - Token invalid or expired: {}", token);
                return false;
            }
            
            // Update user password
            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            // Mark token as used
            resetToken.setUsed(true);
            saveTokenWithFallback(resetToken);
            
            // Invalidate all other password reset tokens for this user
            tokenRepository.markAllUserTokensAsUsed(user, TokenType.PASSWORD_RESET);
            
            logger.info("Password reset successfully for user: {}", user.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Error during password reset for token: {}", token, e);
            return false;
        }
    }
    
    /**
     * Check if token is valid
     */
    public boolean isTokenValid(String token, String tokenType) {
        try {
            Optional<VerificationToken> tokenOpt = tokenRepository.findByTokenAndTokenType(token, tokenType);
            return tokenOpt.isPresent() && tokenOpt.get().isValid();
        } catch (Exception e) {
            logger.error("Error checking token validity: {}", token, e);
            return false;
        }
    }
    
    /**
     * Helper method to save verification token with fallback for AUTO_INCREMENT issues
     */
    private VerificationToken saveTokenWithFallback(VerificationToken token) {
        try {
            return tokenRepository.save(token);
        } catch (Exception e) {
            logger.warn("Failed to save token with auto-generated ID, trying manual ID assignment: {}", e.getMessage());
            
            // Fallback: manually assign an ID
            Long maxId = tokenRepository.findMaxId();
            Long nextId = (maxId != null ? maxId + 1 : 1L);
            token.setId(nextId);
            
            try {
                VerificationToken savedToken = tokenRepository.save(token);
                logger.info("Successfully saved token with manual ID: {}", nextId);
                return savedToken;
            } catch (Exception e2) {
                logger.error("Failed to save token even with manual ID assignment", e2);
                throw new RuntimeException("Failed to save verification token: " + e2.getMessage(), e2);
            }
        }
    }

    /**
     * Clean up expired tokens (can be called by scheduled task)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            tokenRepository.deleteExpiredTokens(LocalDateTime.now());
            logger.info("Expired tokens cleaned up successfully");
        } catch (Exception e) {
            logger.error("Error cleaning up expired tokens", e);
        }
    }
}