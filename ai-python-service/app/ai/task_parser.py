"""
Task Parser using lightweight language models for natural language processing
"""
import re
import logging
from typing import Dict, Any, Optional, List
from dataclasses import dataclass
from enum import Enum
import json

logger = logging.getLogger(__name__)


class Priority(Enum):
    """Task priority levels"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    URGENT = "urgent"


@dataclass
class TaskParseResult:
    """Result of task parsing operation"""
    title: str
    description: str
    priority: Priority
    estimated_hours: float
    tags: List[str]
    confidence: float
    category: Optional[str] = None


class TaskParser:
    """
    TaskParser class using lightweight language models for natural language processing
    """
    
    def __init__(self):
        self.priority_keywords = {
            Priority.URGENT: ["urgent", "asap", "immediately", "critical", "emergency", "now"],
            Priority.HIGH: ["important", "high", "priority", "soon", "deadline", "due"],
            Priority.MEDIUM: ["medium", "normal", "regular", "standard"],
            Priority.LOW: ["low", "later", "when possible", "eventually", "someday"]
        }
        
        self.time_patterns = [
            (r'(\d+)\s*hours?', lambda m: float(m.group(1))),
            (r'(\d+)\s*hrs?', lambda m: float(m.group(1))),
            (r'(\d+)\s*h', lambda m: float(m.group(1))),
            (r'(\d+)\s*minutes?', lambda m: float(m.group(1)) / 60),
            (r'(\d+)\s*mins?', lambda m: float(m.group(1)) / 60),
            (r'(\d+)\s*days?', lambda m: float(m.group(1)) * 8),
            (r'(\d+)\s*weeks?', lambda m: float(m.group(1)) * 40),
            (r'half\s*hour', lambda m: 0.5),
            (r'quarter\s*hour', lambda m: 0.25),
        ]
        
        self.category_keywords = {
            "development": ["code", "develop", "program", "implement", "build", "create", "fix", "debug"],
            "meeting": ["meeting", "call", "discuss", "review", "standup", "sync"],
            "documentation": ["document", "write", "readme", "spec", "guide", "manual"],
            "testing": ["test", "qa", "verify", "validate", "check"],
            "research": ["research", "investigate", "analyze", "study", "explore"],
            "planning": ["plan", "design", "architect", "strategy", "roadmap"],
            "maintenance": ["update", "upgrade", "maintain", "refactor", "cleanup"]
        }
        
        logger.info("TaskParser initialized with rule-based processing")
    
    def parse_task(self, text: str) -> TaskParseResult:
        """
        Parse natural language task description into structured data
        
        Args:
            text: Natural language task description
            
        Returns:
            TaskParseResult with extracted information
        """
        try:
            # Clean and normalize input
            cleaned_text = self._clean_text(text)
            
            # Extract title (first sentence or up to 100 chars)
            title = self._extract_title(cleaned_text)
            
            # Extract priority
            priority = self._extract_priority(cleaned_text)
            
            # Extract time estimate
            estimated_hours = self._extract_time_estimate(cleaned_text)
            
            # Extract tags
            tags = self._extract_tags(cleaned_text)
            
            # Extract category
            category = self._extract_category(cleaned_text)
            
            # Calculate confidence based on extracted information
            confidence = self._calculate_confidence(text, title, priority, estimated_hours, tags, category)
            
            result = TaskParseResult(
                title=title,
                description=cleaned_text,
                priority=priority,
                estimated_hours=estimated_hours,
                tags=tags,
                confidence=confidence,
                category=category
            )
            
            logger.debug(f"Parsed task: {title} (confidence: {confidence:.2f})")
            return result
            
        except Exception as e:
            logger.error(f"Task parsing failed: {str(e)}")
            # Return fallback result
            return TaskParseResult(
                title=text[:50] + "..." if len(text) > 50 else text,
                description=text,
                priority=Priority.MEDIUM,
                estimated_hours=1.0,
                tags=["unparsed"],
                confidence=0.1,
                category=None
            )
    
    def _clean_text(self, text: str) -> str:
        """Clean and normalize input text"""
        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text.strip())
        
        # Remove common prefixes
        prefixes = ["todo:", "task:", "do:", "need to:", "should:", "must:"]
        text_lower = text.lower()
        for prefix in prefixes:
            if text_lower.startswith(prefix):
                text = text[len(prefix):].strip()
                break
        
        return text
    
    def _extract_title(self, text: str) -> str:
        """Extract task title from text"""
        # Try to find first sentence
        sentences = re.split(r'[.!?]+', text)
        if sentences and len(sentences[0].strip()) > 0:
            title = sentences[0].strip()
        else:
            title = text
        
        # Limit title length
        if len(title) > 100:
            title = title[:97] + "..."
        
        # Capitalize first letter
        if title:
            title = title[0].upper() + title[1:]
        
        return title or "Untitled Task"
    
    def _extract_priority(self, text: str) -> Priority:
        """Extract priority from text based on keywords"""
        text_lower = text.lower()
        
        # Count matches for each priority level
        priority_scores = {}
        for priority, keywords in self.priority_keywords.items():
            score = sum(1 for keyword in keywords if keyword in text_lower)
            if score > 0:
                priority_scores[priority] = score
        
        # Return highest scoring priority, default to medium
        if priority_scores:
            return max(priority_scores.keys(), key=lambda p: priority_scores[p])
        
        return Priority.MEDIUM
    
    def _extract_time_estimate(self, text: str) -> float:
        """Extract time estimate from text"""
        text_lower = text.lower()
        
        # Try to match time patterns
        for pattern, converter in self.time_patterns:
            match = re.search(pattern, text_lower)
            if match:
                try:
                    return converter(match)
                except (ValueError, AttributeError):
                    continue
        
        # Estimate based on text length and complexity
        word_count = len(text.split())
        
        if word_count < 5:
            return 0.5  # Very short task
        elif word_count < 15:
            return 1.0  # Short task
        elif word_count < 30:
            return 2.0  # Medium task
        elif word_count < 50:
            return 4.0  # Long task
        else:
            return 8.0  # Very long task
    
    def _extract_tags(self, text: str) -> List[str]:
        """Extract relevant tags from text"""
        tags = []
        text_lower = text.lower()
        
        # Look for hashtags
        hashtags = re.findall(r'#(\w+)', text)
        tags.extend(hashtags)
        
        # Look for action words
        action_words = ["create", "update", "delete", "fix", "implement", "design", "test", "review"]
        for word in action_words:
            if word in text_lower:
                tags.append(word)
        
        # Look for technology keywords
        tech_keywords = ["api", "database", "frontend", "backend", "ui", "ux", "mobile", "web"]
        for keyword in tech_keywords:
            if keyword in text_lower:
                tags.append(keyword)
        
        # Remove duplicates and limit to 5 tags
        tags = list(set(tags))[:5]
        
        return tags
    
    def _extract_category(self, text: str) -> Optional[str]:
        """Extract task category from text"""
        text_lower = text.lower()
        
        category_scores = {}
        for category, keywords in self.category_keywords.items():
            score = sum(1 for keyword in keywords if keyword in text_lower)
            if score > 0:
                category_scores[category] = score
        
        if category_scores:
            return max(category_scores.keys(), key=lambda c: category_scores[c])
        
        return None
    
    def _calculate_confidence(self, original_text: str, title: str, priority: Priority, 
                            estimated_hours: float, tags: List[str], category: Optional[str]) -> float:
        """Calculate confidence score for the parsing result"""
        confidence = 0.5  # Base confidence
        
        # Boost confidence based on extracted information quality
        if len(title) > 5 and title != "Untitled Task":
            confidence += 0.1
        
        if priority != Priority.MEDIUM:  # Non-default priority found
            confidence += 0.1
        
        if estimated_hours != 1.0:  # Non-default time estimate
            confidence += 0.1
        
        if tags:
            confidence += min(0.2, len(tags) * 0.05)
        
        if category:
            confidence += 0.1
        
        # Reduce confidence for very short or very long inputs
        word_count = len(original_text.split())
        if word_count < 3:
            confidence -= 0.2
        elif word_count > 100:
            confidence -= 0.1
        
        # Ensure confidence is between 0 and 1
        return max(0.0, min(1.0, confidence))
    
    def parse_multiple_tasks(self, text: str) -> List[TaskParseResult]:
        """
        Parse multiple tasks from a single text input
        
        Args:
            text: Text containing multiple tasks (separated by newlines or bullets)
            
        Returns:
            List of TaskParseResult objects
        """
        try:
            # Split by common separators
            separators = ['\n', '•', '*', '-', '1.', '2.', '3.', '4.', '5.']
            
            # Start with the full text
            tasks = [text]
            
            # Split by each separator
            for separator in separators:
                new_tasks = []
                for task in tasks:
                    if separator in task:
                        parts = task.split(separator)
                        new_tasks.extend([part.strip() for part in parts if part.strip()])
                    else:
                        new_tasks.append(task)
                tasks = new_tasks
            
            # Parse each task
            results = []
            for task_text in tasks:
                if len(task_text.strip()) > 3:  # Minimum task length
                    result = self.parse_task(task_text.strip())
                    results.append(result)
            
            return results if results else [self.parse_task(text)]
            
        except Exception as e:
            logger.error(f"Multiple task parsing failed: {str(e)}")
            return [self.parse_task(text)]
    
    def to_dict(self, result: TaskParseResult) -> Dict[str, Any]:
        """Convert TaskParseResult to dictionary"""
        return {
            "title": result.title,
            "description": result.description,
            "priority": result.priority.value,
            "estimated_hours": result.estimated_hours,
            "tags": result.tags,
            "confidence": result.confidence,
            "category": result.category
        }
    
    def from_dict(self, data: Dict[str, Any]) -> TaskParseResult:
        """Create TaskParseResult from dictionary"""
        return TaskParseResult(
            title=data.get("title", ""),
            description=data.get("description", ""),
            priority=Priority(data.get("priority", "medium")),
            estimated_hours=data.get("estimated_hours", 1.0),
            tags=data.get("tags", []),
            confidence=data.get("confidence", 0.5),
            category=data.get("category")
        )