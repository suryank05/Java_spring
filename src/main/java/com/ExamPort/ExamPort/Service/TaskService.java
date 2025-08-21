package com.ExamPort.ExamPort.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ExamPort.ExamPort.Entity.Exam;
import com.ExamPort.ExamPort.Repository.Exam_repo;
import com.ExamPort.ExamPort.Entity.Question;
import com.ExamPort.ExamPort.Repository.QuestionRepository;

@Service
public class TaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
	
	
	@Autowired
	Exam_repo exam;
	@Autowired
	QuestionRepository questionRepository;

	public void AddExam(Exam e) {
        logger.info("Adding new exam: {}", e.getTitle());
        
        try {
            // Save questions first to ensure correct_options are persisted
            if (e.getQuestions() != null) {
                logger.debug("Processing {} questions for exam: {}", e.getQuestions().size(), e.getTitle());
                
                for (Question q : e.getQuestions()) {
                    // Patch: Set option_number for each option based on its index
                    if (q.getOptions() != null) {
                        for (int i = 0; i < q.getOptions().size(); i++) {
                            q.getOptions().get(i).setOption_number(i);
                        }
                        logger.debug("Question processed with {} options: {}", q.getOptions().size(), q.getQuestion());
                    }
                    questionRepository.save(q);
                }
            }
            
            exam.save(e);
            logger.info("Exam saved successfully: {} with {} questions", e.getTitle(), 
                       e.getQuestions() != null ? e.getQuestions().size() : 0);
        } catch (Exception ex) {
            logger.error("Error adding exam: {}", e.getTitle(), ex);
            throw ex;
        }
    }





	public List<Exam> GetExam() {
        logger.info("Fetching all exams");
        
        try {
            List<Exam> exams = exam.findAll();
            logger.info("Retrieved {} exams", exams.size());
            return exams;
        } catch (Exception e) {
            logger.error("Error fetching exams", e);
            throw e;
        }
	}
	

	// TODO: Exams are now filtered by course.allowedEmails. Implement course-based filtering if needed.

	public void deleteExam(Long id) {
        logger.info("Deleting exam with ID: {}", id);
        
        try {
            if (exam.existsById(id)) {
                exam.deleteById(id);
                logger.info("Exam deleted successfully with ID: {}", id);
            } else {
                logger.warn("Attempted to delete non-existent exam with ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting exam with ID: {}", id, e);
            throw e;
        }
	}
	
	public Exam updateExam(Exam e) {
        logger.info("Updating exam: {}", e.getTitle());
        
        try {
            // Update questions if they exist
            if (e.getQuestions() != null) {
                logger.debug("Processing {} questions for exam update: {}", e.getQuestions().size(), e.getTitle());
                
                for (Question q : e.getQuestions()) {
                    // Set option_number for each option based on its index
                    if (q.getOptions() != null) {
                        for (int i = 0; i < q.getOptions().size(); i++) {
                            q.getOptions().get(i).setOption_number(i);
                        }
                        logger.debug("Question updated with {} options: {}", q.getOptions().size(), q.getQuestion());
                    }
                }
            }
            
            Exam savedExam = exam.save(e);
            logger.info("Exam updated successfully: {} with {} questions", savedExam.getTitle(), 
                       savedExam.getQuestions() != null ? savedExam.getQuestions().size() : 0);
            return savedExam;
        } catch (Exception ex) {
            logger.error("Error updating exam: {}", e.getTitle(), ex);
            throw ex;
        }
    }
}
