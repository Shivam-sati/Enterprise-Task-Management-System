package com.taskmanagement.task.service;

import com.taskmanagement.task.dto.CreateTagRequest;
import com.taskmanagement.task.dto.TagResponse;
import com.taskmanagement.task.exception.TagNotFoundException;
import com.taskmanagement.task.mapper.TagMapper;
import com.taskmanagement.task.model.Tag;
import com.taskmanagement.task.repository.TagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TagService {
    
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);
    
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    
    @Autowired
    public TagService(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }
    
    @CacheEvict(value = "tags", key = "#userId")
    public TagResponse createTag(String userId, CreateTagRequest request) {
        logger.info("Creating tag: {} for user: {}", request.getName(), userId);
        
        // Check if tag already exists for user
        if (tagRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("Tag with name '" + request.getName() + "' already exists");
        }
        
        Tag tag = tagMapper.toEntity(request, userId);
        Tag savedTag = tagRepository.save(tag);
        
        logger.info("Created tag: {} with ID: {}", savedTag.getName(), savedTag.getTagId());
        return tagMapper.toResponse(savedTag);
    }
    
    @Cacheable(value = "tags", key = "#userId")
    public List<TagResponse> getTagsByUserId(String userId) {
        logger.debug("Retrieving tags for user: {}", userId);
        
        List<Tag> tags = tagRepository.findByUserIdOrderByUsageCountDesc(userId);
        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public Page<TagResponse> getTagsByUserId(String userId, int page, int size, String sortBy, String sortDirection) {
        logger.debug("Retrieving paginated tags for user: {} (page: {}, size: {})", userId, page, size);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Tag> tagPage = tagRepository.findByUserId(userId, pageable);
        return tagPage.map(tagMapper::toResponse);
    }
    
    public TagResponse getTagById(String tagId, String userId) {
        logger.debug("Retrieving tag: {} for user: {}", tagId, userId);
        
        Tag tag = tagRepository.findByTagId(tagId)
                .orElseThrow(() -> new TagNotFoundException(tagId));
        
        // Verify tag belongs to user
        if (!tag.getUserId().equals(userId)) {
            throw new TagNotFoundException(tagId);
        }
        
        return tagMapper.toResponse(tag);
    }
    
    public List<TagResponse> searchTags(String userId, String searchTerm) {
        logger.debug("Searching tags for user: {} with term: {}", userId, searchTerm);
        
        List<Tag> tags = tagRepository.findByUserIdAndNameContainingIgnoreCase(userId, searchTerm);
        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<TagResponse> getPopularTags(String userId) {
        logger.debug("Retrieving popular tags for user: {}", userId);
        
        List<Tag> tags = tagRepository.findPopularTagsByUserId(userId);
        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @CacheEvict(value = "tags", key = "#userId")
    public TagResponse updateTag(String tagId, String userId, CreateTagRequest request) {
        logger.info("Updating tag: {} for user: {}", tagId, userId);
        
        Tag tag = tagRepository.findByTagId(tagId)
                .orElseThrow(() -> new TagNotFoundException(tagId));
        
        // Verify tag belongs to user
        if (!tag.getUserId().equals(userId)) {
            throw new TagNotFoundException(tagId);
        }
        
        // Check if new name conflicts with existing tag
        if (!tag.getName().equals(request.getName()) && 
            tagRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("Tag with name '" + request.getName() + "' already exists");
        }
        
        tag.setName(request.getName());
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        
        Tag updatedTag = tagRepository.save(tag);
        
        logger.info("Updated tag: {} for user: {}", tagId, userId);
        return tagMapper.toResponse(updatedTag);
    }
    
    @CacheEvict(value = "tags", key = "#userId")
    public void deleteTag(String tagId, String userId) {
        logger.info("Deleting tag: {} for user: {}", tagId, userId);
        
        Tag tag = tagRepository.findByTagId(tagId)
                .orElseThrow(() -> new TagNotFoundException(tagId));
        
        // Verify tag belongs to user
        if (!tag.getUserId().equals(userId)) {
            throw new TagNotFoundException(tagId);
        }
        
        tagRepository.delete(tag);
        
        logger.info("Deleted tag: {} for user: {}", tagId, userId);
    }
    
    @CacheEvict(value = "tags", key = "#userId")
    public void cleanupUnusedTags(String userId) {
        logger.info("Cleaning up unused tags for user: {}", userId);
        
        tagRepository.deleteUnusedTagsByUserId(userId);
        
        logger.info("Cleaned up unused tags for user: {}", userId);
    }
    
    public void incrementTagUsage(String userId, String tagName) {
        tagRepository.findByUserIdAndName(userId, tagName)
                .ifPresent(tag -> {
                    tag.incrementUsage();
                    tagRepository.save(tag);
                });
    }
    
    public void decrementTagUsage(String userId, String tagName) {
        tagRepository.findByUserIdAndName(userId, tagName)
                .ifPresent(tag -> {
                    tag.decrementUsage();
                    tagRepository.save(tag);
                });
    }
    
    public void updateTagUsageCounts(String userId, List<String> oldTags, List<String> newTags) {
        // Decrement usage for removed tags
        if (oldTags != null) {
            oldTags.stream()
                    .filter(tag -> newTags == null || !newTags.contains(tag))
                    .forEach(tag -> decrementTagUsage(userId, tag));
        }
        
        // Increment usage for added tags
        if (newTags != null) {
            newTags.stream()
                    .filter(tag -> oldTags == null || !oldTags.contains(tag))
                    .forEach(tag -> incrementTagUsage(userId, tag));
        }
    }
    
    public long getTagCount(String userId) {
        return tagRepository.countByUserId(userId);
    }
    
    public long getUsedTagCount(String userId) {
        return tagRepository.countUsedTagsByUserId(userId);
    }
}