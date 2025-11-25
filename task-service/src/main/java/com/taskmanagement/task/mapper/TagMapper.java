package com.taskmanagement.task.mapper;

import com.taskmanagement.task.dto.CreateTagRequest;
import com.taskmanagement.task.dto.TagResponse;
import com.taskmanagement.task.model.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {
    
    public Tag toEntity(CreateTagRequest request, String userId) {
        Tag tag = new Tag(userId, request.getName());
        
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
        
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        
        return tag;
    }
    
    public TagResponse toResponse(Tag tag) {
        TagResponse response = new TagResponse();
        
        response.setTagId(tag.getTagId());
        response.setName(tag.getName());
        response.setDescription(tag.getDescription());
        response.setColor(tag.getColor());
        response.setUsageCount(tag.getUsageCount());
        response.setCreatedAt(tag.getCreatedAt());
        response.setUpdatedAt(tag.getUpdatedAt());
        
        return response;
    }
}