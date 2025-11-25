package com.taskmanagement.task.repository;

import com.taskmanagement.task.model.Subtask;
import com.taskmanagement.task.model.SubtaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtaskRepository extends MongoRepository<Subtask, String> {
    
    // Basic queries
    Optional<Subtask> findBySubtaskId(String subtaskId);
    
    List<Subtask> findByTaskId(String taskId);
    
    List<Subtask> findByTaskIdOrderByOrder(String taskId);
    
    // Status-based queries
    List<Subtask> findByTaskIdAndStatus(String taskId, SubtaskStatus status);
    
    List<Subtask> findByTaskIdAndStatusOrderByOrder(String taskId, SubtaskStatus status);
    
    // Statistics queries
    @Query(value = "{ 'taskId': ?0, 'status': ?1 }", count = true)
    long countByTaskIdAndStatus(String taskId, SubtaskStatus status);
    
    @Query(value = "{ 'taskId': ?0 }", count = true)
    long countByTaskId(String taskId);
    
    // Completion tracking
    @Query("{ 'taskId': ?0, 'status': 'COMPLETED' }")
    List<Subtask> findCompletedSubtasksByTaskId(String taskId);
    
    @Query("{ 'taskId': ?0, 'status': 'TODO' }")
    List<Subtask> findPendingSubtasksByTaskId(String taskId);
    
    // Order management
    @Query("{ 'taskId': ?0, 'order': { $gte: ?1 } }")
    List<Subtask> findByTaskIdAndOrderGreaterThanEqual(String taskId, int order);
    
    @Query(value = "{ 'taskId': ?0 }", sort = "{ 'order': -1 }")
    Optional<Subtask> findTopByTaskIdOrderByOrderDesc(String taskId);
    
    // Cleanup
    void deleteByTaskId(String taskId);
    
    // Existence checks
    boolean existsBySubtaskId(String subtaskId);
    
    boolean existsByTaskIdAndOrder(String taskId, int order);
}