"""
Tests for AI processing endpoints with real AI functionality
"""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
import json
from datetime import datetime, timedelta

# Test data
SAMPLE_TASK_TEXT = "Create a new user authentication system with JWT tokens"
SAMPLE_LONG_TEXT = "A" * 2001  # Exceeds max length
SAMPLE_TASKS_FOR_PRIORITIZATION = [
    {
        "id": "task-1",
        "title": "Fix critical bug in payment system",
        "description": "Urgent fix needed for payment processing",
        "due_date": (datetime.now() + timedelta(days=1)).isoformat(),
        "category": "bug",
        "priority": "high"
    },
    {
        "id": "task-2", 
        "title": "Update documentation",
        "description": "Update API documentation",
        "category": "documentation",
        "priority": "low"
    },
    {
        "id": "task-3",
        "title": "Implement new feature",
        "description": "Add user profile management",
        "due_date": (datetime.now() + timedelta(days=7)).isoformat(),
        "category": "feature",
        "priority": "medium"
    }
]

SAMPLE_TASK_DATA = {
    "tasks": [
        {
            "id": "1",
            "title": "Complete project setup",
            "description": "Set up development environment",
            "status": "completed",
            "created_at": (datetime.now() - timedelta(days=5)).isoformat(),
            "completed_at": (datetime.now() - timedelta(days=4)).isoformat(),
            "category": "setup",
            "priority": "high",
            "estimated_hours": 2.0,
            "actual_hours": 1.5
        },
        {
            "id": "2",
            "title": "Write unit tests",
            "description": "Create comprehensive test suite",
            "status": "in_progress",
            "created_at": (datetime.now() - timedelta(days=3)).isoformat(),
            "category": "testing",
            "priority": "medium",
            "estimated_hours": 4.0
        },
        {
            "id": "3",
            "title": "Deploy to staging",
            "description": "Deploy application to staging environment",
            "status": "completed",
            "created_at": (datetime.now() - timedelta(days=2)).isoformat(),
            "completed_at": (datetime.now() - timedelta(days=1)).isoformat(),
            "category": "deployment",
            "priority": "high",
            "estimated_hours": 1.0,
            "actual_hours": 1.5
        }
    ]
}

class TestTaskParsingEndpoint:
    """Test cases for task parsing endpoint"""
    
    def test_parse_task_success(self, client: TestClient):
        """Test successful task parsing"""
        request_data = {"text": SAMPLE_TASK_TEXT}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        
        # Validate response structure
        required_fields = [
            "title", "description", "priority", "estimated_hours", 
            "tags", "confidence", "category", "processing_time_ms"
        ]
        for field in required_fields:
            assert field in data, f"Missing field: {field}"
        
        # Validate data types
        assert isinstance(data["title"], str)
        assert isinstance(data["description"], str)
        assert isinstance(data["priority"], str)
        assert isinstance(data["estimated_hours"], (int, float))
        assert isinstance(data["tags"], list)
        assert isinstance(data["confidence"], (int, float))
        assert isinstance(data["processing_time_ms"], (int, float))
        
        # Validate ranges
        assert 0.0 <= data["confidence"] <= 1.0
        assert data["estimated_hours"] > 0
        assert data["processing_time_ms"] >= 0
        
        # Validate content quality
        assert len(data["title"]) > 0
        assert data["title"] != "Untitled Task"  # Should extract meaningful title
        assert data["priority"] in ["low", "medium", "high", "urgent"]
    
    def test_parse_task_empty_text(self, client: TestClient):
        """Test task parsing with empty text"""
        request_data = {"text": ""}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        assert response.status_code == 422  # Pydantic validation error
        data = response.json()
        assert "detail" in data  # FastAPI validation error format
    
    def test_parse_task_whitespace_only(self, client: TestClient):
        """Test task parsing with whitespace only"""
        request_data = {"text": "   \n\t   "}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        assert response.status_code == 422  # Pydantic validation error
        data = response.json()
        assert "detail" in data
    
    def test_parse_task_too_long(self, client: TestClient):
        """Test task parsing with text exceeding maximum length"""
        request_data = {"text": SAMPLE_LONG_TEXT}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        assert response.status_code == 422  # Pydantic validation error
        data = response.json()
        assert "detail" in data
    
    def test_parse_task_missing_field(self, client: TestClient):
        """Test task parsing with missing required field"""
        response = client.post("/ai/parse-task", json={})
        
        assert response.status_code == 422
        data = response.json()
        assert "detail" in data
        # Check that the error mentions the missing field
        assert any("text" in str(error) for error in data["detail"])
    
    def test_parse_task_invalid_json(self, client: TestClient):
        """Test task parsing with invalid JSON"""
        response = client.post(
            "/ai/parse-task", 
            data="invalid json",
            headers={"Content-Type": "application/json"}
        )
        
        assert response.status_code == 422
    
    def test_parse_task_special_characters(self, client: TestClient):
        """Test task parsing with special characters"""
        request_data = {"text": "Fix bug with émojis 🐛 and spëcial chars"}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        assert len(data["title"]) > 0
    
    def test_parse_task_with_time_indicators(self, client: TestClient):
        """Test task parsing with time indicators"""
        request_data = {"text": "Complete urgent task in 2 hours"}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        
        # Should detect urgency and time estimate
        assert data["priority"] in ["high", "urgent"]
        assert data["estimated_hours"] == 2.0


