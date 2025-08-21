package com.ExamPort.ExamPort.Service;

import com.ExamPort.ExamPort.Entity.ContactMessage;
import com.ExamPort.ExamPort.Entity.ContactMessageStatus;
import com.ExamPort.ExamPort.Entity.Course;
import com.ExamPort.ExamPort.Entity.Exam;
import com.ExamPort.ExamPort.Entity.Question;
import com.ExamPort.ExamPort.Entity.Result;
import com.ExamPort.ExamPort.Entity.User;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${app.email.admin}")
    private String adminEmail;

    /**
     * Send exam result notification to student (Plain Text)
     */
    public void sendExamResultNotification(User student, Exam exam, Result result) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled. Skipping email for user: {}", student.getEmail());
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(student.getEmail());
            message.setSubject("Exam Result - " + exam.getTitle());
            
            String emailBody = buildPlainTextEmailBody(student, exam, result);
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Plain text exam result email sent successfully to: {}", student.getEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send plain text exam result email to: {}", student.getEmail(), e);
        }
    }

    /**
     * Send exam result notification to student (HTML Format)
     */
    public void sendExamResultNotificationHtml(User student, Exam exam, Result result, Map<String, String> answers) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled. Skipping HTML email for user: {}", student.getEmail());
            return;
        }
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(student.getEmail());
            helper.setSubject("🎓 Exam Result - " + exam.getTitle());
            
            String htmlBody = buildHtmlEmailBody(student, exam, result, answers);
            helper.setText(htmlBody, true);
            
            mailSender.send(mimeMessage);
            logger.info("HTML exam result email sent successfully to: {}", student.getEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to send HTML exam result email to: {}", student.getEmail(), e);
            // Fallback to plain text email
            sendExamResultNotification(student, exam, result);
        }
    }

    /**
     * Send email verification email
     */
    public void sendVerificationEmail(String toEmail, String token) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled. Skipping verification email for: {}", toEmail);
            return;
        }
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Verify Your Email - ExamWizards");
            
            String verificationUrl = "http://localhost:5173/verify-email?token=" + token;
            String htmlBody = buildVerificationEmailBody(toEmail, verificationUrl);
            helper.setText(htmlBody, true);
            
            mailSender.send(mimeMessage);
            logger.info("Email verification email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send email verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled. Skipping password reset email for: {}", toEmail);
            return;
        }
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔑 Reset Your Password - ExamWizards");
            
            String resetUrl = "http://localhost:5173/reset-password?token=" + token;
            String htmlBody = buildPasswordResetEmailBody(toEmail, resetUrl);
            helper.setText(htmlBody, true);
            
            mailSender.send(mimeMessage);
            logger.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Send payment receipt email to student
     */
    public void sendPaymentReceiptEmail(User student, Course course, String transactionId, String orderId, BigDecimal amount) {
        if (!emailEnabled) {
            logger.warn("Email notifications are disabled. Cannot send payment receipt email for user: {}", student.getEmail());
            throw new RuntimeException("Email service is disabled");
        }
        
        logger.info("Preparing to send payment receipt email to: {} for course: {} with amount: ₹{}", 
                   student.getEmail(), course.getName(), amount);
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(student.getEmail());
            helper.setSubject("🧾 Payment Receipt - " + course.getName());
            
            logger.debug("Building payment receipt email body for transaction: {}", transactionId);
            String htmlBody = buildPaymentReceiptEmailBody(student, course, transactionId, orderId, amount);
            helper.setText(htmlBody, true);
            
            logger.info("Sending payment receipt email to: {} via SMTP", student.getEmail());
            mailSender.send(mimeMessage);
            logger.info("Payment receipt email sent successfully to: {} for transaction: {}", student.getEmail(), transactionId);
            
        } catch (MessagingException e) {
            logger.error("MessagingException while sending payment receipt email to: {} - Error: {}", student.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send payment receipt email: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending payment receipt email to: {} - Error: {}", student.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send payment receipt email: " + e.getMessage(), e);
        }
    }

    /**
     * Send contact form notification to admin
     */
    public void sendContactFormNotification(ContactMessage contactMessage) {
        if (!emailEnabled) {
            logger.warn("Email notifications are disabled. Cannot send contact form notification");
            return;
        }
        
        logger.info("Preparing to send contact form notification to admin: {}", adminEmail);
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("📧 New Contact Form Submission - " + contactMessage.getSubject());
            
            logger.debug("Building contact form notification email body");
            String htmlBody = buildContactFormNotificationEmailBody(contactMessage);
            helper.setText(htmlBody, true);
            
            logger.info("Sending contact form notification email to admin: {}", adminEmail);
            mailSender.send(mimeMessage);
            logger.info("Contact form notification email sent successfully to admin: {}", adminEmail);
            
        } catch (MessagingException e) {
            logger.error("MessagingException while sending contact form notification to admin: {} - Error: {}", adminEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send contact form notification: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending contact form notification to admin: {} - Error: {}", adminEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send contact form notification: " + e.getMessage(), e);
        }
    }

    /**
     * Send confirmation email to user who submitted contact form
     */
    public void sendContactFormConfirmation(ContactMessage contactMessage) {
        if (!emailEnabled) {
            logger.warn("Email notifications are disabled. Cannot send contact form confirmation");
            return;
        }
        
        logger.info("Preparing to send contact form confirmation to user: {}", contactMessage.getEmail());
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(contactMessage.getEmail());
            helper.setSubject("✅ Thank you for contacting us - " + contactMessage.getSubject());
            
            logger.debug("Building contact form confirmation email body");
            String htmlBody = buildContactFormConfirmationEmailBody(contactMessage);
            helper.setText(htmlBody, true);
            
            logger.info("Sending contact form confirmation email to user: {}", contactMessage.getEmail());
            mailSender.send(mimeMessage);
            logger.info("Contact form confirmation email sent successfully to user: {}", contactMessage.getEmail());
            
        } catch (MessagingException e) {
            logger.error("MessagingException while sending contact form confirmation to user: {} - Error: {}", contactMessage.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send contact form confirmation: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending contact form confirmation to user: {} - Error: {}", contactMessage.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send contact form confirmation: " + e.getMessage(), e);
        }
    }

    /**
     * Send status update email to user when admin updates contact message status
     */
    public void sendContactStatusUpdateEmail(ContactMessage contactMessage, ContactMessageStatus oldStatus) {
        if (!emailEnabled) {
            logger.warn("Email notifications are disabled. Cannot send contact status update email");
            return;
        }
        
        logger.info("Preparing to send contact status update email to user: {}", contactMessage.getEmail());
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(contactMessage.getEmail());
            helper.setSubject("📋 Update on your inquiry - " + contactMessage.getSubject());
            
            logger.debug("Building contact status update email body");
            String htmlBody = buildContactStatusUpdateEmailBody(contactMessage, oldStatus);
            helper.setText(htmlBody, true);
            
            logger.info("Sending contact status update email to user: {}", contactMessage.getEmail());
            mailSender.send(mimeMessage);
            logger.info("Contact status update email sent successfully to user: {}", contactMessage.getEmail());
            
        } catch (MessagingException e) {
            logger.error("MessagingException while sending contact status update to user: {} - Error: {}", contactMessage.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send contact status update: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending contact status update to user: {} - Error: {}", contactMessage.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send contact status update: " + e.getMessage(), e);
        }
    }

    /**
     * Build plain text email body
     */
    private String buildPlainTextEmailBody(User student, Exam exam, Result result) {
        double totalMarks = exam.getTotalMarks() > 0 ? exam.getTotalMarks() : 
                           (exam.getQuestions() != null ? exam.getQuestions().stream()
                               .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 1).sum() : 0);
        
        double percentage = totalMarks > 0 ? (result.getScore() / totalMarks) * 100 : 0;
        String grade = calculateGrade(percentage);
        
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(student.getFullName() != null ? student.getFullName() : student.getUsername()).append(",\n\n");
        body.append("Thank you for completing the exam: ").append(exam.getTitle()).append("\n\n");
        body.append("Here are your results:\n");
        body.append("================================\n");
        body.append("Score: ").append(String.format("%.1f", result.getScore())).append("/").append(String.format("%.0f", totalMarks)).append("\n");
        body.append("Percentage: ").append(String.format("%.1f", percentage)).append("%\n");
        body.append("Grade: ").append(grade).append("\n");
        body.append("Status: ").append(result.getPassed() ? "PASSED ✓" : "FAILED ✗").append("\n");
        body.append("Time Taken: ").append(formatTime(result.getTimeTaken())).append("\n");
        body.append("Submitted: ").append(result.getAttemptDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))).append("\n");
        body.append("================================\n\n");
        
        if (result.getFeedback() != null && !result.getFeedback().trim().isEmpty()) {
            body.append("Feedback:\n").append(result.getFeedback()).append("\n\n");
        }
        
        body.append("Thank you for using ExamWizards!\n\n");
        body.append("Best regards,\n");
        body.append("ExamWizards Team\n");
        body.append("https://examwizards.com");
        
        return body.toString();
    }

    /**
     * Build HTML email body with enhanced formatting
     */
    private String buildHtmlEmailBody(User student, Exam exam, Result result, Map<String, String> answers) {
        double totalMarks = exam.getTotalMarks() > 0 ? exam.getTotalMarks() : 
                           (exam.getQuestions() != null ? exam.getQuestions().stream()
                               .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 1).sum() : 0);
        
        double percentage = totalMarks > 0 ? (result.getScore() / totalMarks) * 100 : 0;
        String grade = calculateGrade(percentage);
        String statusColor = result.getPassed() ? "#10B981" : "#EF4444";
        String statusText = result.getPassed() ? "PASSED ✓" : "FAILED ✗";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Exam Result</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".content { padding: 30px; }");
        html.append(".result-card { background: #f8fafc; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #667eea; }");
        html.append(".score-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 15px; margin: 20px 0; }");
        html.append(".score-item { text-align: center; padding: 15px; background: #f1f5f9; border-radius: 8px; }");
        html.append(".score-value { font-size: 24px; font-weight: bold; color: #1e293b; }");
        html.append(".score-label { font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }");
        html.append(".status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; }");
        html.append(".feedback { background: #fef3c7; border: 1px solid #f59e0b; border-radius: 8px; padding: 15px; margin: 20px 0; }");
        html.append(".footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 20px; font-size: 14px; }");
        html.append(".footer a { color: #60a5fa; text-decoration: none; }");
        html.append("@media (max-width: 600px) { .score-grid { grid-template-columns: repeat(2, 1fr); } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🎓 Exam Result</h1>");
        html.append("<p style='margin: 10px 0 0 0; opacity: 0.9;'>").append(exam.getTitle()).append("</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<h2 style='color: #1e293b; margin-bottom: 10px;'>Dear ").append(student.getFullName() != null ? student.getFullName() : student.getUsername()).append(",</h2>");
        html.append("<p style='color: #64748b; margin-bottom: 25px;'>Thank you for completing your exam. Here are your detailed results:</p>");
        
        // Result Card
        html.append("<div class='result-card'>");
        html.append("<div class='score-grid'>");
        
        html.append("<div class='score-item'>");
        html.append("<div class='score-value'>").append(String.format("%.1f", result.getScore())).append("/").append(String.format("%.0f", totalMarks)).append("</div>");
        html.append("<div class='score-label'>Score</div>");
        html.append("</div>");
        
        html.append("<div class='score-item'>");
        html.append("<div class='score-value'>").append(String.format("%.1f", percentage)).append("%</div>");
        html.append("<div class='score-label'>Percentage</div>");
        html.append("</div>");
        
        html.append("<div class='score-item'>");
        html.append("<div class='score-value'>").append(grade).append("</div>");
        html.append("<div class='score-label'>Grade</div>");
        html.append("</div>");
        
        html.append("<div class='score-item'>");
        html.append("<div class='score-value'>").append(formatTime(result.getTimeTaken())).append("</div>");
        html.append("<div class='score-label'>Time Taken</div>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Status Badge
        html.append("<div style='text-align: center; margin-top: 20px;'>");
        html.append("<span class='status-badge' style='background-color: ").append(statusColor).append("; color: white;'>").append(statusText).append("</span>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Submission Details
        html.append("<div style='background: #f8fafc; border-radius: 8px; padding: 15px; margin: 20px 0;'>");
        html.append("<h3 style='margin: 0 0 10px 0; color: #1e293b; font-size: 16px;'>📅 Submission Details</h3>");
        html.append("<p style='margin: 5px 0; color: #64748b;'><strong>Submitted:</strong> ").append(result.getAttemptDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))).append("</p>");
        html.append("<p style='margin: 5px 0; color: #64748b;'><strong>Duration:</strong> ").append(formatTime(result.getTimeTaken())).append("</p>");
        html.append("</div>");
        
        // Feedback
        if (result.getFeedback() != null && !result.getFeedback().trim().isEmpty()) {
            html.append("<div class='feedback'>");
            html.append("<h3 style='margin: 0 0 10px 0; color: #92400e; font-size: 16px;'>💬 Feedback</h3>");
            html.append("<p style='margin: 0; color: #92400e;'>").append(result.getFeedback()).append("</p>");
            html.append("</div>");
        }
        
        // Question-wise feedback (if available)
        if (exam.getQuestions() != null && !exam.getQuestions().isEmpty() && answers != null && !answers.isEmpty()) {
            html.append("<div style='margin: 25px 0;'>");
            html.append("<h3 style='color: #1e293b; margin-bottom: 15px;'>📝 Question Summary</h3>");
            
            int correctCount = 0;
            int totalQuestions = exam.getQuestions().size();
            
            for (Question question : exam.getQuestions()) {
                String userAnswer = answers.get(String.valueOf(question.getQue_id()));
                boolean isCorrect = isAnswerCorrect(question, userAnswer);
                if (isCorrect) correctCount++;
                
                String statusIcon = isCorrect ? "✅" : "❌";
                String statusColor2 = isCorrect ? "#10B981" : "#EF4444";
                
                html.append("<div style='border-left: 3px solid ").append(statusColor2).append("; padding: 10px 15px; margin: 10px 0; background: #f8fafc;'>");
                html.append("<p style='margin: 0; font-weight: 600; color: #1e293b;'>").append(statusIcon).append(" Question ").append(question.getQue_id()).append("</p>");
                html.append("<p style='margin: 5px 0 0 0; color: #64748b; font-size: 14px;'>").append(question.getQuestion()).append("</p>");
                html.append("</div>");
            }
            
            html.append("<div style='text-align: center; margin-top: 15px; padding: 15px; background: #e0f2fe; border-radius: 8px;'>");
            html.append("<p style='margin: 0; color: #0277bd; font-weight: 600;'>Questions Answered Correctly: ").append(correctCount).append("/").append(totalQuestions).append("</p>");
            html.append("</div>");
        }
        
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<p style='color: #64748b; font-size: 16px;'>Thank you for using <strong>ExamWizards</strong>!</p>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p style='margin: 0;'>Best regards,<br><strong>ExamWizards Team</strong></p>");
        html.append("<p style='margin: 10px 0 0 0;'><a href='http://localhost:5173'>Visit ExamWizards</a></p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Check if the user's answer is correct
     */
    private boolean isAnswerCorrect(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty() || 
            question.getCorrect_options() == null || question.getCorrect_options().isEmpty()) {
            return false;
        }
        
        if ("mcq".equals(question.getType())) {
            // Single choice
            int correctOptionIndex = question.getCorrect_options().get(0);
            if (question.getOptions() != null && correctOptionIndex < question.getOptions().size()) {
                String correctAnswer = question.getOptions().get(correctOptionIndex).getAvailableOption();
                return correctAnswer != null && correctAnswer.equals(userAnswer.trim());
            }
        } else if ("multiple".equals(question.getType())) {
            // Multiple choice - check if all correct options are selected
            String[] userAnswers = userAnswer.split(",");
            java.util.List<String> userAnswersList = java.util.Arrays.stream(userAnswers)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toList());
            
            java.util.List<String> correctAnswers = question.getCorrect_options().stream()
                .filter(index -> question.getOptions() != null && index < question.getOptions().size())
                .map(index -> question.getOptions().get(index).getAvailableOption())
                .collect(java.util.stream.Collectors.toList());
            
            return correctAnswers.size() == userAnswersList.size() &&
                   correctAnswers.containsAll(userAnswersList);
        }
        
        return false;
    }

    /**
     * Calculate grade based on percentage
     */
    private String calculateGrade(double percentage) {
        if (percentage >= 95) return "A+";
        if (percentage >= 90) return "A";
        if (percentage >= 85) return "B+";
        if (percentage >= 80) return "B";
        if (percentage >= 75) return "C+";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    /**
     * Format time in seconds to readable format
     */
    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    /**
     * Build email verification email body
     */
    private String buildVerificationEmailBody(String email, String verificationUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Verify Your Email</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".content { padding: 30px; }");
        html.append(".button { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }");
        html.append(".footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 20px; font-size: 14px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🔐 Verify Your Email</h1>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<h2 style='color: #1e293b; margin-bottom: 20px;'>Welcome to ExamWizards!</h2>");
        html.append("<p style='color: #64748b; margin-bottom: 25px;'>Thank you for registering with ExamWizards. To complete your registration and start using our platform, please verify your email address by clicking the button below:</p>");
        
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<a href='").append(verificationUrl).append("' class='button' style='color: white;'>Verify Email Address</a>");
        html.append("</div>");
        
        html.append("<p style='color: #64748b; font-size: 14px; margin-top: 30px;'>If the button doesn't work, you can copy and paste this link into your browser:</p>");
        html.append("<p style='color: #667eea; font-size: 14px; word-break: break-all;'>").append(verificationUrl).append("</p>");
        
        html.append("<div style='background: #fef3c7; border: 1px solid #f59e0b; border-radius: 8px; padding: 15px; margin: 20px 0;'>");
        html.append("<p style='color: #92400e; margin: 0; font-size: 14px;'><strong>Important:</strong> This verification link will expire in 24 hours for security reasons.</p>");
        html.append("</div>");
        
        html.append("<p style='color: #64748b; margin-top: 30px;'>If you didn't create an account with ExamWizards, please ignore this email.</p>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p style='margin: 0;'>Best regards,<br><strong>ExamWizards Team</strong></p>");
        html.append("<p style='margin: 10px 0 0 0;'>© 2025 ExamWizards. All rights reserved.</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Build password reset email body
     */
    private String buildPasswordResetEmailBody(String email, String resetUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Reset Your Password</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".content { padding: 30px; }");
        html.append(".button { display: inline-block; background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }");
        html.append(".footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 20px; font-size: 14px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🔑 Reset Your Password</h1>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<h2 style='color: #1e293b; margin-bottom: 20px;'>Password Reset Request</h2>");
        html.append("<p style='color: #64748b; margin-bottom: 25px;'>We received a request to reset your password for your ExamWizards account. Click the button below to create a new password:</p>");
        
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<a href='").append(resetUrl).append("' class='button' style='color: white;'>Reset Password</a>");
        html.append("</div>");
        
        html.append("<p style='color: #64748b; font-size: 14px; margin-top: 30px;'>If the button doesn't work, you can copy and paste this link into your browser:</p>");
        html.append("<p style='color: #ef4444; font-size: 14px; word-break: break-all;'>").append(resetUrl).append("</p>");
        
        html.append("<div style='background: #fef2f2; border: 1px solid #ef4444; border-radius: 8px; padding: 15px; margin: 20px 0;'>");
        html.append("<p style='color: #dc2626; margin: 0; font-size: 14px;'><strong>Security Notice:</strong> This password reset link will expire in 30 minutes for your security.</p>");
        html.append("</div>");
        
        html.append("<p style='color: #64748b; margin-top: 30px;'>If you didn't request a password reset, please ignore this email. Your password will remain unchanged.</p>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p style='margin: 0;'>Best regards,<br><strong>ExamWizards Team</strong></p>");
        html.append("<p style='margin: 10px 0 0 0;'>© 2025 ExamWizards. All rights reserved.</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Build payment receipt email body
     */
    private String buildPaymentReceiptEmailBody(User student, Course course, String transactionId, String orderId, BigDecimal amount) {
        String currentDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Payment Receipt</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #10B981 0%, #059669 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".content { padding: 30px; }");
        html.append(".receipt-card { background: #f8fafc; border-radius: 8px; padding: 20px; margin: 20px 0; border: 2px dashed #10B981; }");
        html.append(".receipt-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #e5e7eb; }");
        html.append(".receipt-row:last-child { border-bottom: none; }");
        html.append(".receipt-label { font-weight: 600; color: #374151; }");
        html.append(".receipt-value { color: #1f2937; }");
        html.append(".amount-highlight { font-size: 24px; font-weight: bold; color: #10B981; }");
        html.append(".success-badge { background: #10B981; color: white; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; display: inline-block; margin: 10px 0; }");
        html.append(".course-info { background: #eff6ff; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #3b82f6; }");
        html.append(".footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 20px; font-size: 14px; }");
        html.append(".footer a { color: #60a5fa; text-decoration: none; }");
        html.append("@media (max-width: 600px) { .receipt-row { flex-direction: column; align-items: flex-start; } .receipt-value { margin-top: 5px; } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🧾 Payment Receipt</h1>");
        html.append("<div class='success-badge'>Payment Successful</div>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<h2 style='color: #1e293b; margin-bottom: 10px;'>Dear ").append(student.getFullName() != null ? student.getFullName() : student.getUsername()).append(",</h2>");
        html.append("<p style='color: #64748b; margin-bottom: 25px;'>Thank you for your payment! Your course enrollment has been confirmed. Here are your payment details:</p>");
        
        // Course Information
        html.append("<div class='course-info'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #1e40af; font-size: 18px;'>📚 Course Details</h3>");
        html.append("<div class='receipt-row'>");
        html.append("<span class='receipt-label'>Course Name:</span>");
        html.append("<span class='receipt-value'>").append(course.getName()).append("</span>");
        html.append("</div>");
        if (course.getDescription() != null && !course.getDescription().trim().isEmpty()) {
            html.append("<div class='receipt-row'>");
            html.append("<span class='receipt-label'>Description:</span>");
            html.append("<span class='receipt-value'>").append(course.getDescription().length() > 100 ? course.getDescription().substring(0, 100) + "..." : course.getDescription()).append("</span>");
            html.append("</div>");
        }
        html.append("<div class='receipt-row'>");
        html.append("<span class='receipt-label'>Instructor:</span>");
        html.append("<span class='receipt-value'>").append(course.getInstructor().getFullName() != null ? course.getInstructor().getFullName() : course.getInstructor().getUsername()).append("</span>");
        html.append("</div>");
        html.append("</div>");
        
        // Payment Receipt
        html.append("<div class='receipt-card'>");
        html.append("<h3 style='margin: 0 0 20px 0; color: #1e293b; font-size: 18px; text-align: center;'>💳 Payment Receipt</h3>");
        
        html.append("<div class='receipt-row'>");
        html.append("<span class='receipt-label'>Transaction ID:</span>");
        html.append("<span class='receipt-value' style='font-family: monospace; font-size: 14px;'>").append(transactionId).append("</span>");
        html.append("</div>");
        
        html.append("<div class='receipt-row'>");
        html.append("<span class='receipt-label'>Order ID:</span>");
        html.append("<span class='receipt-value' style='font-family: monospace; font-size: 14px;'>").append(orderId).append("</span>");
        html.append("</div>");
        
        html.append("<div class='receipt-row'>");
        html.append("<span class='receipt-label'>Payment Date:</span>");
        html.append("<span class='receipt-value'>").append(currentDate).append("</span>");
        html.append("</div>");
        
        html.append("<div class='receipt-row'>");
        html.append("<span class='receipt-label'>Payment Method:</span>");
        html.append("<span class='receipt-value'>Online Payment (Razorpay)</span>");
        html.append("</div>");
        
        html.append("<div class='receipt-row'>");
        html.append("<span class='receipt-label'>Student Email:</span>");
        html.append("<span class='receipt-value'>").append(student.getEmail()).append("</span>");
        html.append("</div>");
        
        html.append("<div class='receipt-row' style='background: #f0fdf4; margin: 15px -20px -20px -20px; padding: 20px; border-radius: 0 0 8px 8px;'>");
        html.append("<span class='receipt-label' style='font-size: 18px;'>Amount Paid:</span>");
        html.append("<span class='amount-highlight'>₹").append(amount.toString()).append("</span>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Next Steps
        html.append("<div style='background: #fef3c7; border: 1px solid #f59e0b; border-radius: 8px; padding: 20px; margin: 25px 0;'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #92400e; font-size: 16px;'>🎯 What's Next?</h3>");
        html.append("<ul style='margin: 0; padding-left: 20px; color: #92400e;'>");
        html.append("<li>You now have full access to the course content</li>");
        html.append("<li>You can start taking exams and assessments</li>");
        html.append("<li>Access your enrolled courses from your dashboard</li>");
        html.append("<li>Keep this receipt for your records</li>");
        html.append("</ul>");
        html.append("</div>");
        
        // Important Notes
        html.append("<div style='background: #eff6ff; border: 1px solid #3b82f6; border-radius: 8px; padding: 15px; margin: 20px 0;'>");
        html.append("<h4 style='margin: 0 0 10px 0; color: #1e40af; font-size: 14px;'>📋 Important Notes:</h4>");
        html.append("<ul style='margin: 0; padding-left: 20px; color: #1e40af; font-size: 14px;'>");
        html.append("<li>This is an auto-generated receipt for your payment</li>");
        html.append("<li>Please save this email for your records</li>");
        html.append("<li>For any queries, contact our support team</li>");
        html.append("<li>Refunds are subject to our terms and conditions</li>");
        html.append("</ul>");
        html.append("</div>");
        
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<p style='color: #64748b; font-size: 16px;'>Thank you for choosing <strong>ExamWizards</strong>!</p>");
        html.append("<p style='color: #64748b; font-size: 14px;'>Happy Learning! 🎓</p>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p style='margin: 0;'>Best regards,<br><strong>ExamWizards Team</strong></p>");
        html.append("<p style='margin: 10px 0 0 0;'><a href='http://localhost:5173'>Visit ExamWizards</a> | <a href='mailto:support@examwizards.com'>Contact Support</a></p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 12px;'>© 2025 ExamWizards. All rights reserved.</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Build contact form notification email body for admin
     */
    private String buildContactFormNotificationEmailBody(ContactMessage contactMessage) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>New Contact Form Submission</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".content { padding: 30px; }");
        html.append(".info-card { background: #f8fafc; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #3b82f6; }");
        html.append(".info-row { display: flex; margin-bottom: 15px; }");
        html.append(".info-label { font-weight: 600; color: #374151; min-width: 100px; margin-right: 15px; }");
        html.append(".info-value { color: #1f2937; flex: 1; }");
        html.append(".message-box { background: #fef3c7; border: 1px solid #f59e0b; border-radius: 8px; padding: 20px; margin: 20px 0; }");
        html.append(".message-content { color: #92400e; white-space: pre-wrap; }");
        html.append(".footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 20px; font-size: 14px; }");
        html.append(".timestamp { color: #6b7280; font-size: 14px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>📧 New Contact Form Submission</h1>");
        html.append("<p style='margin: 10px 0 0 0; opacity: 0.9;'>ExamWizards Contact Form</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<h2 style='color: #1e293b; margin-bottom: 10px;'>Contact Details</h2>");
        html.append("<p style='color: #64748b; margin-bottom: 25px;'>You have received a new message through the ExamWizards contact form:</p>");
        
        // Contact Information Card
        html.append("<div class='info-card'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #1e293b; font-size: 18px;'>📋 Contact Information</h3>");
        
        html.append("<div class='info-row'>");
        html.append("<div class='info-label'>👤 Name:</div>");
        html.append("<div class='info-value'>").append(contactMessage.getName()).append("</div>");
        html.append("</div>");
        
        html.append("<div class='info-row'>");
        html.append("<div class='info-label'>📧 Email:</div>");
        html.append("<div class='info-value'><a href='mailto:").append(contactMessage.getEmail()).append("' style='color: #3b82f6; text-decoration: none;'>").append(contactMessage.getEmail()).append("</a></div>");
        html.append("</div>");
        
        html.append("<div class='info-row'>");
        html.append("<div class='info-label'>📝 Subject:</div>");
        html.append("<div class='info-value'><strong>").append(contactMessage.getSubject()).append("</strong></div>");
        html.append("</div>");
        
        html.append("<div class='info-row'>");
        html.append("<div class='info-label'>🔢 Reference:</div>");
        html.append("<div class='info-value'><strong style='font-family: monospace; background: #e0e7ff; padding: 4px 8px; border-radius: 4px; color: #3730a3;'>").append(contactMessage.getReferenceNumber()).append("</strong></div>");
        html.append("</div>");
        
        html.append("<div class='info-row'>");
        html.append("<div class='info-label'>🕒 Submitted:</div>");
        html.append("<div class='info-value timestamp'>").append(contactMessage.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Message Content
        html.append("<div class='message-box'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #92400e; font-size: 18px;'>💬 Message Content</h3>");
        html.append("<div class='message-content'>").append(contactMessage.getMessage()).append("</div>");
        html.append("</div>");
        
        // Quick Reply Button
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<a href='mailto:").append(contactMessage.getEmail()).append("?subject=Re: ").append(contactMessage.getSubject()).append("' ");
        html.append("style='display: inline-block; background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 10px;'>📧 Reply to ").append(contactMessage.getName()).append("</a>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p style='margin: 0;'>This is an automated notification from <strong>ExamWizards Contact Form</strong></p>");
        html.append("<p style='margin: 10px 0 0 0;'>© 2025 ExamWizards. All rights reserved.</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Build contact form confirmation email body for user
     */
    private String buildContactFormConfirmationEmailBody(ContactMessage contactMessage) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Thank You for Contacting Us</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".content { padding: 30px; }");
        html.append(".message-card { background: #f0fdf4; border: 1px solid #10b981; border-radius: 8px; padding: 20px; margin: 20px 0; }");
        html.append(".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin: 20px 0; }");
        html.append(".info-item { background: #f8fafc; padding: 15px; border-radius: 8px; border-left: 4px solid #667eea; }");
        html.append(".info-label { font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 5px; }");
        html.append(".info-value { font-weight: 600; color: #1e293b; }");
        html.append(".footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 20px; font-size: 14px; }");
        html.append(".footer a { color: #60a5fa; text-decoration: none; }");
        html.append("@media (max-width: 600px) { .info-grid { grid-template-columns: 1fr; } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>✅ Thank You!</h1>");
        html.append("<p style='margin: 10px 0 0 0; opacity: 0.9;'>We've received your message</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<h2 style='color: #1e293b; margin-bottom: 10px;'>Dear ").append(contactMessage.getName()).append(",</h2>");
        html.append("<p style='color: #64748b; margin-bottom: 25px;'>Thank you for reaching out to us! We have successfully received your message and our team will review it shortly.</p>");
        
        // Reference Number Highlight
        html.append("<div style='background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%); border: 2px solid #6366f1; border-radius: 12px; padding: 25px; margin: 25px 0; text-align: center;'>");
        html.append("<h3 style='margin: 0 0 10px 0; color: #3730a3; font-size: 18px;'>🔢 Your Reference Number</h3>");
        html.append("<div style='font-family: monospace; font-size: 24px; font-weight: bold; color: #1e1b4b; background: white; padding: 15px; border-radius: 8px; border: 2px dashed #6366f1; margin: 15px 0;'>").append(contactMessage.getReferenceNumber()).append("</div>");
        html.append("<p style='margin: 10px 0 0 0; color: #4338ca; font-size: 14px;'><strong>Please save this reference number for future correspondence.</strong></p>");
        html.append("</div>");
        
        // Confirmation Message
        html.append("<div class='message-card'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #059669; font-size: 18px;'>📧 Your Message Details</h3>");
        html.append("<div class='info-grid'>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='info-label'>Subject</div>");
        html.append("<div class='info-value'>").append(contactMessage.getSubject()).append("</div>");
        html.append("</div>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='info-label'>Reference Number</div>");
        html.append("<div class='info-value' style='font-family: monospace; font-size: 16px; font-weight: bold; color: #059669;'>").append(contactMessage.getReferenceNumber()).append("</div>");
        html.append("</div>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='info-label'>Submitted</div>");
        html.append("<div class='info-value'>").append(contactMessage.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Message content
        html.append("<div style='margin-top: 20px; padding: 15px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 6px;'>");
        html.append("<div class='info-label'>Your Message</div>");
        html.append("<div style='color: #374151; margin-top: 8px; white-space: pre-wrap;'>").append(contactMessage.getMessage()).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        
        // What happens next
        html.append("<div style='background: #eff6ff; border: 1px solid #3b82f6; border-radius: 8px; padding: 20px; margin: 25px 0;'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #1d4ed8; font-size: 16px;'>🚀 What happens next?</h3>");
        html.append("<ul style='margin: 0; padding-left: 20px; color: #1e40af;'>");
        html.append("<li style='margin-bottom: 8px;'>Our team will review your message within 24 hours</li>");
        html.append("<li style='margin-bottom: 8px;'>You'll receive a personalized response via email</li>");
        html.append("<li style='margin-bottom: 8px;'>For urgent matters, we'll prioritize your request</li>");
        html.append("</ul>");
        html.append("</div>");
        
        // Contact info
        html.append("<div style='background: #f8fafc; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: center;'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #1e293b; font-size: 16px;'>📞 Need immediate assistance?</h3>");
        html.append("<p style='margin: 0; color: #64748b;'>If your matter is urgent, you can also reach us at:</p>");
        html.append("<p style='margin: 10px 0 0 0; color: #667eea; font-weight: 600;'>support@examwizards.com</p>");
        html.append("</div>");
        
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<p style='color: #64748b; font-size: 16px;'>Thank you for choosing <strong>ExamWizards</strong>!</p>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p style='margin: 0;'>Best regards,<br><strong>ExamWizards Support Team</strong></p>");
        html.append("<p style='margin: 10px 0 0 0;'><a href='http://localhost:5173'>Visit ExamWizards</a> | <a href='mailto:support@examwizards.com'>Contact Support</a></p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 12px;'>© 2025 ExamWizards. All rights reserved.</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Build contact status update email body
     */
    private String buildContactStatusUpdateEmailBody(ContactMessage contactMessage, ContactMessageStatus oldStatus) {
        String statusColor = getStatusColor(contactMessage.getStatus());
        String statusIcon = getStatusIcon(contactMessage.getStatus());
        String statusText = getStatusText(contactMessage.getStatus());
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Status Update - Your Inquiry</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".content { padding: 30px; }");
        html.append(".status-card { background: #f8fafc; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid ").append(statusColor).append("; }");
        html.append(".status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; background-color: ").append(statusColor).append("; color: white; }");
        html.append(".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin: 20px 0; }");
        html.append(".info-item { background: #f8fafc; padding: 15px; border-radius: 8px; }");
        html.append(".info-label { font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 5px; }");
        html.append(".info-value { font-weight: 600; color: #1e293b; }");
        html.append(".footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 20px; font-size: 14px; }");
        html.append(".footer a { color: #60a5fa; text-decoration: none; }");
        html.append("@media (max-width: 600px) { .info-grid { grid-template-columns: 1fr; } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>📋 Status Update</h1>");
        html.append("<p style='margin: 10px 0 0 0; opacity: 0.9;'>Your inquiry has been updated</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<h2 style='color: #1e293b; margin-bottom: 10px;'>Dear ").append(contactMessage.getName()).append(",</h2>");
        html.append("<p style='color: #64748b; margin-bottom: 25px;'>We wanted to update you on the status of your recent inquiry. Here are the latest details:</p>");
        
        // Status Update Card
        html.append("<div class='status-card'>");
        html.append("<div style='text-align: center; margin-bottom: 20px;'>");
        html.append("<span class='status-badge'>").append(statusIcon).append(" ").append(statusText).append("</span>");
        html.append("</div>");
        
        html.append("<div class='info-grid'>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='info-label'>Subject</div>");
        html.append("<div class='info-value'>").append(contactMessage.getSubject()).append("</div>");
        html.append("</div>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='info-label'>Reference Number</div>");
        html.append("<div class='info-value' style='font-family: monospace; font-weight: bold; color: #667eea;'>").append(contactMessage.getReferenceNumber()).append("</div>");
        html.append("</div>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='info-label'>Previous Status</div>");
        html.append("<div class='info-value'>").append(getStatusText(oldStatus)).append("</div>");
        html.append("</div>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='info-label'>Current Status</div>");
        html.append("<div class='info-value'>").append(statusText).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");
        
        // Admin Response (if available)
        if (contactMessage.getAdminResponse() != null && !contactMessage.getAdminResponse().trim().isEmpty()) {
            html.append("<div style='background: #f0f9ff; border: 1px solid #0ea5e9; border-radius: 8px; padding: 20px; margin: 25px 0;'>");
            html.append("<h3 style='margin: 0 0 15px 0; color: #0369a1; font-size: 16px;'>💬 Response from our team</h3>");
            html.append("<div style='color: #0c4a6e; white-space: pre-wrap;'>").append(contactMessage.getAdminResponse()).append("</div>");
            html.append("</div>");
        }
        
        // Next steps based on status
        html.append("<div style='background: #fef3c7; border: 1px solid #f59e0b; border-radius: 8px; padding: 20px; margin: 25px 0;'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #92400e; font-size: 16px;'>🔄 What's next?</h3>");
        
        if (contactMessage.getStatus() == ContactMessageStatus.IN_PROGRESS) {
            html.append("<p style='margin: 0; color: #92400e;'>Our team is actively working on your inquiry. We'll keep you updated as we make progress.</p>");
        } else if (contactMessage.getStatus() == ContactMessageStatus.REPLIED) {
            html.append("<p style='margin: 0; color: #92400e;'>We've provided a response to your inquiry. If you have any follow-up questions, please don't hesitate to contact us again.</p>");
        } else {
            html.append("<p style='margin: 0; color: #92400e;'>We'll continue to keep you updated on any changes to your inquiry status.</p>");
        }
        
        html.append("</div>");
        
        // Contact info
        html.append("<div style='background: #f8fafc; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: center;'>");
        html.append("<h3 style='margin: 0 0 15px 0; color: #1e293b; font-size: 16px;'>📞 Need to reach us?</h3>");
        html.append("<p style='margin: 0; color: #64748b;'>If you have any questions about this update, feel free to contact us:</p>");
        html.append("<p style='margin: 10px 0 0 0; color: #667eea; font-weight: 600;'>support@examwizards.com</p>");
        html.append("</div>");
        
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<p style='color: #64748b; font-size: 16px;'>Thank you for your patience!</p>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p style='margin: 0;'>Best regards,<br><strong>ExamWizards Support Team</strong></p>");
        html.append("<p style='margin: 10px 0 0 0;'><a href='http://localhost:5173'>Visit ExamWizards</a> | <a href='mailto:support@examwizards.com'>Contact Support</a></p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 12px;'>© 2025 ExamWizards. All rights reserved.</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Get status color for styling
     */
    private String getStatusColor(ContactMessageStatus status) {
        switch (status) {
            case PENDING:
                return "#f59e0b";
            case IN_PROGRESS:
                return "#3b82f6";
            case REPLIED:
                return "#10b981";
            default:
                return "#6b7280";
        }
    }

    /**
     * Get status icon
     */
    private String getStatusIcon(ContactMessageStatus status) {
        switch (status) {
            case PENDING:
                return "⏳";
            case IN_PROGRESS:
                return "🔄";
            case REPLIED:
                return "✅";
            default:
                return "📋";
        }
    }

    /**
     * Get human-readable status text
     */
    private String getStatusText(ContactMessageStatus status) {
        switch (status) {
            case PENDING:
                return "Pending Review";
            case IN_PROGRESS:
                return "In Progress";
            case REPLIED:
                return "Replied";
            default:
                return status.toString();
        }
    }
}