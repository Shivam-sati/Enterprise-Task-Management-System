package com.taskmanagement.task.repository;

import com.taskmanagement.task.model.Task;
import com.taskmanagement.task.model.TaskStatus;
import com.taskmanagement.task.model.TaskPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    
    // Basic queries
    Optional<Task> findByTaskId(String taskId);
    
    Optional<Task> findByTaskIdAndUserId(String taskId, String userId);
    
    List<Task> findByUserId(String userId);
    
    Page<Task> findByUserId(String userId, Pageable pageable);
    
    // Status-based queries
    List<Task> findByUserIdAndStatus(String userId, TaskStatus status);
    
    Page<Task> findByUserIdAndStatus(String userId, TaskStatus status, Pageable pageable);
    
    List<Task> findByUserIdAndStatusIn(String userId, List<TaskStatus> statuses);
    
    // Priority-based queries
    List<Task> findByUserIdAndPriority(String userId, TaskPriority priority);
    
    Page<Task> findByUserIdAndPriority(String userId, TaskPriority priority, Pageable pageable);
    
    List<Task> findByUserIdAndPriorityIn(String userId, List<TaskPriority> priorities);
    
    // Tag-based queries
    List<Task> findByUserIdAndTagsContaining(String userId, String tag);
    
    Page<Task> findByUserIdAndTagsContaining(String userId, String tag, Pageable pageable);
    
    @Query("{ 'userId': ?0, 'tags': { $in: ?1 } }")
    List<Task> findByUserIdAndTagsIn(String userId, List<String> tags);
    
    // Date-based queries
    List<Task> findByUserIdAndDueDateBefore(String userId, LocalDateTime date);
    
    List<Task> findByUserIdAndDueDateBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Task> findByUserIdAndDueDateIsNull(String userId);
    
    // Overdue tasks
    @Query("{ 'userId': ?0, 'dueDate': { $lt: ?1 }, 'status': { $nin: ['COMPLETED', 'CANCELLED'] } }")
    List<Task> findOverdueTasks(String userId, LocalDateTime currentDate);
    
    // Recurring tasks
    List<Task> findByUserIdAndIsRecurringTrue(String userId);
    
    @Query("{ 'isRecurring': true, 'status': 'COMPLETED', 'dueDate': { $lt: ?0 } }")
    List<Task> findCompletedRecurringTasksBeforeDate(LocalDateTime date);
    
    // Dependency queries
    @Query("{ 'dependencies': ?0 }")
    List<Task> findTasksWithDependency(String taskId);
    
    @Query("{ 'userId': ?0, 'dependencies': { $size: 0 } }")
    List<Task> findTasksWithoutDependencies(String userId);
    
    // Complex filtering queries
    @Query("{ 'userId': ?0, " +
           "$and: [" +
           "  { $or: [ { 'status': { $exists: false } }, { 'status': { $in: ?1 } } ] }," +
           "  { $or: [ { 'priority': { $exists: false } }, { 'priority': { $in: ?2 } } ] }," +
           "  { $or: [ { 'tags': { $exists: false } }, { 'tags': { $in: ?3 } } ] }" +
           "] }")
    Page<Task> findByUserIdWithFilters(String userId, 
                                      List<TaskStatus> statuses, 
                                      List<TaskPriority> priorities, 
                                      List<String> tags, 
                                      Pageable pageable);
    
    // Statistics queries
    @Query(value = "{ 'userId': ?0, 'status': ?1 }", count = true)
    long countByUserIdAndStatus(String userId, TaskStatus status);
    
    @Query(value = "{ 'userId': ?0, 'priority': ?1 }", count = true)
    long countByUserIdAndPriority(String userId, TaskPriority priority);
    
    @Query(value = "{ 'userId': ?0, 'completedAt': { $gte: ?1, $lt: ?2 } }", count = true)
    long countCompletedTasksBetweenDates(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Text search (will be enhanced with Atlas Search)
    @Query("{ 'userId': ?0, $text: { $search: ?1 } }")
    List<Task> findByUserIdAndTextSearch(String userId, String searchText);
    
    // Cleanup queries
    @Query(value = "{ 'status': 'COMPLETED', 'completedAt': { $lt: ?0 } }", delete = true)
    void deleteCompletedTasksOlderThan(LocalDateTime date);
    
    // Existence checks
    boolean existsByTaskId(String taskId);
    
    boolean existsByTaskIdAndUserId(String taskId, String userId);
}