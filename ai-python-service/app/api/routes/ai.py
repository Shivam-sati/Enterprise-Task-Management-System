"""
AI processing endpoints with real AI functionality
"""
from fastapi import APIRouter, HTTPException, status, Depends
from pydantic import BaseModel, Field, validator
from typing import List, Optional, Dict, Any
import logging
import asyncio
from datetime import datetime
import os

from app.ai.task_parser import TaskParser, Priority
from app.ai.task_prioritizer import TaskPrioritizer, UrgencyLevel, ImportanceLevel
from app.ai.insight_generator import InsightGenerator
from app.api.error_handlers import (
    AIServiceError, ProcessingTimeoutError, InvalidInputError, InsufficientDataError,
    timeout_handler, validate_task_data, validate_prioritization_tasks, validate_parse_request
)

logger = logging.getLogger(__name__)
router = APIRouter()

# Configuration
DEFAULT_TIMEOUT = float(os.getenv("AI_PROCESSING_TIMEOUT", "30.0"))
MAX_CONCURRENT_REQUESTS = int(os.getenv("MAX_CONCURRENT_REQUESTS", "10"))

# Initialize AI processors
task_parser = TaskParser()
task_prioritizer = TaskPrioritizer()
insight_generator = InsightGenerator()

# Semaphore for controlling concurrent requests
request_semaphore = asyncio.Semaphore(MAX_CONCURRENT_REQUESTS)

# Request Models
class TaskParseRequest(BaseModel):
    """Request model for task parsing"""
    text: str = Field(..., min_length=1, max_length=2000, description="Natural language task description")
    
    @validator('text')
    def validate_text(cls, v):
        if not v.strip():
            raise ValueError('Task text cannot be empty')
        return v.strip()

class TaskForPrioritization(BaseModel):
    """Individual task for prioritization"""
    id: str = Field(..., description="Unique task identifier")
    title: str = Field(..., min_length=1, max_length=500, description="Task title")
    description: Optional[str] = Field(None, max_length=2000, description="Task description")
    due_date: Optional[str] = Field(None, description="Due date in ISO format")
    category: Optional[str] = Field(None, max_length=100, description="Task category")
    priority: Optional[str] = Field(None, description="Current priority level")

class TaskPrioritizationRequest(BaseModel):
    """Request model for task prioritization"""
    tasks: List[TaskForPrioritization] = Field(..., min_items=1, max_items=50, description="List of tasks to prioritize")
    
    @validator('tasks')
    def validate_tasks(cls, v):
        if not v:
            raise ValueError('At least one task is required')
        return v

class InsightsRequest(BaseModel):
    """Request model for productivity insights"""
    user_id: Optional[str] = Field(None, description="User identifier")
    task_data: Dict[str, Any] = Field(..., description="Task history and statistics")
    period_days: Optional[int] = Field(30, ge=1, le=365, description="Analysis period in days")

# Response Models
class TaskParseResponse(BaseModel):
    """Response model for task parsing"""
    title: str
    description: str
    priority: str
    estimated_hours: float
    tags: List[str]
    confidence: float
    category: Optional[str] = None
    processing_time_ms: float

class TaskPriorityResponse(BaseModel):
    """Individual task priority in response"""
    task_id: str
    priority_score: float
    urgency: str
    importance: str
    reasoning: str
    factors: List[str]
    confidence: float

class TaskPrioritizationResponse(BaseModel):
    """Response model for task prioritization"""
    prioritized_tasks: List[TaskPriorityResponse]
    reasoning: str
    confidence: float
    factors_considered: List[str]
    total_tasks: int
    processing_time_ms: float

class ProductivityInsightResponse(BaseModel):
    """Individual productivity insight"""
    insight: str
    category: str
    confidence: float
    recommendation: Optional[str] = None

class ProductivityPatternResponse(BaseModel):
    """Detected productivity pattern"""
    pattern_type: str
    description: str
    strength: float
    trend: str

class InsightsResponse(BaseModel):
    """Response model for productivity insights"""
    insights: List[str]
    recommendations: List[str]
    detailed_insights: List[ProductivityInsightResponse]
    detected_patterns: List[ProductivityPatternResponse]
    confidence: float
    analysis_period: str
    data_quality: str
    processing_time_ms: float

# API Endpoints
@router.post("/parse-task", response_model=TaskParseResponse)
async def parse_task(request: TaskParseRequest):
    """
    Parse natural language task description into structured data
    
    This endpoint uses AI models to extract:
    - Task title and description
    - Priority level
    - Time estimation
    - Relevant tags and category
    
    Raises:
        InvalidInputError: If input text is invalid
        ProcessingTimeoutError: If processing takes too long
        AIServiceError: If AI processing fails
    """
    start_time = datetime.now()
    
    async with request_semaphore:
        try:
            # Validate input
            validate_parse_request(request.text)
            
            logger.info(f"Parsing task: {request.text[:50]}...")
            
            # Run parsing with timeout
            async def parse_with_timeout():
                loop = asyncio.get_event_loop()
                return await loop.run_in_executor(None, task_parser.parse_task, request.text)
            
            result = await timeout_handler(parse_with_timeout(), DEFAULT_TIMEOUT)
            
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            
            response = TaskParseResponse(
                title=result.title,
                description=result.description,
                priority=result.priority.value,
                estimated_hours=result.estimated_hours,
                tags=result.tags,
                confidence=result.confidence,
                category=result.category,
                processing_time_ms=processing_time
            )
            
            logger.info(f"Task parsed successfully in {processing_time:.2f}ms")
            return response
            
        except (InvalidInputError, ProcessingTimeoutError, AIServiceError):
            # Re-raise our custom errors
            raise
        except Exception as e:
            logger.error(f"Unexpected error in task parsing: {str(e)}", exc_info=True)
            raise AIServiceError(
                f"Task parsing failed due to an internal error: {str(e)}",
                "PARSING_ERROR"
            )

