package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(java.security.Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to /me endpoint");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Fetching profile for user: {}", username);
        
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                logger.warn("User profile not found with username: {}, trying email lookup...", username);
                // Try to find by email as fallback (in case user logged in with email)
                userOpt = userRepository.findByEmail(username);
                if (userOpt.isEmpty()) {
                    logger.warn("User profile not found with username or email: {}", username);
                    return ResponseEntity.status(404).body("User not found");
                }
            }
            
            User user = userOpt.get();
            logger.debug("Profile retrieved successfully for user: {}", username);
            
            return ResponseEntity.ok(new UserProfileDTO(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getRole(),
        user.getFullName(),
        user.getAvatarUrl(),
        user.getGender(),
        user.getPhoneNumber()
    ));
        } catch (Exception e) {
            logger.error("Error fetching profile for user: {}", username, e);
            return ResponseEntity.internalServerError().body("Error fetching user profile");
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(java.security.Principal principal, @RequestBody UserProfileDTO update) {
        if (principal == null) {
            logger.warn("Unauthorized profile update attempt");
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = principal.getName();
        logger.info("Profile update request for user: {}", username);
        
        try {
            logger.debug("Received update data: username={}, email={}, phoneNumber={}, fullName={}", 
                        update.username, update.email, update.phoneNumber, update.fullName);
            
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                logger.warn("User not found for profile update with username: {}, trying email lookup...", username);
                // Try to find by email as fallback (in case user logged in with email)
                userOpt = userRepository.findByEmail(username);
                if (userOpt.isEmpty()) {
                    logger.warn("User not found for profile update with username or email: {}", username);
                    java.util.Map<String, String> error = new java.util.HashMap<>();
                    error.put("error", "User session expired. Please log in again.");
                    return ResponseEntity.status(404).body(error);
                }
            }
            
            User user = userOpt.get();
            logger.debug("Current user data: username={}, email={}, phoneNumber={}", 
                        user.getUsername(), user.getEmail(), user.getPhoneNumber());
            boolean passwordChanged = false;
            
            // Validate uniqueness before updating
            if (update.username != null && !update.username.isBlank() && !update.username.equals(user.getUsername())) {
                Optional<User> existingUser = userRepository.findByUsername(update.username);
                if (existingUser.isPresent()) {
                    logger.warn("Username already exists: {}", update.username);
                    java.util.Map<String, String> error = new java.util.HashMap<>();
                    error.put("error", "Username '" + update.username + "' is already taken by another user");
                    return ResponseEntity.status(400).body(error);
                }
                logger.debug("Updating username for user: {} to {}", username, update.username);
                user.setUsername(update.username);
                
                // Mark that username was changed - this will require re-login
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("usernameChanged", true);
                response.put("message", "Username updated successfully. Please log in again with your new username.");
                response.put("newUsername", update.username);
                
                userRepository.save(user);
                logger.info("Username changed for user: {} to {} - re-login required", username, update.username);
                
                return ResponseEntity.ok(response);
            }
            
            // Email updates are not allowed for security reasons
            if (update.email != null && !update.email.isBlank() && !update.email.equals(user.getEmail())) {
                logger.warn("Attempted email update blocked for user: {} - Email changes not allowed", username);
                java.util.Map<String, String> error = new java.util.HashMap<>();
                error.put("error", "Email address cannot be changed for security reasons");
                return ResponseEntity.status(400).body(error);
            }
            
            if (update.phoneNumber != null && !update.phoneNumber.isBlank()) {
                // Validate phone number format (exactly 10 digits)
                if (!update.phoneNumber.matches("^[0-9]{10}$")) {
                    logger.warn("Invalid phone number format: {}", update.phoneNumber);
                    java.util.Map<String, String> error = new java.util.HashMap<>();
                    error.put("error", "Phone number must be exactly 10 digits");
                    return ResponseEntity.status(400).body(error);
                }
                
                // Check if phone number is already taken by another user
                String currentPhoneNumber = user.getPhoneNumber();
                if (currentPhoneNumber == null || !update.phoneNumber.equals(currentPhoneNumber)) {
                    try {
                        logger.debug("Checking phone number uniqueness for: {}", update.phoneNumber);
                        
                        // Try to use the findByPhoneNumber method
                        Optional<User> existingUser = userRepository.findByPhoneNumber(update.phoneNumber);
                        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                            logger.warn("Phone number already exists: {}", update.phoneNumber);
                            java.util.Map<String, String> error = new java.util.HashMap<>();
                            error.put("error", "Phone number '" + update.phoneNumber + "' is already registered with another user");
                            return ResponseEntity.status(400).body(error);
                        }
                        logger.debug("Phone number uniqueness check passed");
                    } catch (Exception e) {
                        logger.error("Error checking phone number uniqueness: {}", e.getMessage(), e);
                        
                        // Fallback: try using existsByPhoneNumber if findByPhoneNumber fails
                        try {
                            logger.debug("Trying fallback phone number check with existsByPhoneNumber");
                            if (userRepository.existsByPhoneNumber(update.phoneNumber)) {
                                logger.warn("Phone number already exists (fallback check): {}", update.phoneNumber);
                                java.util.Map<String, String> error = new java.util.HashMap<>();
                                error.put("error", "Phone number '" + update.phoneNumber + "' is already registered with another user");
                                return ResponseEntity.status(400).body(error);
                            }
                        } catch (Exception fallbackError) {
                            logger.error("Fallback phone number check also failed: {}", fallbackError.getMessage(), fallbackError);
                            // If both methods fail, we'll skip uniqueness validation but still allow the update
                            logger.warn("Skipping phone number uniqueness check due to database errors");
                        }
                    }
                }
                user.setPhoneNumber(update.phoneNumber);
            }
            
            // Update other fields
            if (update.fullName != null) user.setFullName(update.fullName);
            if (update.gender != null) user.setGender(update.gender);
            if (update.avatarUrl != null) user.setAvatarUrl(update.avatarUrl);
            
            // Password update logic
            if (update.currentPassword != null && update.newPassword != null && !update.newPassword.isBlank()) {
                logger.info("Password change request for user: {}", username);
                org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
                
                // Check if current password is correct
                if (!encoder.matches(update.currentPassword, user.getPassword())) {
                    logger.warn("Invalid current password provided for user: {}", username);
                    java.util.Map<String, String> error = new java.util.HashMap<>();
                    error.put("error", "Current password is incorrect");
                    return ResponseEntity.status(400).body(error);
                }
                
                // Check if new password is same as current password
                if (encoder.matches(update.newPassword, user.getPassword())) {
                    logger.warn("New password is same as current password for user: {}", username);
                    java.util.Map<String, String> error = new java.util.HashMap<>();
                    error.put("error", "New password cannot be the same as current password");
                    return ResponseEntity.status(400).body(error);
                }
                
                user.setPassword(encoder.encode(update.newPassword));
                passwordChanged = true;
                logger.info("Password updated successfully for user: {}", username);
            }
            
            userRepository.save(user);
            logger.info("Profile updated successfully for user: {} (password changed: {})", username, passwordChanged);
            
            return ResponseEntity.ok(new UserProfileDTO(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getRole(),
        user.getFullName(),
        user.getAvatarUrl(),
        user.getGender(),
        user.getPhoneNumber()
    ));
        } catch (Exception e) {
            logger.error("Error updating profile for user: {} - Error: {}", username, e.getMessage(), e);
            java.util.Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "Error updating user profile: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    public static class UserProfileDTO {
        public Long id;
        public String username;
        public String email;
        public String role;
        public String fullName;
        public String avatarUrl;
        public String gender;
        public String phoneNumber;
        public String currentPassword;
        public String newPassword;
        
        public UserProfileDTO() {
            // Default constructor for JSON deserialization
        }
        
        public UserProfileDTO(Long id, String username, String email, String role, String fullName, String avatarUrl, String gender, String phoneNumber) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
            this.fullName = fullName;
            this.avatarUrl = avatarUrl;
            this.gender = gender;
            this.phoneNumber = phoneNumber;
        }
    }
}
