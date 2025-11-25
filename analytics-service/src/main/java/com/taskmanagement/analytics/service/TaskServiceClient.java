package com.taskmanagement.analytics.service;

import com.taskmanagement.analytics.dto.TaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceClient {

    private final RestTemplate restTemplate;
    
    @Value("${app.task-service.url:http://task-service:8081}")
    private String taskServiceUrl;

    public List<TaskDto> getTasksForUser(String userId, Period period) {
        try {
            LocalDateTime fromDate = LocalDateTime.now().minus(period);
            
            String url = UriComponentsBuilder.fromHttpUrl(taskServiceUrl)
                    .path("/api/tasks/user/{userId}")
                    .queryParam("fromDate", fromDate.toString())
                    .buildAndExpand(userId)
                    .toUriString();
            
            log.debug("Fetching tasks from: {}", url);
            
            ResponseEntity<List<TaskDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TaskDto>>() {}
            );
            
            List<TaskDto> tasks = response.getBody();
            log.info("Retrieved {} tasks for user: {}", tasks != null ? tasks.size() : 0, userId);
            
            return tasks != null ? tasks : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error fetching tasks for user: {} from task service", userId, e);
            return new ArrayList<>();
        }
    }

    public List<TaskDto> getAllTasksForUser(String userId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(taskServiceUrl)
                    .path("/api/tasks/user/{userId}")
                    .buildAndExpand(userId)
                    .toUriString();
            
            log.debug("Fetching all tasks from: {}", url);
            
            ResponseEntity<List<TaskDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TaskDto>>() {}
            );
            
            List<TaskDto> tasks = response.getBody();
            log.info("Retrieved {} total tasks for user: {}", tasks != null ? tasks.size() : 0, userId);
            
            return tasks != null ? tasks : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error fetching all tasks for user: {} from task service", userId, e);
            return new ArrayList<>();
        }
    }

    public boolean isTaskServiceHealthy() {
        try {
            String healthUrl = taskServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Task service health check failed", e);
            return false;
        }
    }
}