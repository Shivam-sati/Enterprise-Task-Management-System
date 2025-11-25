package com.taskmanagement.task.service;

import com.taskmanagement.task.dto.CreateSubtaskRequest;
import com.taskmanagement.task.dto.SubtaskResponse;
import com.taskmanagement.task.exception.SubtaskNotFoundException;
import com.taskmanagement.task.exception.TaskNotFoundException;
import com.taskmanagement.task.mapper.SubtaskMapper;
import com.taskmanagement.task.model.Subtask;
import com.taskmanagement.task.model.SubtaskStatus;
import com.taskmanagement.task.repository.SubtaskRepository;
import com.taskmanagement.task.repository.TaskRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubtaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubtaskService.class);
    
    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;
    private final SubtaskMapper subtaskMapper;
    
    @Autowired
    public SubtaskService(SubtaskRepository subtaskRepository, 
                         TaskRepository taskRepository,
                         SubtaskMapper subtaskMapper) {
        this.subtaskRepository = subtaskRepository;
        this.taskRepository = taskRepository;
        this.subtaskMapper = subtaskMapper;
    }
    
    @CacheEvict(value = "tasks", key = "#taskId")
    public SubtaskResponse createSubtask(String taskId, String userId, CreateSubtaskRequest request) {
        logger.info("Creating subtask for task: {} by user: {}", taskId, userId);
        
        // Verify task exists and belongs to user
        if (!taskRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new TaskNotFoundException(taskId, userId);
        }
        
        // Determine order
        int order = request.getOrder() != null ? request.getOrder() : getNextOrder(taskId);
        
        // Create subtask
        Subtask subtask = subtaskMapper.toEntity(request, taskId, order);
        Subtask savedSubtask = subtaskRepository.save(subtask);
        
        logger.info("Created subtask: {} for task: {}", savedSubtask.getSubtaskId(), taskId);
        return subtaskMapper.toResponse(savedSubtask);
    }
    
    @Cacheable(value = "subtasks", key = "#taskId")
    public List<SubtaskResponse> getSubtasksByTaskId(String taskId, String userId) {
        logger.debug("Retrieving subtasks for task: {} by user: {}", taskId, userId);
        
        // Verify task exists and belongs to user
        if (!taskRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new TaskNotFoundException(taskId, userId);
        }
        
        List<Subtask> subtasks = subtaskRepository.findByTaskIdOrderByOrder(taskId);
        return subtasks.stream()
                .map(subtaskMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @CacheEvict(value = {"tasks", "subtasks"}, key = "#taskId")
    public SubtaskResponse updateSubtaskStatus(String subtaskId, String taskId, String userId, SubtaskStatus status) {
        logger.info("Updating subtask status: {} to {} for task: {} by user: {}", subtaskId, status, taskId, userId);
        
        // Verify task exists and belongs to user
        if (!taskRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new TaskNotFoundException(taskId, userId);
        }
        
        Subtask subtask = subtaskRepository.findBySubtaskId(subtaskId)
                .orElseThrow(() -> new SubtaskNotFoundException(subtaskId, taskId));
        
        // Verify subtask belongs to the task
        if (!subtask.getTaskId().equals(taskId)) {
            throw new SubtaskNotFoundException(subtaskId, taskId);
        }
        
        subtask.setStatus(status);
        Subtask updatedSubtask = subtaskRepository.save(subtask);
        
        logger.info("Updated subtask status: {} to {}", subtaskId, status);
        return subtaskMapper.toResponse(updatedSubtask);
    }
    
    @CacheEvict(value = {"tasks", "subtasks"}, key = "#taskId")
    public void deleteSubtask(String subtaskId, String taskId, String userId) {
        logger.info("Deleting subtask: {} from task: {} by user: {}", subtaskId, taskId, userId);
        
        // Verify task exists and belongs to user
        if (!taskRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new TaskNotFoundException(taskId, userId);
        }
        
        Subtask subtask = subtaskRepository.findBySubtaskId(subtaskId)
                .orElseThrow(() -> new SubtaskNotFoundException(subtaskId, taskId));
        
        // Verify subtask belongs to the task
        if (!subtask.getTaskId().equals(taskId)) {
            throw new SubtaskNotFoundException(subtaskId, taskId);
        }
        
        subtaskRepository.delete(subtask);
        
        // Reorder remaining subtasks
        reorderSubtasks(taskId, subtask.getOrder());
        
        logger.info("Deleted subtask: {} from task: {}", subtaskId, taskId);
    }
    
    @CacheEvict(value = {"tasks", "subtasks"}, key = "#taskId")
    public void deleteAllSubtasksByTaskId(String taskId) {
        logger.info("Deleting all subtasks for task: {}", taskId);
        subtaskRepository.deleteByTaskId(taskId);
    }
    
    public long getCompletedSubtaskCount(String taskId) {
        return subtaskRepository.countByTaskIdAndStatus(taskId, SubtaskStatus.COMPLETED);
    }
    
    public long getTotalSubtaskCount(String taskId) {
        return subtaskRepository.countByTaskId(taskId);
    }
    
    public boolean areAllSubtasksCompleted(String taskId) {
        long total = getTotalSubtaskCount(taskId);
        if (total == 0) {
            return true; // No subtasks means all are "completed"
        }
        long completed = getCompletedSubtaskCount(taskId);
        return completed == total;
    }
    
    @CacheEvict(value = {"tasks", "subtasks"}, key = "#taskId")
    public List<SubtaskResponse> reorderSubtasks(String taskId, List<String> subtaskIds) {
        logger.info("Reordering subtasks for task: {}", taskId);
        
        List<Subtask> subtasks = subtaskRepository.findByTaskIdOrderByOrder(taskId);
        
        for (int i = 0; i < subtaskIds.size(); i++) {
            String subtaskId = subtaskIds.get(i);
            Subtask subtask = subtasks.stream()
                    .filter(s -> s.getSubtaskId().equals(subtaskId))
                    .findFirst()
                    .orElseThrow(() -> new SubtaskNotFoundException(subtaskId, taskId));
            
            subtask.setOrder(i);
            subtaskRepository.save(subtask);
        }
        
        List<Subtask> reorderedSubtasks = subtaskRepository.findByTaskIdOrderByOrder(taskId);
        return reorderedSubtasks.stream()
                .map(subtaskMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    private int getNextOrder(String taskId) {
        return subtaskRepository.findTopByTaskIdOrderByOrderDesc(taskId)
                .map(subtask -> subtask.getOrder() + 1)
                .orElse(0);
    }
    
    private void reorderSubtasks(String taskId, int deletedOrder) {
        List<Subtask> subtasksToReorder = subtaskRepository.findByTaskIdAndOrderGreaterThanEqual(taskId, deletedOrder);
        
        for (Subtask subtask : subtasksToReorder) {
            subtask.setOrder(subtask.getOrder() - 1);
            subtaskRepository.save(subtask);
        }
    }
}