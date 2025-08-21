package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling payment-related operations
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * Get Razorpay configuration for frontend
     * @return Razorpay public key
     */
    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig() {
        try {
            logger.info("Fetching payment configuration");
            
            String razorpayKeyId = paymentService.getRazorpayKeyId();
            
            if (razorpayKeyId == null) {
                logger.warn("Razorpay key not configured");
                return ResponseEntity.status(500).body("Payment gateway not configured");
            }
            
            Map<String, String> config = new HashMap<>();
            config.put("razorpayKeyId", razorpayKeyId);
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            logger.error("Error fetching payment configuration", e);
            return ResponseEntity.status(500).body("Error fetching payment configuration");
        }
    }
}