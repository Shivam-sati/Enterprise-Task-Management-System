package com.taskmanagement.task.service;

import com.taskmanagement.task.dto.CreateTaskRequest;
import com.taskmanagement.task.dto.TaskFilterRequest;
import com.taskmanagement.task.dto.TaskResponse;
import com.taskmanagement.task.dto.UpdateTaskRequest;
import com.taskmanagement.task.exception.TaskNotFoundException;
import com.taskmanagement.task.mapper.TaskMapper;
import com.taskmanagement.task.model.Task;
import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.model.TaskPriority;
import com.taskmanagement.task.repository.TaskRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Autowired
    public TaskService(TaskRepository taskRepository,
                      TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    public TaskResponse createTask(String userId, CreateTaskRequest request) {
        logger.info("Creating task for user: {}", userId);
        Task task = taskMapper.toEntity(request, userId);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse getTaskById(String taskId, String userId) {
        Task task = taskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new TaskNotFoundException(taskId, userId));
        return taskMapper.toResponse(task);
    }

    public Page<TaskResponse> getTasksByUserId(String userId, Pageable pageable) {
        Page<Task> taskPage = taskRepository.findByUserId(userId, pageable);
        return taskPage.map(taskMapper::toResponse);
    }

    public TaskResponse updateTask(String taskId, String userId, UpdateTaskRequest request) {
        Task task = taskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new TaskNotFoundException(taskId, userId));
        taskMapper.updateEntity(task, request);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    public void deleteTask(String taskId, String userId) {
        Task task = taskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new TaskNotFoundException(taskId, userId));
        taskRepository.delete(task);
    }

    public Map<String, Long> getTaskStatistics(String userId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) taskRepository.findByUserId(userId).size());
        return stats;
    }

    // Placeholder methods for controller compatibility
    public Page<TaskResponse> getFilteredTasks(String userId, TaskFilterRequest filterRequest) {
        return getTasksByUserId(userId, PageRequest.of(0, 20));
    }

    public Page<TaskResponse> searchTasks(String userId, String searchText, Pageable pageable) {
        return getTasksByUserId(userId, pageable);
    }

    public TaskResponse updateTaskStatus(String taskId, String userId, TaskStatus status) {
        return updateTask(taskId, userId, new UpdateTaskRequest());
    }

    public TaskResponse addDependency(String taskId, String userId, String dependencyId) {
        return getTaskById(taskId, userId);
    }

    public TaskResponse removeDependency(String taskId, String userId, String dependencyId) {
        return getTaskById(taskId, userId);
    }

    public List<TaskResponse> getOverdueTasks(String userId) {
        return new ArrayList<>();
    }

    public List<TaskResponse> getTasksByStatus(String userId, TaskStatus status) {
        return new ArrayList<>();
    }

    public List<TaskResponse> getTasksByPriority(String userId, TaskPriority priority) {
        return new ArrayList<>();
    }

    public List<TaskResponse> getTasksByTag(String userId, String tag) {
        return new ArrayList<>();
    }

    public List<TaskResponse> getReadyTasks(String userId) {
        return new ArrayList<>();
    }
}