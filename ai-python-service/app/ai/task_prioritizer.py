"""
Task Prioritizer using AI models for intelligent task ranking
"""
import logging
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import Enum
import re
import json

logger = logging.getLogger(__name__)


class UrgencyLevel(Enum):
    """Urgency levels for tasks"""
    LOW = 1
    MEDIUM = 2
    HIGH = 3
    CRITICAL = 4


class ImportanceLevel(Enum):
    """Importance levels for tasks"""
    LOW = 1
    MEDIUM = 2
    HIGH = 3
    CRITICAL = 4


@dataclass
class TaskPriority:
    """Priority information for a single task"""
    task_id: str
    priority_score: float
    urgency: UrgencyLevel
    importance: ImportanceLevel
    reasoning: str
    factors: List[str]
    confidence: float


@dataclass
class PrioritizationResult:
    """Result of task prioritization operation"""
    prioritized_tasks: List[TaskPriority]
    reasoning: str
    confidence: float
    factors_considered: List[str]
    total_tasks: int
    processing_time_ms: float


class TaskPrioritizer:
    """
    TaskPrioritizer using AI models for intelligent task ranking
    """
    
    def __init__(self):
        self.urgency_keywords = {
            UrgencyLevel.CRITICAL: ["urgent", "asap", "immediately", "critical", "emergency", "now", "today"],
            UrgencyLevel.HIGH: ["soon", "deadline", "due", "tomorrow", "this week", "important"],
            UrgencyLevel.MEDIUM: ["next week", "upcoming", "scheduled", "planned"],
            UrgencyLevel.LOW: ["later", "when possible", "eventually", "someday", "future"]
        }
        
        self.importance_keywords = {
            ImportanceLevel.CRITICAL: ["critical", "essential", "vital", "must have", "required", "blocker"],
            ImportanceLevel.HIGH: ["important", "significant", "major", "key", "priority"],
            ImportanceLevel.MEDIUM: ["useful", "helpful", "good to have", "nice to have"],
            ImportanceLevel.LOW: ["minor", "optional", "low priority", "if time permits"]
        }
        
        self.complexity_indicators = {
            "high": ["complex", "difficult", "challenging", "research", "investigate", "design", "architect"],
            "medium": ["implement", "develop", "create", "build", "update", "modify"],
            "low": ["fix", "update", "change", "review", "test", "document"]
        }
        
        self.dependency_indicators = ["depends on", "requires", "needs", "after", "before", "blocked by"]
        
        logger.info("TaskPrioritizer initialized with rule-based prioritization")
    
    def prioritize_tasks(self, tasks: List[Dict[str, Any]]) -> PrioritizationResult:
        """
        Prioritize a list of tasks using AI-powered analysis
        
        Args:
            tasks: List of task dictionaries with keys like 'id', 'title', 'description', 'due_date', etc.
            
        Returns:
            PrioritizationResult with prioritized tasks and reasoning
        """
        start_time = datetime.now()
        
        try:
            if not tasks:
                return PrioritizationResult(
                    prioritized_tasks=[],
                    reasoning="No tasks provided for prioritization",
                    confidence=1.0,
                    factors_considered=[],
                    total_tasks=0,
                    processing_time_ms=0.0
                )
            
            # Analyze each task
            task_priorities = []
            for task in tasks:
                priority = self._analyze_task_priority(task)
                task_priorities.append(priority)
            
            # Sort by priority score (descending)
            task_priorities.sort(key=lambda x: x.priority_score, reverse=True)
            
            # Generate overall reasoning
            reasoning = self._generate_prioritization_reasoning(task_priorities)
            
            # Calculate overall confidence
            confidence = self._calculate_overall_confidence(task_priorities)
            
            # Identify factors considered
            factors_considered = self._get_factors_considered(tasks)
            
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            
            result = PrioritizationResult(
                prioritized_tasks=task_priorities,
                reasoning=reasoning,
                confidence=confidence,
                factors_considered=factors_considered,
                total_tasks=len(tasks),
                processing_time_ms=processing_time
            )
            
            logger.info(f"Prioritized {len(tasks)} tasks in {processing_time:.2f}ms")
            return result
            
        except Exception as e:
            logger.error(f"Task prioritization failed: {str(e)}")
            # Return fallback result
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            return PrioritizationResult(
                prioritized_tasks=[
                    TaskPriority(
                        task_id=str(i),
                        priority_score=0.5,
                        urgency=UrgencyLevel.MEDIUM,
                        importance=ImportanceLevel.MEDIUM,
                        reasoning="Fallback prioritization due to error",
                        factors=["error"],
                        confidence=0.1
                    ) for i, task in enumerate(tasks)
                ],
                reasoning="Prioritization failed, using fallback ordering",
                confidence=0.1,
                factors_considered=["error"],
                total_tasks=len(tasks),
                processing_time_ms=processing_time
            )
    
    def _analyze_task_priority(self, task: Dict[str, Any]) -> TaskPriority:
        """Analyze priority for a single task"""
        task_id = str(task.get('id', task.get('task_id', 'unknown')))
        title = task.get('title', '')
        description = task.get('description', '')
        due_date = task.get('due_date')
        
        # Combine text for analysis
        full_text = f"{title} {description}".lower()
        
        # Analyze urgency
        urgency = self._analyze_urgency(full_text, due_date)
        
        # Analyze importance
        importance = self._analyze_importance(full_text)
        
        # Analyze complexity
        complexity = self._analyze_complexity(full_text)
        
        # Check for dependencies
        has_dependencies = self._check_dependencies(full_text)
        
        # Calculate priority score
        priority_score = self._calculate_priority_score(urgency, importance, complexity, has_dependencies)
        
        # Generate reasoning
        reasoning = self._generate_task_reasoning(urgency, importance, complexity, has_dependencies)
        
        # Identify factors
        factors = self._identify_factors(urgency, importance, complexity, has_dependencies, due_date)
        
        # Calculate confidence
        confidence = self._calculate_task_confidence(title, description, due_date)
        
        return TaskPriority(
            task_id=task_id,
            priority_score=priority_score,
            urgency=urgency,
            importance=importance,
            reasoning=reasoning,
            factors=factors,
            confidence=confidence
        )
    
    def _analyze_urgency(self, text: str, due_date: Optional[str] = None) -> UrgencyLevel:
        """Analyze urgency level from text and due date"""
        # Check due date first
        if due_date:
            try:
                if isinstance(due_date, str):
                    due = datetime.fromisoformat(due_date.replace('Z', '+00:00'))
                else:
                    due = due_date
                
                now = datetime.now()
                days_until_due = (due - now).days
                
                if days_until_due < 0:
                    return UrgencyLevel.CRITICAL  # Overdue
                elif days_until_due == 0:
                    return UrgencyLevel.CRITICAL  # Due today
                elif days_until_due == 1:
                    return UrgencyLevel.HIGH  # Due tomorrow
                elif days_until_due <= 3:
                    return UrgencyLevel.HIGH  # Due this week
                elif days_until_due <= 7:
                    return UrgencyLevel.MEDIUM  # Due next week
                
            except (ValueError, TypeError):
                pass  # Invalid date format, fall back to text analysis
        
        # Analyze text for urgency keywords
        urgency_scores = {}
        for level, keywords in self.urgency_keywords.items():
            score = sum(1 for keyword in keywords if keyword in text)
            if score > 0:
                urgency_scores[level] = score
        
        if urgency_scores:
            return max(urgency_scores.keys(), key=lambda x: urgency_scores[x])
        
        return UrgencyLevel.MEDIUM
    
    def _analyze_importance(self, text: str) -> ImportanceLevel:
        """Analyze importance level from text"""
        importance_scores = {}
        for level, keywords in self.importance_keywords.items():
            score = sum(1 for keyword in keywords if keyword in text)
            if score > 0:
                importance_scores[level] = score
        
        if importance_scores:
            return max(importance_scores.keys(), key=lambda x: importance_scores[x])
        
        return ImportanceLevel.MEDIUM
    
    def _analyze_complexity(self, text: str) -> str:
        """Analyze task complexity from text"""
        complexity_scores = {}
        for level, keywords in self.complexity_indicators.items():
            score = sum(1 for keyword in keywords if keyword in text)
            if score > 0:
                complexity_scores[level] = score
        
        if complexity_scores:
            return max(complexity_scores.keys(), key=lambda x: complexity_scores[x])
        
        # Estimate based on text length
        word_count = len(text.split())
        if word_count > 50:
            return "high"
        elif word_count > 20:
            return "medium"
        else:
            return "low"
    
    def _check_dependencies(self, text: str) -> bool:
        """Check if task has dependencies"""
        return any(indicator in text for indicator in self.dependency_indicators)
    
    def _calculate_priority_score(self, urgency: UrgencyLevel, importance: ImportanceLevel, 
                                complexity: str, has_dependencies: bool) -> float:
        """Calculate overall priority score (0.0 to 1.0)"""
        # Base score from urgency and importance (Eisenhower Matrix)
        urgency_weight = urgency.value / 4.0  # 0.25 to 1.0
        importance_weight = importance.value / 4.0  # 0.25 to 1.0
        
        # Eisenhower Matrix calculation
        base_score = (urgency_weight * 0.6) + (importance_weight * 0.4)
        
        # Adjust for complexity (higher complexity = slightly lower priority for quick wins)
        complexity_adjustment = {
            "low": 0.1,    # Boost for quick wins
            "medium": 0.0,  # No adjustment
            "high": -0.05   # Slight penalty for complex tasks
        }
        base_score += complexity_adjustment.get(complexity, 0.0)
        
        # Adjust for dependencies (dependencies lower priority)
        if has_dependencies:
            base_score -= 0.1
        
        # Ensure score is between 0 and 1
        return max(0.0, min(1.0, base_score))
    
    def _generate_task_reasoning(self, urgency: UrgencyLevel, importance: ImportanceLevel, 
                               complexity: str, has_dependencies: bool) -> str:
        """Generate reasoning for task prioritization"""
        reasons = []
        
        if urgency == UrgencyLevel.CRITICAL:
            reasons.append("Critical urgency due to deadline or keywords")
        elif urgency == UrgencyLevel.HIGH:
            reasons.append("High urgency indicated")
        
        if importance == ImportanceLevel.CRITICAL:
            reasons.append("Critical importance for project success")
        elif importance == ImportanceLevel.HIGH:
            reasons.append("High importance identified")
        
        if complexity == "low":
            reasons.append("Low complexity allows for quick completion")
        elif complexity == "high":
            reasons.append("High complexity requires careful planning")
        
        if has_dependencies:
            reasons.append("Has dependencies that may affect scheduling")
        
        if not reasons:
            reasons.append("Standard priority based on available information")
        
        return "; ".join(reasons)
    
    def _identify_factors(self, urgency: UrgencyLevel, importance: ImportanceLevel, 
                         complexity: str, has_dependencies: bool, due_date: Optional[str]) -> List[str]:
        """Identify factors that influenced prioritization"""
        factors = []
        
        if due_date:
            factors.append("due_date")
        
        if urgency != UrgencyLevel.MEDIUM:
            factors.append("urgency")
        
        if importance != ImportanceLevel.MEDIUM:
            factors.append("importance")
        
        factors.append("complexity")
        
        if has_dependencies:
            factors.append("dependencies")
        
        return factors
    
    def _calculate_task_confidence(self, title: str, description: str, due_date: Optional[str]) -> float:
        """Calculate confidence in task prioritization"""
        confidence = 0.5  # Base confidence
        
        # Boost confidence based on available information
        if title and len(title) > 5:
            confidence += 0.1
        
        if description and len(description) > 10:
            confidence += 0.1
        
        if due_date:
            confidence += 0.2
        
        # Boost confidence if we found specific keywords
        text = f"{title} {description}".lower()
        keyword_found = False
        
        for keywords in self.urgency_keywords.values():
            if any(keyword in text for keyword in keywords):
                keyword_found = True
                break
        
        if not keyword_found:
            for keywords in self.importance_keywords.values():
                if any(keyword in text for keyword in keywords):
                    keyword_found = True
                    break
        
        if keyword_found:
            confidence += 0.1
        
        return max(0.0, min(1.0, confidence))
    
    def _generate_prioritization_reasoning(self, task_priorities: List[TaskPriority]) -> str:
        """Generate overall reasoning for the prioritization"""
        if not task_priorities:
            return "No tasks to prioritize"
        
        high_priority_count = sum(1 for tp in task_priorities if tp.priority_score > 0.7)
        medium_priority_count = sum(1 for tp in task_priorities if 0.3 <= tp.priority_score <= 0.7)
        low_priority_count = sum(1 for tp in task_priorities if tp.priority_score < 0.3)
        
        reasoning_parts = []
        
        if high_priority_count > 0:
            reasoning_parts.append(f"{high_priority_count} high-priority tasks identified")
        
        if medium_priority_count > 0:
            reasoning_parts.append(f"{medium_priority_count} medium-priority tasks")
        
        if low_priority_count > 0:
            reasoning_parts.append(f"{low_priority_count} lower-priority tasks")
        
        reasoning = "Prioritization based on urgency, importance, and complexity. " + "; ".join(reasoning_parts)
        
        return reasoning
    
    def _calculate_overall_confidence(self, task_priorities: List[TaskPriority]) -> float:
        """Calculate overall confidence in prioritization"""
        if not task_priorities:
            return 1.0
        
        # Average individual task confidences
        avg_confidence = sum(tp.confidence for tp in task_priorities) / len(task_priorities)
        
        # Boost confidence if we have good spread of priorities
        scores = [tp.priority_score for tp in task_priorities]
        score_range = max(scores) - min(scores) if scores else 0
        
        if score_range > 0.3:  # Good differentiation
            avg_confidence += 0.1
        
        return max(0.0, min(1.0, avg_confidence))
    
    def _get_factors_considered(self, tasks: List[Dict[str, Any]]) -> List[str]:
        """Get list of factors that were considered in prioritization"""
        factors = ["urgency", "importance", "complexity"]
        
        # Check if any tasks have due dates
        if any(task.get('due_date') for task in tasks):
            factors.append("due_date")
        
        # Check if any tasks mention dependencies
        has_dependencies = False
        for task in tasks:
            text = f"{task.get('title', '')} {task.get('description', '')}".lower()
            if any(indicator in text for indicator in self.dependency_indicators):
                has_dependencies = True
                break
        
        if has_dependencies:
            factors.append("dependencies")
        
        return factors
    
    def to_dict(self, result: PrioritizationResult) -> Dict[str, Any]:
        """Convert PrioritizationResult to dictionary"""
        return {
            "prioritized_tasks": [
                {
                    "task_id": tp.task_id,
                    "priority_score": tp.priority_score,
                    "urgency": tp.urgency.name.lower(),
                    "importance": tp.importance.name.lower(),
                    "reasoning": tp.reasoning,
                    "factors": tp.factors,
                    "confidence": tp.confidence
                }
                for tp in result.prioritized_tasks
            ],
            "reasoning": result.reasoning,
            "confidence": result.confidence,
            "factors_considered": result.factors_considered,
            "total_tasks": result.total_tasks,
            "processing_time_ms": result.processing_time_ms
        }
    
    def prioritize_single_task_list(self, task_descriptions: List[str]) -> PrioritizationResult:
        """
        Prioritize a simple list of task descriptions
        
        Args:
            task_descriptions: List of task description strings
            
        Returns:
            PrioritizationResult with prioritized tasks
        """
        # Convert descriptions to task dictionaries
        tasks = []
        for i, description in enumerate(task_descriptions):
            tasks.append({
                'id': i,
                'title': description,
                'description': description
            })
        
        return self.prioritize_tasks(tasks)