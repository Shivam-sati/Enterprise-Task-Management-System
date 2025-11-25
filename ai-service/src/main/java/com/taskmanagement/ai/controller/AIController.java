package com.taskmanagement.ai.controller;

import com.taskmanagement.ai.service.PythonAIServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final PythonAIServiceClient pythonAIServiceClient;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AI Service (Java Proxy)",
                "message", "AI Service proxy is running successfully"));
    }

    @PostMapping("/parse-task")
    public ResponseEntity<Map<String, Object>> parseTask(@RequestBody Map<String, String> request) {
        String taskText = request.get("text");
        log.info("Received parse task request: {}", taskText);

        try {
            Map<String, Object> response = pythonAIServiceClient.parseTask(request);
            log.info("Successfully processed parse task request");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing parse task request: {}", e.getMessage());
            // The circuit breaker will handle fallback, but if it fails completely, return
            // error
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to process task parsing request", "message", e.getMessage()));
        }
    }

    @PostMapping("/prioritize-tasks")
    public ResponseEntity<Map<String, Object>> prioritizeTasks(@RequestBody Map<String, Object> request) {
        log.info("Received prioritize tasks request");

        try {
            Map<String, Object> response = pythonAIServiceClient.prioritizeTasks(request);
            log.info("Successfully processed prioritize tasks request");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing prioritize tasks request: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to process task prioritization request", "message", e.getMessage()));
        }
    }

    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getProductivityInsights() {
        log.info("Received productivity insights request");

        try {
            Map<String, Object> response = pythonAIServiceClient.getProductivityInsights();
            log.info("Successfully processed productivity insights request");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing productivity insights request: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to process productivity insights request", "message",
                            e.getMessage()));
        }
    }
}