@router.post("/prioritize-tasks", response_model=TaskPrioritizationResponse)
async def prioritize_tasks(request: TaskPrioritizationRequest):
    """
    Prioritize a list of tasks using AI-powered analysis
    
    This endpoint analyzes tasks based on:
    - Urgency (deadlines, keywords)
    - Importance (impact, business value)
    - Complexity (effort required)
    - Dependencies (blocking relationships)
    
    Raises:
        InvalidInputError: If input tasks are invalid
        InsufficientDataError: If insufficient tasks provided
        ProcessingTimeoutError: If processing takes too long
        AIServiceError: If AI processing fails
    """
    start_time = datetime.now()
    
    async with request_semaphore:
        try:
            # Validate input
            validate_prioritization_tasks(request.tasks)
            
            logger.info(f"Prioritizing {len(request.tasks)} tasks...")
            
            # Convert request tasks to dict format
            tasks_dict = []
            for task in request.tasks:
                task_dict = {
                    'id': task.id,
                    'title': task.title,
                    'description': task.description or '',
                    'due_date': task.due_date,
                    'category': task.category,
                    'priority': task.priority
                }
                tasks_dict.append(task_dict)
            
            # Run prioritization with timeout
            async def prioritize_with_timeout():
                loop = asyncio.get_event_loop()
                return await loop.run_in_executor(None, task_prioritizer.prioritize_tasks, tasks_dict)
            
            result = await timeout_handler(prioritize_with_timeout(), DEFAULT_TIMEOUT * 2)  # Double timeout for batch processing
            
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            
            # Convert result to response format
            prioritized_tasks = []
            for task_priority in result.prioritized_tasks:
                prioritized_tasks.append(TaskPriorityResponse(
                    task_id=task_priority.task_id,
                    priority_score=task_priority.priority_score,
                    urgency=task_priority.urgency.name.lower(),
                    importance=task_priority.importance.name.lower(),
                    reasoning=task_priority.reasoning,
                    factors=task_priority.factors,
                    confidence=task_priority.confidence
                ))
            
            response = TaskPrioritizationResponse(
                prioritized_tasks=prioritized_tasks,
                reasoning=result.reasoning,
                confidence=result.confidence,
                factors_considered=result.factors_considered,
                total_tasks=result.total_tasks,
                processing_time_ms=processing_time
            )
            
            logger.info(f"Tasks prioritized successfully in {processing_time:.2f}ms")
            return response
            
        except (InvalidInputError, InsufficientDataError, ProcessingTimeoutError, AIServiceError):
            # Re-raise our custom errors
            raise
        except Exception as e:
            logger.error(f"Unexpected error in task prioritization: {str(e)}", exc_info=True)
            raise AIServiceError(
                f"Task prioritization failed due to an internal error: {str(e)}",
                "PRIORITIZATION_ERROR"
            )

@router.post("/insights", response_model=InsightsResponse)
async def generate_insights(request: InsightsRequest):
    """
    Generate productivity insights from task data
    
    This endpoint analyzes task patterns to provide:
    - Productivity insights and recommendations
    - Time-based patterns (peak hours, best days)
    - Category performance analysis
    - Completion trends and workload patterns
    
    Raises:
        InvalidInputError: If input data is invalid
        InsufficientDataError: If insufficient data for analysis
        ProcessingTimeoutError: If processing takes too long
        AIServiceError: If AI processing fails
    """
    start_time = datetime.now()
    
    async with request_semaphore:
        try:
            # Validate input
            validate_task_data(request.task_data)
            
            logger.info(f"Generating insights for user {request.user_id}...")
            
            # Prepare task data for analysis
            analysis_data = {
                'tasks': request.task_data.get('tasks', []),
                'user_id': request.user_id,
                'period_days': request.period_days,
                'metadata': request.task_data.get('metadata', {})
            }
            
            # Run insight generation with timeout
            async def generate_with_timeout():
                loop = asyncio.get_event_loop()
                return await loop.run_in_executor(None, insight_generator.generate_insights, analysis_data)
            
            result = await timeout_handler(generate_with_timeout(), DEFAULT_TIMEOUT * 3)  # Triple timeout for complex analysis
            
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            
            # Convert result to response format
            detailed_insights = []
            for insight in result.detailed_insights:
                detailed_insights.append(ProductivityInsightResponse(
                    insight=insight.insight,
                    category=insight.category,
                    confidence=insight.confidence,
                    recommendation=insight.recommendation
                ))
            
            detected_patterns = []
            for pattern in result.detected_patterns:
                detected_patterns.append(ProductivityPatternResponse(
                    pattern_type=pattern.pattern_type,
                    description=pattern.description,
                    strength=pattern.strength,
                    trend=pattern.trend
                ))
            
            response = InsightsResponse(
                insights=result.insights,
                recommendations=result.recommendations,
                detailed_insights=detailed_insights,
                detected_patterns=detected_patterns,
                confidence=result.confidence,
                analysis_period=result.analysis_period,
                data_quality=result.data_quality,
                processing_time_ms=processing_time
            )
            
            logger.info(f"Insights generated successfully in {processing_time:.2f}ms")
            return response
            
        except (InvalidInputError, InsufficientDataError, ProcessingTimeoutError, AIServiceError):
            # Re-raise our custom errors
            raise
        except Exception as e:
            logger.error(f"Unexpected error in insight generation: {str(e)}", exc_info=True)
            raise AIServiceError(
                f"Insight generation failed due to an internal error: {str(e)}",
                "INSIGHT_ERROR"
            )