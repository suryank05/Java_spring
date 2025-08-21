package com.ExamPort.ExamPort.Controller;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request DTO for payment verification from Razorpay
 */
public class PaymentVerificationRequest {
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotBlank(message = "Payment ID is required")
    private String paymentId;
    
    @NotBlank(message = "Signature is required")
    private String signature;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    // Default constructor
    public PaymentVerificationRequest() {}
    
    // Constructor
    public PaymentVerificationRequest(String orderId, String paymentId, String signature, 
                                    Long courseId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.signature = signature;
        this.courseId = courseId;
    }
    
    // Getters and setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public Long getCourseId() {
        return courseId;
    }
    
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
    

    
    @Override
    public String toString() {
        return "PaymentVerificationRequest{" +
                "orderId='" + orderId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", signature='[HIDDEN]'" +
                ", courseId=" + courseId +
                '}';
    }
}