package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.ContactMessage;
import com.ExamPort.ExamPort.Entity.ContactMessageStatus;
import com.ExamPort.ExamPort.Service.ContactMessageService;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ContactMessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactMessageController.class);
    
    @Autowired
    private ContactMessageService contactMessageService;

    @PostMapping
    public ResponseEntity<?> createContactMessage(@Valid @RequestBody ContactMessage contactMessage) {
        try {
            logger.info("Received contact form submission from: {} with subject: {}", 
                       contactMessage.getEmail(), contactMessage.getSubject());
            
            ContactMessage saved = contactMessageService.save(contactMessage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thank you for your message! We'll get back to you soon.");
            response.put("id", saved.getId());
            response.put("referenceNumber", saved.getReferenceNumber());
            response.put("submittedAt", saved.getSubmittedAt());
            
            logger.info("Contact form submission processed successfully with ID: {}", saved.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing contact form submission from: {} - Error: {}", 
                        contactMessage.getEmail(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Sorry, there was an error processing your message. Please try again later.");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Admin endpoints
    @GetMapping("/admin/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) ContactMessageStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String searchTerm) {
        
        try {
            logger.info("Admin fetching contact messages - page: {}, size: {}, filters: email={}, status={}, referenceNumber={}, searchTerm={}", 
                       page, size, email, status, referenceNumber, searchTerm);
            
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ContactMessage> messages;
            if (email != null || status != null || startDate != null || endDate != null || referenceNumber != null || searchTerm != null) {
                messages = contactMessageService.getMessagesWithFilters(email, status, startDate, endDate, referenceNumber, searchTerm, pageable);
            } else {
                messages = contactMessageService.getAllMessages(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages.getContent());
            response.put("totalElements", messages.getTotalElements());
            response.put("totalPages", messages.getTotalPages());
            response.put("currentPage", messages.getNumber());
            response.put("size", messages.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching contact messages for admin", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching contact messages");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/admin/messages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMessageById(@PathVariable Long id) {
        try {
            logger.info("Admin fetching contact message by ID: {}", id);
            
            Optional<ContactMessage> messageOpt = contactMessageService.getMessageById(id);
            if (messageOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Contact message not found");
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", messageOpt.get());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching contact message by ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching contact message");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PutMapping("/admin/messages/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMessageStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        try {
            logger.info("Admin updating status for contact message ID: {}", id);
            
            String statusStr = (String) request.get("status");
            String adminResponse = (String) request.get("adminResponse");
            
            if (statusStr == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Status is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            ContactMessageStatus newStatus;
            try {
                newStatus = ContactMessageStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid status value");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            ContactMessage updatedMessage = contactMessageService.updateMessageStatus(id, newStatus, adminResponse);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status updated successfully");
            response.put("contactMessage", updatedMessage);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error updating contact message status for ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error updating contact message status for ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating contact message status");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getContactStats() {
        try {
            logger.info("Admin fetching contact message statistics");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", contactMessageService.getMessageCountByStatus(ContactMessageStatus.PENDING));
            stats.put("inProgress", contactMessageService.getMessageCountByStatus(ContactMessageStatus.IN_PROGRESS));
            stats.put("replied", contactMessageService.getMessageCountByStatus(ContactMessageStatus.REPLIED));
            
            List<ContactMessage> recentMessages = contactMessageService.getRecentMessages();
            stats.put("recentMessages", recentMessages);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching contact message statistics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching statistics");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @DeleteMapping("/admin/messages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        try {
            logger.info("Admin deleting contact message ID: {}", id);
            
            contactMessageService.deleteMessage(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contact message deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error deleting contact message ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error deleting contact message ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error deleting contact message");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
