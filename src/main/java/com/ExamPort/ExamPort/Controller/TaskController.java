package com.ExamPort.ExamPort.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ExamPort.ExamPort.Entity.Exam;
import com.ExamPort.ExamPort.Service.TaskService;

@RestController
public class TaskController {
	
	@Autowired
	TaskService task;
	
	@GetMapping("/welcome")
	public String welcome() {
		return "the project is working";
	}
	
	@PostMapping("/exam")
	public void AddExam(@RequestBody Exam e) {
		task.AddExam(e);
	}
	
	@GetMapping("/exam")
	public List GetExam() {
		return task.GetExam();
	}

}
