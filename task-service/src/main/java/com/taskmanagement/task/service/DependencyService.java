package com.taskmanagement.task.service;

import com.taskmanagement.task.exception.CircularDependencyException;
import com.taskmanagement.task.model.Task;
import com.taskmanagement.task.repository.TaskRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DependencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyService.class);
    
    private final TaskRepository taskRepository;
    
    @Autowired
    public DependencyService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    
    /**
     * Validates that adding a dependency won't create a circular dependency
     */
    public void validateDependency(String taskId, String dependencyId) {
        logger.debug("Validating dependency: {} -> {}", taskId, dependencyId);
        
        if (taskId.equals(dependencyId)) {
            throw new CircularDependencyException("Task cannot depend on itself: " + taskId);
        }
        
        if (wouldCreateCircularDependency(taskId, dependencyId)) {
            throw new CircularDependencyException(taskId, dependencyId);
        }
    }
    
    /**
     * Validates all dependencies for a task to ensure no circular dependencies exist
     */
    public void validateAllDependencies(String taskId, List<String> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }
        
        logger.debug("Validating all dependencies for task: {}", taskId);
        
        for (String dependencyId : dependencies) {
            validateDependency(taskId, dependencyId);
        }
    }
    
    /**
     * Checks if adding a dependency would create a circular dependency
     */
    private boolean wouldCreateCircularDependency(String taskId, String dependencyId) {
        // Get the dependency task
        Optional<Task> dependencyTask = taskRepository.findByTaskId(dependencyId);
        if (dependencyTask.isEmpty()) {
            return false; // Dependency doesn't exist, no circular dependency possible
        }
        
        // Use DFS to check if taskId is reachable from dependencyId
        Set<String> visited = new HashSet<>();
        return hasPathTo(dependencyId, taskId, visited);
    }
    
    /**
     * Uses DFS to check if there's a path from source to target through dependencies
     */
    private boolean hasPathTo(String source, String target, Set<String> visited) {
        if (source.equals(target)) {
            return true;
        }
        
        if (visited.contains(source)) {
            return false; // Already visited this node
        }
        
        visited.add(source);
        
        Optional<Task> sourceTask = taskRepository.findByTaskId(source);
        if (sourceTask.isEmpty() || sourceTask.get().getDependencies() == null) {
            return false;
        }
        
        // Check all dependencies of the source task
        for (String dependency : sourceTask.get().getDependencies()) {
            if (hasPathTo(dependency, target, visited)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets all tasks that depend on the given task (reverse dependencies)
     */
    public List<Task> getTasksDependingOn(String taskId) {
        logger.debug("Finding tasks that depend on: {}", taskId);
        return taskRepository.findTasksWithDependency(taskId);
    }
    
    /**
     * Gets the dependency chain for a task (all tasks it depends on, recursively)
     */
    public List<String> getDependencyChain(String taskId) {
        logger.debug("Getting dependency chain for task: {}", taskId);
        
        List<String> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        buildDependencyChain(taskId, chain, visited);
        
        return chain;
    }
    
    private void buildDependencyChain(String taskId, List<String> chain, Set<String> visited) {
        if (visited.contains(taskId)) {
            return; // Avoid infinite loops
        }
        
        visited.add(taskId);
        
        Optional<Task> task = taskRepository.findByTaskId(taskId);
        if (task.isEmpty() || task.get().getDependencies() == null) {
            return;
        }
        
        for (String dependency : task.get().getDependencies()) {
            if (!chain.contains(dependency)) {
                chain.add(dependency);
                buildDependencyChain(dependency, chain, visited);
            }
        }
    }
    
    /**
     * Checks if a task can be completed (all dependencies are completed)
     */
    public boolean canTaskBeCompleted(String taskId, String userId) {
        logger.debug("Checking if task can be completed: {}", taskId);
        
        Optional<Task> task = taskRepository.findByTaskIdAndUserId(taskId, userId);
        if (task.isEmpty()) {
            return false;
        }
        
        List<String> dependencies = task.get().getDependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            return true; // No dependencies, can be completed
        }
        
        // Check if all dependencies are completed
        for (String dependencyId : dependencies) {
            Optional<Task> dependencyTask = taskRepository.findByTaskIdAndUserId(dependencyId, userId);
            if (dependencyTask.isEmpty() || !dependencyTask.get().getStatus().isCompleted()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets all tasks that are ready to be worked on (no pending dependencies)
     */
    public List<Task> getReadyTasks(String userId) {
        logger.debug("Getting ready tasks for user: {}", userId);
        
        List<Task> allTasks = taskRepository.findByUserId(userId);
        List<Task> readyTasks = new ArrayList<>();
        
        for (Task task : allTasks) {
            if (task.getStatus().isActive() && canTaskBeCompleted(task.getTaskId(), userId)) {
                readyTasks.add(task);
            }
        }
        
        return readyTasks;
    }
    
    /**
     * Removes a task from all dependency lists when it's deleted
     */
    public void removeDependencyReferences(String taskId) {
        logger.info("Removing dependency references to deleted task: {}", taskId);
        
        List<Task> dependentTasks = getTasksDependingOn(taskId);
        
        for (Task task : dependentTasks) {
            task.removeDependency(taskId);
            taskRepository.save(task);
        }
        
        logger.info("Removed {} dependency references to task: {}", dependentTasks.size(), taskId);
    }
}