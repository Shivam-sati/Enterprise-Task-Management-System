package com.taskmanagement.task.controller;

import com.taskmanagement.task.dto.CreateTagRequest;
import com.taskmanagement.task.dto.TagResponse;
import com.taskmanagement.task.service.TagService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    
    private static final Logger logger = LoggerFactory.getLogger(TagController.class);
    
    private final TagService tagService;
    
    @Autowired
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }
    
    @PostMapping
    public ResponseEntity<TagResponse> createTag(
            @Valid @RequestBody CreateTagRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Creating tag: {} for user: {}", request.getName(), userId);
        
        TagResponse response = tagService.createTag(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<TagResponse>> getTags(Authentication authentication) {
        String userId = authentication.getName();
        logger.debug("Getting tags for user: {}", userId);
        
        List<TagResponse> response = tagService.getTagsByUserId(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paginated")
    public ResponseEntity<Page<TagResponse>> getTagsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "usageCount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting paginated tags for user: {} (page: {}, size: {})", userId, page, size);
        
        Page<TagResponse> response = tagService.getTagsByUserId(userId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{tagId}")
    public ResponseEntity<TagResponse> getTag(
            @PathVariable String tagId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Getting tag: {} for user: {}", tagId, userId);
        
        TagResponse response = tagService.getTagById(tagId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<TagResponse>> searchTags(
            @RequestParam String q,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("Searching tags for user: {} with query: {}", userId, q);
        
        List<TagResponse> response = tagService.searchTags(userId, q);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/popular")
    public ResponseEntity<List<TagResponse>> getPopularTags(Authentication authentication) {
        String userId = authentication.getName();
        logger.debug("Getting popular tags for user: {}", userId);
        
        List<TagResponse> response = tagService.getPopularTags(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{tagId}")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable String tagId,
            @Valid @RequestBody CreateTagRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Updating tag: {} for user: {}", tagId, userId);
        
        TagResponse response = tagService.updateTag(tagId, userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable String tagId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Deleting tag: {} for user: {}", tagId, userId);
        
        tagService.deleteTag(tagId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupUnusedTags(Authentication authentication) {
        String userId = authentication.getName();
        logger.info("Cleaning up unused tags for user: {}", userId);
        
        tagService.cleanupUnusedTags(userId);
        return ResponseEntity.noContent().build();
    }
}