package com.ExamPort.ExamPort.Entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "courses")
@JsonIgnoreProperties({"exams", "hibernateLazyInitializer", "handler"})
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Course name is required")
    @Size(min = 3, max = 100, message = "Course name must be between 3 and 100 characters")
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_allowed_emails", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "email")
    private List<String> allowedEmails = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Exam> exams = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "instructor_id")
    private User instructor;

    // New fields for course visibility and pricing
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private CourseVisibility visibility = CourseVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private CoursePricing pricing = CoursePricing.FREE;

    @Column(precision = 10, scale = 2)
    @DecimalMin(value = "0.01", message = "Price must be greater than 0 for paid courses")
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getAllowedEmails() { return allowedEmails; }
    public void setAllowedEmails(List<String> allowedEmails) { this.allowedEmails = allowedEmails; }
    public List<Exam> getExams() { return exams; }
    public void setExams(List<Exam> exams) { this.exams = exams; }
    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }
    
    // New getters and setters for visibility and pricing
    public CourseVisibility getVisibility() { return visibility; }
    public void setVisibility(CourseVisibility visibility) { this.visibility = visibility; }
    public CoursePricing getPricing() { return pricing; }
    public void setPricing(CoursePricing pricing) { this.pricing = pricing; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
