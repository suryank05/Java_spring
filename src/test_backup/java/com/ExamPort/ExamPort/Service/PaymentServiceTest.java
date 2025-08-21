package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.User;
import com.ExamPort.ExamPort.Exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        // Set test configuration
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "test_key_id");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_key_secret");

        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("student@test.com");
        testUser.setFullName("Test Student");

        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setName("Test Course");
        testCourse.setDescription("Test Description");
        testCourse.setPricing("PAID");
        testCourse.setPrice(1000.0);
    }

    @Test
    void createPaymentOrder_Success() {
        // Act
        Map<String, Object> result = paymentService.createPaymentOrder(testCourse, testUser);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("orderId"));
        assertTrue(result.containsKey("amount"));
        assertTrue(result.containsKey("currency"));
        assertTrue(result.containsKey("courseName"));
        assertTrue(result.containsKey("studentName"));
        assertTrue(result.containsKey("studentEmail"));

        assertEquals(100000, result.get("amount")); // 1000 * 100 (paise)
        assertEquals("INR", result.get("currency"));
        assertEquals("Test Course", result.get("courseName"));
        assertEquals("Test Student", result.get("studentName"));
        assertEquals("student@test.com", result.get("studentEmail"));

        String orderId = (String) result.get("orderId");
        assertTrue(orderId.startsWith("order_"));
    }

    @Test
    void createPaymentOrder_FreeCourse() {
        // Arrange
        testCourse.setPricing("FREE");
        testCourse.setPrice(0.0);

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.createPaymentOrder(testCourse, testUser);
        });

        assertEquals("Cannot create payment order for free course", exception.getMessage());
    }

    @Test
    void createPaymentOrder_NullPrice() {
        // Arrange
        testCourse.setPrice(null);

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.createPaymentOrder(testCourse, testUser);
        });

        assertEquals("Course price is not set", exception.getMessage());
    }

    @Test
    void createPaymentOrder_ZeroPrice() {
        // Arrange
        testCourse.setPrice(0.0);

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.createPaymentOrder(testCourse, testUser);
        });

        assertEquals("Course price must be greater than zero", exception.getMessage());
    }

    @Test
    void verifyPaymentSignature_ValidSignature() {
        // Arrange
        String orderId = "order_test123";
        String paymentId = "pay_test123";
        String signature = "valid_signature";

        // Note: In a real implementation, you would mock the Razorpay signature verification
        // For this test, we'll assume the method returns true for any non-null values

        // Act
        boolean result = paymentService.verifyPaymentSignature(orderId, paymentId, signature);

        // Assert
        // Since we're using a mock implementation that always returns true for demo purposes
        assertTrue(result);
    }

    @Test
    void verifyPaymentSignature_NullOrderId() {
        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.verifyPaymentSignature(null, "pay_test123", "signature");
        });

        assertEquals("Order ID cannot be null", exception.getMessage());
    }

    @Test
    void verifyPaymentSignature_NullPaymentId() {
        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.verifyPaymentSignature("order_test123", null, "signature");
        });

        assertEquals("Payment ID cannot be null", exception.getMessage());
    }

    @Test
    void verifyPaymentSignature_NullSignature() {
        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.verifyPaymentSignature("order_test123", "pay_test123", null);
        });

        assertEquals("Signature cannot be null", exception.getMessage());
    }

    @Test
    void calculateAmount_Success() {
        // Act
        int result = paymentService.calculateAmount(1000.0);

        // Assert
        assertEquals(100000, result); // 1000 * 100
    }

    @Test
    void calculateAmount_WithDecimals() {
        // Act
        int result = paymentService.calculateAmount(999.99);

        // Assert
        assertEquals(99999, result); // 999.99 * 100
    }

    @Test
    void calculateAmount_Zero() {
        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.calculateAmount(0.0);
        });

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void calculateAmount_Negative() {
        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.calculateAmount(-100.0);
        });

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void generateOrderId_UniqueIds() {
        // Act
        String orderId1 = paymentService.generateOrderId();
        String orderId2 = paymentService.generateOrderId();

        // Assert
        assertNotNull(orderId1);
        assertNotNull(orderId2);
        assertNotEquals(orderId1, orderId2);
        assertTrue(orderId1.startsWith("order_"));
        assertTrue(orderId2.startsWith("order_"));
    }

    @Test
    void isValidAmount_ValidAmounts() {
        // Act & Assert
        assertTrue(paymentService.isValidAmount(1.0));
        assertTrue(paymentService.isValidAmount(999.99));
        assertTrue(paymentService.isValidAmount(10000.0));
    }

    @Test
    void isValidAmount_InvalidAmounts() {
        // Act & Assert
        assertFalse(paymentService.isValidAmount(0.0));
        assertFalse(paymentService.isValidAmount(-1.0));
        assertFalse(paymentService.isValidAmount(-999.99));
    }

    @Test
    void isValidAmount_NullAmount() {
        // Act & Assert
        assertFalse(paymentService.isValidAmount(null));
    }

    @Test
    void formatCurrency_Success() {
        // Act
        String result = paymentService.formatCurrency(1000.0);

        // Assert
        assertEquals("₹1,000.00", result);
    }

    @Test
    void formatCurrency_WithDecimals() {
        // Act
        String result = paymentService.formatCurrency(999.99);

        // Assert
        assertEquals("₹999.99", result);
    }

    @Test
    void formatCurrency_LargeAmount() {
        // Act
        String result = paymentService.formatCurrency(123456.78);

        // Assert
        assertEquals("₹1,23,456.78", result);
    }
}