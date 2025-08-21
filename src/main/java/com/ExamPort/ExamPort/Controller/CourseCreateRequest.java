package com.ExamPort.ExamPort.Controller;

import com.ExamPort.ExamPort.Entity.CourseVisibility;
import com.ExamPort.ExamPort.Entity.CoursePricing;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for course creation with visibility and pricing options
 */
public class CourseCreateRequest {
    
    @NotBlank(message = "Course name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Course visibility is required")
    private CourseVisibility visibility;
    
    @NotNull(message = "Course pricing is required")
    private CoursePricing pricing;
    
    @DecimalMin(value = "0.01", message = "Price must be greater than 0 for paid courses")
    private BigDecimal price;
    
    // Default constructor
    public CourseCreateRequest() {}
    
    // Constructor
    public CourseCreateRequest(String name, String description, CourseVisibility visibility, 
                              CoursePricing pricing, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.pricing = pricing;
        this.price = price;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public CourseVisibility getVisibility() {
        return visibility;
    }
    
    public void setVisibility(CourseVisibility visibility) {
        this.visibility = visibility;
    }
    
    public CoursePricing getPricing() {
        return pricing;
    }
    
    public void setPricing(CoursePricing pricing) {
        this.pricing = pricing;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    /**
     * Validate the request data
     * @return validation error message or null if valid
     */
    public String validate() {
        if (pricing == CoursePricing.PAID && (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
            return "Price is required and must be greater than 0 for paid courses";
        }
        
        if (pricing == CoursePricing.FREE && price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            return "Price should not be set for free courses";
        }
        
        if (visibility == CourseVisibility.PRIVATE && pricing == CoursePricing.PAID) {
            return "Private courses cannot be paid courses";
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        return "CourseCreateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", visibility=" + visibility +
                ", pricing=" + pricing +
                ", price=" + price +
                '}';
    }
}