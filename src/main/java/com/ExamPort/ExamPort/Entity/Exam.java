package com.ExamPort.ExamPort.Entity;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Entity
@Table(name = "exam")
public class Exam {
    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnoreProperties({"exams"})
    private Course course;
    // --- New fields for full exam creation support ---
    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column
    private String startDate;

    @Column
    private String startTime;

    @Column
    private String endDate;

    @Column
    private String endTime;

    @Column(length = 2000)
    private String instructions;

    @Column
    private int totalMarks;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long Exam_id;
	
    @Column(nullable = false)
    private int duration;

    @Column(nullable=false)
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="ex_id")
    private List<Question> questions = new ArrayList<>();

    @LastModifiedBy
    private String Exam_modifier;

    private boolean isactive;

    public Exam() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and setters for new fields ---
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public String getEndDate() {
        return endDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    public String getInstructions() {
        return instructions;
    }
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    public int getTotalMarks() {
        return totalMarks;
    }
    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

    public Exam(long exam_id, int duration, List<Question> questions, String exam_modifier,
                boolean isactive) {
        super();
        Exam_id = exam_id;
        this.duration = duration;
        this.questions = questions;
        Exam_modifier = exam_modifier;
        this.isactive = isactive;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("exam_id")
    public long getExam_id() {
        return Exam_id;
    }

    public void setExam_id(long exam_id) {
        Exam_id = exam_id;
    }

    public Course getCourse() {
        return course;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }


	public List<Question> getQuestions() {
		return questions;
	}


	public void setQuestions(List<Question> questions) {
		this.questions = questions;
	}


	public String getExam_modifier() {
		return Exam_modifier;
	}


	public void setExam_modifier(String exam_modifier) {
		Exam_modifier = exam_modifier;
	}


	public boolean isIsactive() {
		return isactive;
	}


	public void setIsactive(boolean isactive) {
		this.isactive = isactive;
	}

    public void setCourse(Course course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "Exam [Exam_id=" + Exam_id + ", duration=" + duration + ", Exam_modifier="
                + Exam_modifier + ", isactive=" + isactive + "]";
    }
}
