package com.ExamPort.ExamPort.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class TestController {

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from test controller!");
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> testStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeUsers", 150L);
        stats.put("examsCompleted", 1250L);
        stats.put("message", "Test data from TestController");
        return ResponseEntity.ok(stats);
    }
}