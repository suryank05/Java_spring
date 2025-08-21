package com.ExamPort.ExamPort.Entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "result")
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String answers;
    
    @Column(name = "attempt_date")
    private LocalDateTime attemptDate;
    
    @Column(columnDefinition = "TEXT")
    private String feedback;
    
    @Column
    private Boolean passed;
    
    @Column
    private Double score;
    
    @Column(name = "time_taken")
    private Integer timeTaken; // in seconds
    
    @Column(name = "user_rank")
    private Integer userRank;
    
    @Column(name = "exam_exam_id")
    private Long examExamId;
    
    @Column(name = "user_id")
    private Long userId;
    
    // Constructors
    public Result() {
        this.attemptDate = LocalDateTime.now();
    }
    
    public Result(String answers, Long examExamId, Long userId, Double score, Integer timeTaken) {
        this();
        this.answers = answers;
        this.examExamId = examExamId;
        this.userId = userId;
        this.score = score;
        this.timeTaken = timeTaken;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAnswers() {
        return answers;
    }
    
    public void setAnswers(String answers) {
        this.answers = answers;
    }
    
    public LocalDateTime getAttemptDate() {
        return attemptDate;
    }
    
    public void setAttemptDate(LocalDateTime attemptDate) {
        this.attemptDate = attemptDate;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public Boolean getPassed() {
        return passed;
    }
    
    public void setPassed(Boolean passed) {
        this.passed = passed;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public Integer getTimeTaken() {
        return timeTaken;
    }
    
    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }
    
    public Integer getUserRank() {
        return userRank;
    }
    
    public void setUserRank(Integer userRank) {
        this.userRank = userRank;
    }
    
    public Long getExamExamId() {
        return examExamId;
    }
    
    public void setExamExamId(Long examExamId) {
        this.examExamId = examExamId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    @Override
    public String toString() {
        return "Result{" +
                "id=" + id +
                ", examExamId=" + examExamId +
                ", userId=" + userId +
                ", score=" + score +
                ", passed=" + passed +
                ", attemptDate=" + attemptDate +
                '}';
    }
}