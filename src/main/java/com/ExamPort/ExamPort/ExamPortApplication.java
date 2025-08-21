package com.ExamPort.ExamPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class ExamPortApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(ExamPortApplication.class);

	public static void main(String[] args) {
		logger.info("Starting ExamPort Application...");
		SpringApplication.run(ExamPortApplication.class, args);
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		logger.info("=================================================");
		logger.info("🚀 ExamPort Application Started Successfully!");
		logger.info("🌐 Server running on: http://localhost:8080");
		logger.info("📚 API Documentation: http://localhost:8080/api");
		logger.info("🔐 Authentication endpoints: /api/auth/*");
		logger.info("=================================================");
	}

}