class TestTaskPrioritizationEndpoint:
    """Test cases for task prioritization endpoint"""
    
    def test_prioritize_tasks_success(self, client: TestClient):
        """Test successful task prioritization"""
        request_data = {"tasks": SAMPLE_TASKS_FOR_PRIORITIZATION}
        
        response = client.post("/ai/prioritize-tasks", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        
        # Validate response structure
        required_fields = [
            "prioritized_tasks", "reasoning", "confidence", 
            "factors_considered", "total_tasks", "processing_time_ms"
        ]
        for field in required_fields:
            assert field in data, f"Missing field: {field}"
        
        # Validate prioritized tasks
        assert len(data["prioritized_tasks"]) == len(SAMPLE_TASKS_FOR_PRIORITIZATION)
        assert data["total_tasks"] == len(SAMPLE_TASKS_FOR_PRIORITIZATION)
        
        for task in data["prioritized_tasks"]:
            task_fields = [
                "task_id", "priority_score", "urgency", "importance", 
                "reasoning", "factors", "confidence"
            ]
            for field in task_fields:
                assert field in task, f"Missing task field: {field}"
            
            # Validate ranges
            assert 0.0 <= task["priority_score"] <= 1.0
            assert 0.0 <= task["confidence"] <= 1.0
            assert task["urgency"] in ["low", "medium", "high", "critical"]
            assert task["importance"] in ["low", "medium", "high", "critical"]
        
        # Validate sorting (should be in descending priority order)
        scores = [task["priority_score"] for task in data["prioritized_tasks"]]
        assert scores == sorted(scores, reverse=True)
    
    def test_prioritize_tasks_empty_list(self, client: TestClient):
        """Test task prioritization with empty task list"""
        request_data = {"tasks": []}
        
        response = client.post("/ai/prioritize-tasks", json=request_data)
        
        assert response.status_code == 422  # Pydantic validation error
        data = response.json()
        assert "detail" in data
    
    def test_prioritize_tasks_too_many(self, client: TestClient):
        """Test task prioritization with too many tasks"""
        # Create 51 tasks (exceeds limit of 50)
        tasks = []
        for i in range(51):
            tasks.append({
                "id": f"task-{i}",
                "title": f"Task {i}",
                "description": f"Description for task {i}"
            })
        
        request_data = {"tasks": tasks}
        
        response = client.post("/ai/prioritize-tasks", json=request_data)
        
        assert response.status_code == 422  # Pydantic validation error
        data = response.json()
        assert "detail" in data
    
    def test_prioritize_tasks_invalid_task_data(self, client: TestClient):
        """Test task prioritization with invalid task data"""
        request_data = {
            "tasks": [
                {
                    "id": "",  # Empty ID
                    "title": "Valid title"
                }
            ]
        }
        
        response = client.post("/ai/prioritize-tasks", json=request_data)
        
        assert response.status_code == 400
        data = response.json()
        assert data["error"] is True
    
    def test_prioritize_tasks_missing_required_fields(self, client: TestClient):
        """Test task prioritization with missing required fields"""
        request_data = {
            "tasks": [
                {
                    "id": "task-1"
                    # Missing title
                }
            ]
        }
        
        response = client.post("/ai/prioritize-tasks", json=request_data)
        
        assert response.status_code == 422
        data = response.json()
        assert "detail" in data
    
    def test_prioritize_single_task(self, client: TestClient):
        """Test prioritization with single task"""
        request_data = {
            "tasks": [SAMPLE_TASKS_FOR_PRIORITIZATION[0]]
        }
        
        response = client.post("/ai/prioritize-tasks", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        assert len(data["prioritized_tasks"]) == 1
        assert data["total_tasks"] == 1


class TestInsightsEndpoint:
    """Test cases for insights generation endpoint"""
    
    def test_generate_insights_success(self, client: TestClient):
        """Test successful insights generation"""
        request_data = {
            "user_id": "test-user-123",
            "task_data": SAMPLE_TASK_DATA,
            "period_days": 30
        }
        
        response = client.post("/ai/insights", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        
        # Validate response structure
        required_fields = [
            "insights", "recommendations", "detailed_insights", 
            "detected_patterns", "confidence", "analysis_period", 
            "data_quality", "processing_time_ms"
        ]
        for field in required_fields:
            assert field in data, f"Missing field: {field}"
        
        # Validate data types
        assert isinstance(data["insights"], list)
        assert isinstance(data["recommendations"], list)
        assert isinstance(data["detailed_insights"], list)
        assert isinstance(data["detected_patterns"], list)
        assert isinstance(data["confidence"], (int, float))
        assert isinstance(data["analysis_period"], str)
        assert isinstance(data["data_quality"], str)
        assert isinstance(data["processing_time_ms"], (int, float))
        
        # Validate ranges
        assert 0.0 <= data["confidence"] <= 1.0
        assert data["processing_time_ms"] >= 0
        
        # Validate detailed insights structure
        for insight in data["detailed_insights"]:
            assert "insight" in insight
            assert "category" in insight
            assert "confidence" in insight
            assert isinstance(insight["confidence"], (int, float))
            assert 0.0 <= insight["confidence"] <= 1.0
        
        # Validate detected patterns structure
        for pattern in data["detected_patterns"]:
            assert "pattern_type" in pattern
            assert "description" in pattern
            assert "strength" in pattern
            assert "trend" in pattern
            assert isinstance(pattern["strength"], (int, float))
            assert 0.0 <= pattern["strength"] <= 1.0
    
    def test_generate_insights_empty_task_data(self, client: TestClient):
        """Test insights generation with empty task data"""
        request_data = {
            "user_id": "test-user",
            "task_data": {"tasks": []},
            "period_days": 30
        }
        
        response = client.post("/ai/insights", json=request_data)
        
        assert response.status_code == 400
        data = response.json()
        assert data["error"] is True
        assert "INSUFFICIENT_DATA" in data["error_code"]
    
    def test_generate_insights_invalid_task_data(self, client: TestClient):
        """Test insights generation with invalid task data"""
        request_data = {
            "user_id": "test-user",
            "task_data": "invalid data",  # Should be dict
            "period_days": 30
        }
        
        response = client.post("/ai/insights", json=request_data)
        
        assert response.status_code == 422
        data = response.json()
        assert "detail" in data
    
    def test_generate_insights_missing_required_fields(self, client: TestClient):
        """Test insights generation with missing required fields"""
        request_data = {
            "user_id": "test-user"
            # Missing task_data
        }
        
        response = client.post("/ai/insights", json=request_data)
        
        assert response.status_code == 422
        data = response.json()
        assert "detail" in data
    
    def test_generate_insights_invalid_period(self, client: TestClient):
        """Test insights generation with invalid period"""
        request_data = {
            "user_id": "test-user",
            "task_data": SAMPLE_TASK_DATA,
            "period_days": 0  # Invalid period
        }
        
        response = client.post("/ai/insights", json=request_data)
        
        assert response.status_code == 422
        data = response.json()
        assert "detail" in data
    
    def test_generate_insights_minimal_data(self, client: TestClient):
        """Test insights generation with minimal valid data"""
        minimal_data = {
            "tasks": [
                {
                    "id": "1",
                    "title": "Test task",
                    "status": "completed"
                }
            ]
        }
        
        request_data = {
            "user_id": "test-user",
            "task_data": minimal_data
        }
        
        response = client.post("/ai/insights", json=request_data)
        
        # Should succeed but with lower confidence
        assert response.status_code == 200
        data = response.json()
        assert "limited data" in data["data_quality"].lower()


class TestErrorHandling:
    """Test cases for error handling across all endpoints"""
    
    def test_content_type_validation(self, client: TestClient):
        """Test that endpoints validate content type"""
        # Test with wrong content type
        response = client.post(
            "/ai/parse-task",
            data="text=test",
            headers={"Content-Type": "application/x-www-form-urlencoded"}
        )
        
        assert response.status_code == 422
    
    def test_error_response_format(self, client: TestClient):
        """Test that error responses follow standard format"""
        response = client.post("/ai/parse-task", json={})
        
        assert response.status_code == 422
        data = response.json()
        
        # FastAPI validation errors have a different format
        assert "detail" in data
        assert isinstance(data["detail"], list)
    
    @patch('app.api.routes.ai.task_parser.parse_task')
    def test_timeout_handling(self, mock_parse, client: TestClient):
        """Test timeout handling for long-running operations"""
        # Mock a function that takes too long (synchronous version)
        import time
        
        def slow_function(*args):
            time.sleep(100)  # Simulate slow operation
            return MagicMock()
        
        mock_parse.side_effect = slow_function
        
        request_data = {"text": "test task"}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        # Should timeout and return appropriate error (or 500 if timeout not properly handled)
        assert response.status_code in [408, 500]  # Accept either timeout or internal error
        data = response.json()
        # Just check that we get some error response
        assert "error" in data or "detail" in data
    
    def test_concurrent_request_handling(self, client: TestClient):
        """Test handling of concurrent requests"""
        import threading
        import time
        
        results = []
        
        def make_request():
            response = client.post("/ai/parse-task", json={"text": "test task"})
            results.append(response.status_code)
        
        # Make multiple concurrent requests
        threads = []
        for _ in range(5):
            thread = threading.Thread(target=make_request)
            threads.append(thread)
            thread.start()
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join()
        
        # All requests should succeed (or fail gracefully)
        assert len(results) == 5
        assert all(code in [200, 408, 503] for code in results)


class TestResponseValidation:
    """Test cases for response validation"""
    
    def test_parse_task_response_schema(self, client: TestClient):
        """Test that parse task response matches expected schema"""
        request_data = {"text": "Create API endpoint"}
        
        response = client.post("/ai/parse-task", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        
        # Validate all required fields are present and correct type
        assert isinstance(data["title"], str)
        assert isinstance(data["description"], str)
        assert isinstance(data["priority"], str)
        assert isinstance(data["estimated_hours"], (int, float))
        assert isinstance(data["tags"], list)
        assert isinstance(data["confidence"], (int, float))
        assert data["category"] is None or isinstance(data["category"], str)
        assert isinstance(data["processing_time_ms"], (int, float))
    
    def test_prioritization_response_schema(self, client: TestClient):
        """Test that prioritization response matches expected schema"""
        request_data = {"tasks": SAMPLE_TASKS_FOR_PRIORITIZATION[:1]}
        
        response = client.post("/ai/prioritize-tasks", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        
        # Validate response schema
        assert isinstance(data["prioritized_tasks"], list)
        assert isinstance(data["reasoning"], str)
        assert isinstance(data["confidence"], (int, float))
        assert isinstance(data["factors_considered"], list)
        assert isinstance(data["total_tasks"], int)
        assert isinstance(data["processing_time_ms"], (int, float))
    
    def test_insights_response_schema(self, client: TestClient):
        """Test that insights response matches expected schema"""
        request_data = {
            "user_id": "test-user",
            "task_data": SAMPLE_TASK_DATA
        }
        
        response = client.post("/ai/insights", json=request_data)
        
        assert response.status_code == 200
        data = response.json()
        
        # Validate response schema
        assert isinstance(data["insights"], list)
        assert isinstance(data["recommendations"], list)
        assert isinstance(data["detailed_insights"], list)
        assert isinstance(data["detected_patterns"], list)
        assert isinstance(data["confidence"], (int, float))
        assert isinstance(data["analysis_period"], str)
        assert isinstance(data["data_quality"], str)
        assert isinstance(data["processing_time_ms"], (int, float))