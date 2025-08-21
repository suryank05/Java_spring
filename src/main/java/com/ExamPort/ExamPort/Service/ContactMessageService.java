package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.ContactMessage;
import com.ExamPort.ExamPort.Entity.ContactMessageStatus;
import com.ExamPort.ExamPort.Repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ContactMessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactMessageService.class);
    
    @Autowired
    private ContactMessageRepository repository;
    
    @Autowired
    private EmailService emailService;

    public ContactMessage save(ContactMessage message) {
        logger.info("Saving contact message from: {} with subject: {}", message.getEmail(), message.getSubject());
        
        // Generate unique reference number
        String referenceNumber = generateReferenceNumber();
        message.setReferenceNumber(referenceNumber);
        logger.info("Generated reference number: {} for contact message from: {}", referenceNumber, message.getEmail());
        
        // Save the message to database
        ContactMessage savedMessage = repository.save(message);
        logger.info("Contact message saved successfully with ID: {} and reference: {}", savedMessage.getId(), savedMessage.getReferenceNumber());
        
        // Send email notification to admin
        try {
            logger.info("Sending email notification to admin for contact message ID: {}", savedMessage.getId());
            emailService.sendContactFormNotification(savedMessage);
            logger.info("Email notification sent successfully to admin for contact message ID: {}", savedMessage.getId());
        } catch (Exception e) {
            logger.error("Failed to send email notification to admin for contact message ID: {} - Error: {}", 
                        savedMessage.getId(), e.getMessage(), e);
            // Don't fail the entire operation if email fails
            // The message is still saved in the database
        }
        
        // Send confirmation email to user
        try {
            logger.info("Sending confirmation email to user for contact message ID: {}", savedMessage.getId());
            emailService.sendContactFormConfirmation(savedMessage);
            logger.info("Confirmation email sent successfully to user for contact message ID: {}", savedMessage.getId());
        } catch (Exception e) {
            logger.error("Failed to send confirmation email to user for contact message ID: {} - Error: {}", 
                        savedMessage.getId(), e.getMessage(), e);
            // Don't fail the entire operation if email fails
            // The message is still saved in the database
        }
        
        return savedMessage;
    }

    public Page<ContactMessage> getAllMessages(Pageable pageable) {
        logger.info("Fetching all contact messages with pagination");
        return repository.findAll(pageable);
    }

    public Page<ContactMessage> getMessagesWithFilters(String email, ContactMessageStatus status, 
                                                      LocalDateTime startDate, LocalDateTime endDate, 
                                                      String referenceNumber, String searchTerm, Pageable pageable) {
        logger.info("Fetching contact messages with filters - email: {}, status: {}, referenceNumber: {}, searchTerm: {}", 
                   email, status, referenceNumber, searchTerm);
        return repository.findWithFilters(email, status, startDate, endDate, referenceNumber, searchTerm, pageable);
    }

    public Optional<ContactMessage> getMessageById(Long id) {
        logger.info("Fetching contact message by ID: {}", id);
        return repository.findById(id);
    }

    @Transactional
    public ContactMessage updateMessageStatus(Long id, ContactMessageStatus newStatus, String adminResponse) {
        logger.info("Updating contact message ID: {} to status: {}", id, newStatus);
        
        Optional<ContactMessage> messageOpt = repository.findById(id);
        if (messageOpt.isEmpty()) {
            logger.error("Contact message not found with ID: {}", id);
            throw new RuntimeException("Contact message not found with ID: " + id);
        }
        
        ContactMessage message = messageOpt.get();
        ContactMessageStatus oldStatus = message.getStatus();
        
        // Update status and response
        message.setStatus(newStatus);
        if (adminResponse != null && !adminResponse.trim().isEmpty()) {
            message.setAdminResponse(adminResponse);
        }
        
        ContactMessage updatedMessage = repository.save(message);
        logger.info("Contact message ID: {} status updated from {} to {}", id, oldStatus, newStatus);
        
        // Send status update email to requester
        try {
            logger.info("Sending status update email to requester for contact message ID: {}", id);
            emailService.sendContactStatusUpdateEmail(updatedMessage, oldStatus);
            logger.info("Status update email sent successfully for contact message ID: {}", id);
        } catch (Exception e) {
            logger.error("Failed to send status update email for contact message ID: {} - Error: {}", 
                        id, e.getMessage(), e);
            // Don't fail the operation if email fails
        }
        
        return updatedMessage;
    }

    public long getMessageCountByStatus(ContactMessageStatus status) {
        return repository.countByStatus(status);
    }

    public List<ContactMessage> getRecentMessages() {
        return repository.findTop10ByOrderBySubmittedAtDesc();
    }

    public void deleteMessage(Long id) {
        logger.info("Deleting contact message with ID: {}", id);
        if (!repository.existsById(id)) {
            throw new RuntimeException("Contact message not found with ID: " + id);
        }
        repository.deleteById(id);
        logger.info("Contact message deleted successfully with ID: {}", id);
    }

    /**
     * Generate a unique reference number for contact messages
     * Format: REF-YYYYMMDD-HHMMSS-XXX
     */
    private String generateReferenceNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timePrefix = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String randomSuffix = String.format("%03d", new Random().nextInt(1000));
        
        String referenceNumber = "REF-" + datePrefix + "-" + timePrefix + "-" + randomSuffix;
        
        // Ensure uniqueness by checking if it already exists
        int attempts = 0;
        while (repository.existsByReferenceNumber(referenceNumber) && attempts < 10) {
            randomSuffix = String.format("%03d", new Random().nextInt(1000));
            referenceNumber = "REF-" + datePrefix + "-" + timePrefix + "-" + randomSuffix;
            attempts++;
        }
        
        return referenceNumber;
    }
}
