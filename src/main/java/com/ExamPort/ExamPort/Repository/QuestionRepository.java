package com.ExamPort.ExamPort.Repository;

import com.ExamPort.ExamPort.Entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
