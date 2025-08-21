package com.ExamPort.ExamPort.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ExamPort.ExamPort.Entity.Exam;


public interface Exam_repo extends JpaRepository<Exam, Long>{
    // Find exams by instructor id (via course)
    List<Exam> findByCourse_Instructor_Id(Long instructorId);
    
    // Find exams by course id
    List<Exam> findByCourse_Id(Long courseId);
    
    // Find exams by course id (alternative naming)
    List<Exam> findByCourseId(Long courseId);
}
