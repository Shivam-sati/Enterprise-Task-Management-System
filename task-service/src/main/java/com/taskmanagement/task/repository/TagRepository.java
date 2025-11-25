package com.taskmanagement.task.repository;

import com.taskmanagement.task.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {
    
    // Basic queries
    Optional<Tag> findByTagId(String tagId);
    
    Optional<Tag> findByUserIdAndName(String userId, String name);
    
    List<Tag> findByUserId(String userId);
    
    Page<Tag> findByUserId(String userId, Pageable pageable);
    
    // Name-based queries
    List<Tag> findByUserIdAndNameContainingIgnoreCase(String userId, String namePattern);
    
    @Query("{ 'userId': ?0, 'name': { $in: ?1 } }")
    List<Tag> findByUserIdAndNameIn(String userId, List<String> names);
    
    // Usage-based queries
    List<Tag> findByUserIdOrderByUsageCountDesc(String userId);
    
    @Query(value = "{ 'userId': ?0, 'usageCount': { $gt: 0 } }", sort = "{ 'usageCount': -1 }")
    List<Tag> findPopularTagsByUserId(String userId);
    
    @Query(value = "{ 'userId': ?0, 'usageCount': 0 }")
    List<Tag> findUnusedTagsByUserId(String userId);
    
    // Statistics queries
    @Query(value = "{ 'userId': ?0 }", count = true)
    long countByUserId(String userId);
    
    @Query(value = "{ 'userId': ?0, 'usageCount': { $gt: 0 } }", count = true)
    long countUsedTagsByUserId(String userId);
    
    // Cleanup queries
    @Query(value = "{ 'userId': ?0, 'usageCount': 0 }", delete = true)
    void deleteUnusedTagsByUserId(String userId);
    
    // Existence checks
    boolean existsByTagId(String tagId);
    
    boolean existsByUserIdAndName(String userId, String name);
}