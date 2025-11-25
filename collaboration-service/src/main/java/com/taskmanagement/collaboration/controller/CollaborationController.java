package com.taskmanagement.collaboration.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collaboration")
@RequiredArgsConstructor
@Slf4j
public class CollaborationController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Collaboration Service",
            "message", "Collaboration Service is running successfully"
        ));
    }

    @PostMapping("/share-task")
    public ResponseEntity<Map<String, Object>> shareTask(@RequestBody Map<String, Object> request) {
        String taskId = (String) request.get("taskId");
        String userId = (String) request.get("userId");
        String permission = (String) request.get("permission");
        
        log.info("Sharing task {} with user {} with permission {}", taskId, userId, permission);
        
        return ResponseEntity.ok(Map.of(
            "shareId", "share_" + System.currentTimeMillis(),
            "taskId", taskId,
            "sharedWith", userId,
            "permission", permission,
            "sharedAt", LocalDateTime.now(),
            "status", "success"
        ));
    }

    @GetMapping("/shared-tasks/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getSharedTasks(@PathVariable String userId) {
        log.info("Getting shared tasks for user: {}", userId);
        
        return ResponseEntity.ok(List.of(
            Map.of(
                "taskId", "task_1",
                "title", "Review Project Proposal",
                "sharedBy", "john.doe@example.com",
                "permission", "EDIT",
                "sharedAt", LocalDateTime.now().minusDays(1)
            ),
            Map.of(
                "taskId", "task_2", 
                "title", "Team Meeting Preparation",
                "sharedBy", "jane.smith@example.com",
                "permission", "VIEW",
                "sharedAt", LocalDateTime.now().minusDays(2)
            )
        ));
    }

    @GetMapping("/team-members/{taskId}")
    public ResponseEntity<List<Map<String, Object>>> getTeamMembers(@PathVariable String taskId) {
        log.info("Getting team members for task: {}", taskId);
        
        return ResponseEntity.ok(List.of(
            Map.of(
                "userId", "user_1",
                "email", "john.doe@example.com",
                "name", "John Doe",
                "permission", "OWNER",
                "joinedAt", LocalDateTime.now().minusDays(5)
            ),
            Map.of(
                "userId", "user_2",
                "email", "jane.smith@example.com", 
                "name", "Jane Smith",
                "permission", "EDIT",
                "joinedAt", LocalDateTime.now().minusDays(3)
            )
        ));
    }

    @GetMapping("/activity-feed/{taskId}")
    public ResponseEntity<List<Map<String, Object>>> getActivityFeed(@PathVariable String taskId) {
        log.info("Getting activity feed for task: {}", taskId);
        
        return ResponseEntity.ok(List.of(
            Map.of(
                "id", "activity_1",
                "type", "TASK_UPDATED",
                "user", "John Doe",
                "message", "Updated task description",
                "timestamp", LocalDateTime.now().minusHours(2)
            ),
            Map.of(
                "id", "activity_2",
                "type", "COMMENT_ADDED",
                "user", "Jane Smith",
                "message", "Added a comment: Great progress!",
                "timestamp", LocalDateTime.now().minusHours(4)
            ),
            Map.of(
                "id", "activity_3",
                "type", "TASK_SHARED",
                "user", "John Doe",
                "message", "Shared task with team",
                "timestamp", LocalDateTime.now().minusDays(1)
            )
        ));
    }

    @PostMapping("/comments/{taskId}")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request) {
        
        String comment = request.get("comment");
        String userId = request.get("userId");
        
        log.info("Adding comment to task {}: {}", taskId, comment);
        
        return ResponseEntity.ok(Map.of(
            "commentId", "comment_" + System.currentTimeMillis(),
            "taskId", taskId,
            "userId", userId,
            "comment", comment,
            "createdAt", LocalDateTime.now(),
            "status", "success"
        ));
    }
}