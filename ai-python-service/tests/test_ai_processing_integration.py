"""
Integration tests for AI processing pipeline
"""
import pytest
import json
from datetime import datetime, timedelta
from typing import Dict, Any, List

from app.ai.task_parser import TaskParser, Priority
from app.ai.task_prioritizer import TaskPrioritizer, UrgencyLevel, ImportanceLevel
from app.ai.insight_generator import InsightGenerator
from app.models.model_manager import ModelManager
from app.models.metadata import ModelType


class TestAIProcessingIntegration:
    """Integration tests for the complete AI processing pipeline"""
    
    @pytest.fixture
    def task_parser(self):
        """Create TaskParser instance"""
        return TaskParser()
    
    @pytest.fixture
    def task_prioritizer(self):
        """Create TaskPrioritizer instance"""
        return TaskPrioritizer()
    
    @pytest.fixture
    def insight_generator(self):
        """Create InsightGenerator instance"""
        return InsightGenerator()
    
    @pytest.fixture
    def model_manager(self):
        """Create ModelManager instance"""
        return ModelManager()
    
    @pytest.fixture
    def sample_task_descriptions(self):
        """Sample task descriptions for testing"""
        return [
            "Fix urgent bug in user authentication system",
            "Write documentation for the new API endpoints",
            "Implement user profile page with React components",
            "Review pull request for database optimization",
            "Plan next sprint and update project roadmap",
            "Update dependencies and fix security vulnerabilities",
            "Create unit tests for the payment processing module"
        ]
    
    @pytest.fixture
    def sample_task_data(self):
        """Sample task data for insights generation"""
        base_date = datetime.now() - timedelta(days=30)
        
        tasks = []
        for i in range(20):
            task_date = base_date + timedelta(days=i)
            completed_date = task_date + timedelta(hours=2 + (i % 6))
            
            task = {
                "id": i,
                "title": f"Task {i}",
                "description": f"Description for task {i}",
                "status": "completed" if i % 3 != 0 else "pending",
                "created_at": task_date.isoformat(),
                "completed_at": completed_date.isoformat() if i % 3 != 0 else None,
                "category": ["development", "testing", "documentation"][i % 3],
                "priority": "high" if i % 5 == 0 else "medium",
                "estimated_hours": 2.0 + (i % 4),
                "actual_hours": 2.5 + (i % 3) if i % 3 != 0 else None
            }
            tasks.append(task)
        
        return {"tasks": tasks, "user_id": "test_user"}
    
    def test_task_parsing_pipeline(self, task_parser, sample_task_descriptions):
        """Test the complete task parsing pipeline"""
        results = []
        
        for description in sample_task_descriptions:
            result = task_parser.parse_task(description)
            results.append(result)
            
            # Validate result structure
            assert result.title is not None
            assert result.description == description
            assert isinstance(result.priority, Priority)
            assert result.estimated_hours > 0
            assert isinstance(result.tags, list)
            assert 0.0 <= result.confidence <= 1.0
        
        # Check that different tasks produce different results
        titles = [r.title for r in results]
        assert len(set(titles)) > 1, "All tasks should not have identical titles"
        
        # Check that urgent tasks are detected
        urgent_results = [r for r in results if r.priority in [Priority.HIGH, Priority.URGENT]]
        assert len(urgent_results) > 0, "Should detect at least one urgent task"
        
        # Check that tags are extracted
        all_tags = []
        for result in results:
            all_tags.extend(result.tags)
        assert len(all_tags) > 0, "Should extract some tags"
    
    def test_task_prioritization_pipeline(self, task_prioritizer, sample_task_descriptions):
        """Test the complete task prioritization pipeline"""
        # Convert descriptions to task format
        tasks = []
        for i, description in enumerate(sample_task_descriptions):
            task = {
                "id": i,
                "title": description,
                "description": description,
                "due_date": (datetime.now() + timedelta(days=i)).isoformat() if i < 3 else None
            }
            tasks.append(task)
        
        result = task_prioritizer.prioritize_tasks(tasks)
        
        # Validate result structure
        assert len(result.prioritized_tasks) == len(tasks)
        assert result.reasoning is not None
        assert 0.0 <= result.confidence <= 1.0
        assert isinstance(result.factors_considered, list)
        assert result.total_tasks == len(tasks)
        assert result.processing_time_ms >= 0
        
        # Check that tasks are properly prioritized
        priority_scores = [tp.priority_score for tp in result.prioritized_tasks]
        assert priority_scores == sorted(priority_scores, reverse=True), "Tasks should be sorted by priority score"
        
        # Check that urgent tasks get higher priority
        urgent_task_indices = [i for i, desc in enumerate(sample_task_descriptions) if "urgent" in desc.lower()]
        if urgent_task_indices:
            urgent_priorities = [tp.priority_score for tp in result.prioritized_tasks 
                               if int(tp.task_id) in urgent_task_indices]
            avg_urgent_priority = sum(urgent_priorities) / len(urgent_priorities)
            avg_overall_priority = sum(priority_scores) / len(priority_scores)
            assert avg_urgent_priority >= avg_overall_priority, "Urgent tasks should have higher average priority"
    
    def test_insight_generation_pipeline(self, insight_generator, sample_task_data):
        """Test the complete insight generation pipeline"""
        result = insight_generator.generate_insights(sample_task_data)
        
        # Validate result structure
        assert isinstance(result.insights, list)
        assert isinstance(result.recommendations, list)
        assert isinstance(result.patterns, dict)
        assert 0.0 <= result.confidence <= 1.0
        assert result.analysis_period is not None
        assert result.data_quality is not None
        
        # Check that insights are generated
        assert len(result.insights) > 0, "Should generate at least one insight"
        assert len(result.recommendations) > 0, "Should generate at least one recommendation"
        
        # Check that patterns are detected
        assert "total_patterns" in result.patterns
        assert result.patterns["total_patterns"] >= 0
        
        # Check detailed insights
        assert isinstance(result.detailed_insights, list)
        for insight in result.detailed_insights:
            assert hasattr(insight, 'insight')
            assert hasattr(insight, 'category')
            assert hasattr(insight, 'confidence')
            assert hasattr(insight, 'supporting_data')
    
    def test_model_manager_integration(self, model_manager, sample_task_descriptions, sample_task_data):
        """Test integration through ModelManager"""
        # Test task parsing through model manager
        for description in sample_task_descriptions[:3]:  # Test first 3 to save time
            result = model_manager.process_task_parsing(description)
            
            # Validate structure
            assert "title" in result
            assert "description" in result
            assert "priority" in result
            assert "estimated_hours" in result
            assert "tags" in result
            assert "confidence" in result
            
            # Validate data types
            assert isinstance(result["title"], str)
            assert isinstance(result["description"], str)
            assert isinstance(result["priority"], str)
            assert isinstance(result["estimated_hours"], (int, float))
            assert isinstance(result["tags"], list)
            assert isinstance(result["confidence"], (int, float))
        
        # Test task prioritization through model manager
        tasks = [{"id": i, "title": desc, "description": desc} 
                for i, desc in enumerate(sample_task_descriptions)]
        tasks_json = json.dumps(tasks)
        
        result = model_manager.process_task_prioritization(tasks_json)
        
        # Validate structure
        assert "prioritized_tasks" in result
        assert "reasoning" in result
        assert "confidence" in result
        assert "factors_considered" in result
        
        # Validate prioritized tasks
        assert len(result["prioritized_tasks"]) == len(tasks)
        for task in result["prioritized_tasks"]:
            assert "task_id" in task
            assert "priority_score" in task
            assert "reasoning" in task
            assert "confidence" in task
        
        # Test insights generation through model manager
        data_json = json.dumps(sample_task_data)
        result = model_manager.process_insights_generation(data_json)
        
        # Validate structure
        assert "insights" in result
        assert "recommendations" in result
        assert "patterns" in result
        assert "confidence" in result
        
        # Validate content
        assert isinstance(result["insights"], list)
        assert isinstance(result["recommendations"], list)
        assert isinstance(result["patterns"], dict)
        assert len(result["insights"]) > 0
    
    def test_end_to_end_pipeline(self, task_parser, task_prioritizer, insight_generator):
        """Test complete end-to-end AI processing pipeline"""
        # Step 1: Parse individual tasks
        raw_descriptions = [
            "Urgent: Fix critical security vulnerability in authentication",
            "Write comprehensive documentation for new API",
            "Implement user dashboard with charts and analytics",
            "Review and merge pull request for performance optimization"
        ]
        
        parsed_tasks = []
        for i, description in enumerate(raw_descriptions):
            parsed = task_parser.parse_task(description)
            task_dict = {
                "id": i,
                "title": parsed.title,
                "description": parsed.description,
                "priority": parsed.priority.value,
                "estimated_hours": parsed.estimated_hours,
                "tags": parsed.tags,
                "category": parsed.category,
                "created_at": datetime.now().isoformat()
            }
            parsed_tasks.append(task_dict)
        
        # Step 2: Prioritize the parsed tasks
        prioritization_result = task_prioritizer.prioritize_tasks(parsed_tasks)
        
        # Verify prioritization worked
        assert len(prioritization_result.prioritized_tasks) == len(parsed_tasks)
        
        # Step 3: Generate insights from historical data (simulate completed tasks)
        historical_tasks = []
        base_date = datetime.now() - timedelta(days=14)
        
        for i in range(15):
            task_date = base_date + timedelta(days=i)
            task = {
                "id": f"hist_{i}",
                "title": f"Historical task {i}",
                "description": f"Completed task {i}",
                "status": "completed",
                "created_at": task_date.isoformat(),
                "completed_at": (task_date + timedelta(hours=2)).isoformat(),
                "category": ["development", "testing", "documentation"][i % 3],
                "priority": "high" if i % 4 == 0 else "medium"
            }
            historical_tasks.append(task)
        
        insight_data = {"tasks": historical_tasks}
        insights_result = insight_generator.generate_insights(insight_data)
        
        # Verify insights were generated
        assert len(insights_result.insights) > 0
        assert len(insights_result.recommendations) > 0
        assert insights_result.confidence > 0
        
        # Step 4: Verify the complete pipeline produces consistent results
        pipeline_results = {
            "parsed_tasks": len(parsed_tasks),
            "prioritized_tasks": len(prioritization_result.prioritized_tasks),
            "insights_generated": len(insights_result.insights),
            "recommendations_generated": len(insights_result.recommendations),
            "overall_confidence": (
                sum(task.get("confidence", 0.5) for task in parsed_tasks) / len(parsed_tasks) +
                prioritization_result.confidence +
                insights_result.confidence
            ) / 3
        }
        
        # Validate pipeline completeness
        assert pipeline_results["parsed_tasks"] > 0
        assert pipeline_results["prioritized_tasks"] > 0
        assert pipeline_results["insights_generated"] > 0
        assert pipeline_results["recommendations_generated"] > 0
        assert pipeline_results["overall_confidence"] > 0.3
        
        return pipeline_results
    
    def test_error_handling_and_fallbacks(self, task_parser, task_prioritizer, insight_generator):
        """Test error handling and fallback mechanisms"""
        # Test task parsing with invalid input
        result = task_parser.parse_task("")
        assert result.title is not None
        assert result.confidence < 0.5  # Low confidence for empty input
        
        result = task_parser.parse_task("a")
        assert result.title is not None
        assert result.confidence < 0.5  # Low confidence for very short input
        
        # Test prioritization with empty task list
        result = task_prioritizer.prioritize_tasks([])
        assert len(result.prioritized_tasks) == 0
        assert result.confidence == 1.0  # High confidence in empty result
        
        # Test prioritization with malformed tasks
        malformed_tasks = [{"invalid": "task"}]
        result = task_prioritizer.prioritize_tasks(malformed_tasks)
        assert len(result.prioritized_tasks) == 1
        assert result.confidence > 0  # Should still work with fallbacks
        
        # Test insights with insufficient data
        result = insight_generator.generate_insights({"tasks": []})
        assert len(result.insights) > 0  # Should provide fallback insights
        assert "insufficient" in result.data_quality.lower() or "limited" in result.data_quality.lower()
        
        # Test insights with malformed data
        result = insight_generator.generate_insights({"invalid": "data"})
        assert len(result.insights) > 0  # Should provide fallback insights
        assert result.confidence < 0.5  # Low confidence for bad data
    
    def test_output_format_consistency(self, model_manager):
        """Test that output formats are consistent across different inputs"""
        # Test multiple task parsing calls
        descriptions = ["Task 1", "Task 2 with more details", "Urgent task 3"]
        results = []
        
        for desc in descriptions:
            result = model_manager.process_task_parsing(desc)
            results.append(result)
        
        # Verify all results have the same structure
        expected_keys = {"title", "description", "priority", "estimated_hours", "tags", "confidence"}
        for result in results:
            assert set(result.keys()) >= expected_keys
            
        # Test multiple prioritization calls
        task_sets = [
            [{"id": 1, "title": "Task A"}],
            [{"id": 1, "title": "Task A"}, {"id": 2, "title": "Task B"}],
            [{"id": i, "title": f"Task {i}"} for i in range(5)]
        ]
        
        for task_set in task_sets:
            result = model_manager.process_task_prioritization(json.dumps(task_set))
            
            # Verify consistent structure
            assert "prioritized_tasks" in result
            assert "reasoning" in result
            assert "confidence" in result
            assert "factors_considered" in result
            assert len(result["prioritized_tasks"]) == len(task_set)
    
    def test_performance_tracking(self, model_manager):
        """Test that performance is tracked correctly"""
        # Perform several operations
        for i in range(3):
            model_manager.process_task_parsing(f"Test task {i}")
        
        # Check performance stats
        stats = model_manager.get_performance_stats()
        assert isinstance(stats, dict)
        
        # Should have stats for task parser
        task_parser_keys = [k for k in stats.keys() if "task_parser" in k]
        assert len(task_parser_keys) > 0
        
        for key in task_parser_keys:
            stat = stats[key]
            assert "avg_inference_time_ms" in stat
            assert "total_inferences" in stat
            assert stat["total_inferences"] > 0
            assert stat["avg_inference_time_ms"] >= 0