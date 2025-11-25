"""
Insight Generator for analyzing task patterns and generating productivity insights
"""
import logging
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass
from datetime import datetime, timedelta
from collections import defaultdict, Counter
import statistics
import json

logger = logging.getLogger(__name__)


@dataclass
class ProductivityInsight:
    """A single productivity insight"""
    insight: str
    category: str
    confidence: float
    supporting_data: Dict[str, Any]
    recommendation: Optional[str] = None


@dataclass
class ProductivityPattern:
    """A detected productivity pattern"""
    pattern_type: str
    description: str
    strength: float  # 0.0 to 1.0
    data_points: int
    trend: str  # "improving", "declining", "stable"


@dataclass
class ProductivityInsights:
    """Complete productivity insights result"""
    insights: List[str]
    recommendations: List[str]
    patterns: Dict[str, Any]
    confidence: float
    detailed_insights: List[ProductivityInsight]
    detected_patterns: List[ProductivityPattern]
    analysis_period: str
    data_quality: str


class InsightGenerator:
    """
    InsightGenerator for analyzing task patterns and generating productivity insights
    """
    
    def __init__(self):
        self.insight_templates = {
            "peak_hours": {
                "template": "You tend to be most productive during {hours}",
                "recommendation": "Schedule your most important tasks during {hours} for optimal performance"
            },
            "completion_rate": {
                "template": "Your task completion rate is {rate}% over the last {period}",
                "recommendation": "Focus on breaking down large tasks to improve completion rates" if "{rate}" == "low" else "Maintain your excellent completion momentum"
            },
            "task_duration": {
                "template": "Your average task completion time is {duration}",
                "recommendation": "Consider time-boxing techniques to improve task estimation accuracy"
            },
            "category_performance": {
                "template": "You perform best on {category} tasks with {performance}% completion rate",
                "recommendation": "Leverage your strength in {category} tasks and apply similar approaches to other areas"
            },
            "workload_pattern": {
                "template": "Your workload tends to be {pattern} with {variation}",
                "recommendation": "Consider load balancing techniques to maintain consistent productivity"
            }
        }
        
        self.pattern_detectors = {
            "time_of_day": self._detect_time_patterns,
            "day_of_week": self._detect_day_patterns,
            "task_category": self._detect_category_patterns,
            "completion_trends": self._detect_completion_trends,
            "workload_distribution": self._detect_workload_patterns
        }
        
        logger.info("InsightGenerator initialized with pattern detection capabilities")
    
    def generate_insights(self, task_data: Dict[str, Any]) -> ProductivityInsights:
        """
        Generate productivity insights from task data
        
        Args:
            task_data: Dictionary containing task history and statistics
            
        Returns:
            ProductivityInsights with analysis and recommendations
        """
        try:
            # Parse and validate input data
            parsed_data = self._parse_task_data(task_data)
            
            if not parsed_data or not parsed_data.get('tasks'):
                return self._generate_fallback_insights("Insufficient task data for analysis")
            
            # Detect patterns
            detected_patterns = self._detect_all_patterns(parsed_data)
            
            # Generate insights from patterns
            detailed_insights = self._generate_detailed_insights(detected_patterns, parsed_data)
            
            # Extract simple insights and recommendations
            insights = [insight.insight for insight in detailed_insights]
            recommendations = [insight.recommendation for insight in detailed_insights if insight.recommendation]
            
            # Create pattern summary
            patterns = self._create_pattern_summary(detected_patterns)
            
            # Calculate overall confidence
            confidence = self._calculate_overall_confidence(detailed_insights, parsed_data)
            
            # Determine analysis period
            analysis_period = self._determine_analysis_period(parsed_data)
            
            # Assess data quality
            data_quality = self._assess_data_quality(parsed_data)
            
            result = ProductivityInsights(
                insights=insights,
                recommendations=recommendations,
                patterns=patterns,
                confidence=confidence,
                detailed_insights=detailed_insights,
                detected_patterns=detected_patterns,
                analysis_period=analysis_period,
                data_quality=data_quality
            )
            
            logger.info(f"Generated {len(insights)} insights with {confidence:.2f} confidence")
            return result
            
        except Exception as e:
            logger.error(f"Insight generation failed: {str(e)}")
            return self._generate_fallback_insights(f"Analysis failed: {str(e)}")
    
    def _parse_task_data(self, task_data: Dict[str, Any]) -> Dict[str, Any]:
        """Parse and normalize task data"""
        try:
            # Handle different input formats
            if 'tasks' in task_data:
                tasks = task_data['tasks']
            elif isinstance(task_data, list):
                tasks = task_data
            else:
                # Try to extract tasks from various possible structures
                tasks = task_data.get('data', task_data.get('task_list', []))
            
            # Normalize task format
            normalized_tasks = []
            for task in tasks:
                if isinstance(task, dict):
                    normalized_task = {
                        'id': task.get('id', task.get('task_id', len(normalized_tasks))),
                        'title': task.get('title', task.get('name', 'Untitled')),
                        'description': task.get('description', ''),
                        'status': task.get('status', 'unknown'),
                        'created_at': task.get('created_at', task.get('createdAt')),
                        'completed_at': task.get('completed_at', task.get('completedAt')),
                        'due_date': task.get('due_date', task.get('dueDate')),
                        'category': task.get('category', task.get('type', 'general')),
                        'priority': task.get('priority', 'medium'),
                        'estimated_hours': task.get('estimated_hours', task.get('estimatedHours', 1.0)),
                        'actual_hours': task.get('actual_hours', task.get('actualHours'))
                    }
                    normalized_tasks.append(normalized_task)
            
            return {
                'tasks': normalized_tasks,
                'user_id': task_data.get('user_id', task_data.get('userId')),
                'period_start': task_data.get('period_start'),
                'period_end': task_data.get('period_end'),
                'metadata': task_data.get('metadata', {})
            }
            
        except Exception as e:
            logger.error(f"Failed to parse task data: {str(e)}")
            return {}
    
    def _detect_all_patterns(self, data: Dict[str, Any]) -> List[ProductivityPattern]:
        """Detect all productivity patterns in the data"""
        patterns = []
        
        for pattern_name, detector in self.pattern_detectors.items():
            try:
                detected = detector(data)
                if detected:
                    patterns.extend(detected if isinstance(detected, list) else [detected])
            except Exception as e:
                logger.warning(f"Pattern detection failed for {pattern_name}: {str(e)}")
        
        return patterns
    
    def _detect_time_patterns(self, data: Dict[str, Any]) -> List[ProductivityPattern]:
        """Detect time-of-day productivity patterns"""
        tasks = data.get('tasks', [])
        completed_tasks = [t for t in tasks if t.get('completed_at')]
        
        if len(completed_tasks) < 3:
            return []
        
        # Extract completion hours
        completion_hours = []
        for task in completed_tasks:
            try:
                if isinstance(task['completed_at'], str):
                    dt = datetime.fromisoformat(task['completed_at'].replace('Z', '+00:00'))
                else:
                    dt = task['completed_at']
                completion_hours.append(dt.hour)
            except (ValueError, TypeError):
                continue
        
        if not completion_hours:
            return []
        
        # Find peak hours
        hour_counts = Counter(completion_hours)
        most_common_hours = hour_counts.most_common(3)
        
        if most_common_hours:
            peak_hour = most_common_hours[0][0]
            peak_count = most_common_hours[0][1]
            total_completions = len(completion_hours)
            
            strength = peak_count / total_completions
            
            if strength > 0.3:  # At least 30% of tasks completed in peak hour
                return [ProductivityPattern(
                    pattern_type="peak_hours",
                    description=f"Peak productivity at {peak_hour}:00 ({peak_count}/{total_completions} tasks)",
                    strength=strength,
                    data_points=total_completions,
                    trend="stable"
                )]
        
        return []
    
    def _detect_day_patterns(self, data: Dict[str, Any]) -> List[ProductivityPattern]:
        """Detect day-of-week productivity patterns"""
        tasks = data.get('tasks', [])
        completed_tasks = [t for t in tasks if t.get('completed_at')]
        
        if len(completed_tasks) < 5:
            return []
        
        # Extract completion days
        completion_days = []
        for task in completed_tasks:
            try:
                if isinstance(task['completed_at'], str):
                    dt = datetime.fromisoformat(task['completed_at'].replace('Z', '+00:00'))
                else:
                    dt = task['completed_at']
                completion_days.append(dt.strftime('%A'))
            except (ValueError, TypeError):
                continue
        
        if not completion_days:
            return []
        
        # Find most productive day
        day_counts = Counter(completion_days)
        most_common_days = day_counts.most_common(2)
        
        if most_common_days:
            best_day = most_common_days[0][0]
            best_count = most_common_days[0][1]
            total_completions = len(completion_days)
            
            strength = best_count / total_completions
            
            if strength > 0.25:  # At least 25% of tasks completed on best day
                return [ProductivityPattern(
                    pattern_type="day_of_week",
                    description=f"Most productive on {best_day} ({best_count}/{total_completions} tasks)",
                    strength=strength,
                    data_points=total_completions,
                    trend="stable"
                )]
        
        return []
    
    def _detect_category_patterns(self, data: Dict[str, Any]) -> List[ProductivityPattern]:
        """Detect task category performance patterns"""
        tasks = data.get('tasks', [])
        
        if len(tasks) < 5:
            return []
        
        # Group by category
        category_stats = defaultdict(lambda: {'total': 0, 'completed': 0})
        
        for task in tasks:
            category = task.get('category', 'general')
            category_stats[category]['total'] += 1
            
            if task.get('status') == 'completed' or task.get('completed_at'):
                category_stats[category]['completed'] += 1
        
        # Calculate completion rates
        patterns = []
        for category, stats in category_stats.items():
            if stats['total'] >= 3:  # Need at least 3 tasks in category
                completion_rate = stats['completed'] / stats['total']
                
                if completion_rate > 0.8:  # High performance category
                    patterns.append(ProductivityPattern(
                        pattern_type="high_performance_category",
                        description=f"Excellent performance in {category} tasks ({completion_rate:.1%} completion rate)",
                        strength=completion_rate,
                        data_points=stats['total'],
                        trend="stable"
                    ))
                elif completion_rate < 0.4:  # Low performance category
                    patterns.append(ProductivityPattern(
                        pattern_type="low_performance_category",
                        description=f"Challenges with {category} tasks ({completion_rate:.1%} completion rate)",
                        strength=1.0 - completion_rate,
                        data_points=stats['total'],
                        trend="stable"
                    ))
        
        return patterns
    
    def _detect_completion_trends(self, data: Dict[str, Any]) -> List[ProductivityPattern]:
        """Detect completion rate trends over time"""
        tasks = data.get('tasks', [])
        
        if len(tasks) < 10:
            return []
        
        # Sort tasks by creation date
        dated_tasks = []
        for task in tasks:
            try:
                created_at = task.get('created_at')
                if created_at:
                    if isinstance(created_at, str):
                        dt = datetime.fromisoformat(created_at.replace('Z', '+00:00'))
                    else:
                        dt = created_at
                    dated_tasks.append((dt, task))
            except (ValueError, TypeError):
                continue
        
        if len(dated_tasks) < 10:
            return []
        
        dated_tasks.sort(key=lambda x: x[0])
        
        # Split into two halves and compare completion rates
        mid_point = len(dated_tasks) // 2
        first_half = dated_tasks[:mid_point]
        second_half = dated_tasks[mid_point:]
        
        first_half_completed = sum(1 for _, task in first_half 
                                 if task.get('status') == 'completed' or task.get('completed_at'))
        second_half_completed = sum(1 for _, task in second_half 
                                  if task.get('status') == 'completed' or task.get('completed_at'))
        
        first_rate = first_half_completed / len(first_half)
        second_rate = second_half_completed / len(second_half)
        
        rate_change = second_rate - first_rate
        
        if abs(rate_change) > 0.1:  # Significant change
            if rate_change > 0:
                trend = "improving"
                description = f"Completion rate improving ({first_rate:.1%} → {second_rate:.1%})"
            else:
                trend = "declining"
                description = f"Completion rate declining ({first_rate:.1%} → {second_rate:.1%})"
            
            return [ProductivityPattern(
                pattern_type="completion_trend",
                description=description,
                strength=abs(rate_change),
                data_points=len(dated_tasks),
                trend=trend
            )]
        
        return []
    
    def _detect_workload_patterns(self, data: Dict[str, Any]) -> List[ProductivityPattern]:
        """Detect workload distribution patterns"""
        tasks = data.get('tasks', [])
        
        if len(tasks) < 7:
            return []
        
        # Group tasks by week
        weekly_counts = defaultdict(int)
        for task in tasks:
            try:
                created_at = task.get('created_at')
                if created_at:
                    if isinstance(created_at, str):
                        dt = datetime.fromisoformat(created_at.replace('Z', '+00:00'))
                    else:
                        dt = created_at
                    
                    # Get week number
                    week_key = dt.strftime('%Y-W%U')
                    weekly_counts[week_key] += 1
            except (ValueError, TypeError):
                continue
        
        if len(weekly_counts) < 2:
            return []
        
        # Calculate workload variation
        counts = list(weekly_counts.values())
        if len(counts) > 1:
            avg_workload = statistics.mean(counts)
            workload_std = statistics.stdev(counts)
            
            variation_coefficient = workload_std / avg_workload if avg_workload > 0 else 0
            
            if variation_coefficient > 0.5:  # High variation
                return [ProductivityPattern(
                    pattern_type="workload_variation",
                    description=f"Highly variable workload (avg: {avg_workload:.1f} tasks/week, std: {workload_std:.1f})",
                    strength=variation_coefficient,
                    data_points=len(counts),
                    trend="variable"
                )]
            elif variation_coefficient < 0.2:  # Consistent workload
                return [ProductivityPattern(
                    pattern_type="consistent_workload",
                    description=f"Consistent workload (avg: {avg_workload:.1f} tasks/week)",
                    strength=1.0 - variation_coefficient,
                    data_points=len(counts),
                    trend="stable"
                )]
        
        return []
    
    def _generate_detailed_insights(self, patterns: List[ProductivityPattern], 
                                  data: Dict[str, Any]) -> List[ProductivityInsight]:
        """Generate detailed insights from detected patterns"""
        insights = []
        
        for pattern in patterns:
            insight = self._pattern_to_insight(pattern, data)
            if insight:
                insights.append(insight)
        
        # Add general insights if we have enough data
        tasks = data.get('tasks', [])
        if len(tasks) >= 5:
            general_insights = self._generate_general_insights(data)
            insights.extend(general_insights)
        
        return insights
    
    def _pattern_to_insight(self, pattern: ProductivityPattern, 
                          data: Dict[str, Any]) -> Optional[ProductivityInsight]:
        """Convert a pattern to an actionable insight"""
        if pattern.pattern_type == "peak_hours":
            hour_match = re.search(r'(\d+):00', pattern.description)
            if hour_match:
                hour = int(hour_match.group(1))
                time_period = "morning" if 6 <= hour < 12 else "afternoon" if 12 <= hour < 18 else "evening"
                
                return ProductivityInsight(
                    insight=f"You're most productive in the {time_period} around {hour}:00",
                    category="timing",
                    confidence=pattern.strength,
                    supporting_data={"peak_hour": hour, "strength": pattern.strength},
                    recommendation=f"Schedule your most important tasks around {hour}:00 for optimal performance"
                )
        
        elif pattern.pattern_type == "day_of_week":
            day_match = re.search(r'Most productive on (\w+)', pattern.description)
            if day_match:
                day = day_match.group(1)
                
                return ProductivityInsight(
                    insight=f"You consistently perform best on {day}s",
                    category="scheduling",
                    confidence=pattern.strength,
                    supporting_data={"best_day": day, "strength": pattern.strength},
                    recommendation=f"Plan your most challenging tasks for {day}s when you're at peak performance"
                )
        
        elif pattern.pattern_type == "high_performance_category":
            category_match = re.search(r'Excellent performance in (\w+) tasks', pattern.description)
            if category_match:
                category = category_match.group(1)
                
                return ProductivityInsight(
                    insight=f"You excel at {category} tasks with high completion rates",
                    category="strengths",
                    confidence=pattern.strength,
                    supporting_data={"category": category, "completion_rate": pattern.strength},
                    recommendation=f"Leverage your {category} expertise to mentor others or take on more complex {category} projects"
                )
        
        elif pattern.pattern_type == "completion_trend":
            if pattern.trend == "improving":
                return ProductivityInsight(
                    insight="Your task completion rate has been steadily improving",
                    category="progress",
                    confidence=pattern.strength,
                    supporting_data={"trend": "improving", "strength": pattern.strength},
                    recommendation="Keep up the excellent momentum and consider documenting what's working well"
                )
            else:
                return ProductivityInsight(
                    insight="Your task completion rate has been declining recently",
                    category="concerns",
                    confidence=pattern.strength,
                    supporting_data={"trend": "declining", "strength": pattern.strength},
                    recommendation="Consider reviewing your workload and task management strategies"
                )
        
        return None
    
    def _generate_general_insights(self, data: Dict[str, Any]) -> List[ProductivityInsight]:
        """Generate general productivity insights"""
        insights = []
        tasks = data.get('tasks', [])
        
        # Overall completion rate
        completed_tasks = [t for t in tasks if t.get('status') == 'completed' or t.get('completed_at')]
        completion_rate = len(completed_tasks) / len(tasks) if tasks else 0
        
        if completion_rate > 0.8:
            insights.append(ProductivityInsight(
                insight=f"Excellent task completion rate of {completion_rate:.1%}",
                category="performance",
                confidence=0.9,
                supporting_data={"completion_rate": completion_rate},
                recommendation="Maintain your current task management approach"
            ))
        elif completion_rate < 0.5:
            insights.append(ProductivityInsight(
                insight=f"Task completion rate of {completion_rate:.1%} has room for improvement",
                category="improvement",
                confidence=0.8,
                supporting_data={"completion_rate": completion_rate},
                recommendation="Consider breaking large tasks into smaller, manageable pieces"
            ))
        
        # Task estimation accuracy (if we have estimated vs actual hours)
        estimated_tasks = [t for t in tasks if t.get('estimated_hours') and t.get('actual_hours')]
        if len(estimated_tasks) >= 3:
            estimation_errors = []
            for task in estimated_tasks:
                estimated = task['estimated_hours']
                actual = task['actual_hours']
                error = abs(actual - estimated) / estimated if estimated > 0 else 1.0
                estimation_errors.append(error)
            
            avg_error = statistics.mean(estimation_errors)
            if avg_error < 0.2:  # Within 20% on average
                insights.append(ProductivityInsight(
                    insight="Your time estimation skills are quite accurate",
                    category="planning",
                    confidence=0.8,
                    supporting_data={"avg_estimation_error": avg_error},
                    recommendation="Continue using your current estimation approach"
                ))
            elif avg_error > 0.5:  # Off by more than 50% on average
                insights.append(ProductivityInsight(
                    insight="Time estimates tend to be significantly off from actual completion times",
                    category="planning",
                    confidence=0.8,
                    supporting_data={"avg_estimation_error": avg_error},
                    recommendation="Track actual time spent on tasks to improve future estimates"
                ))
        
        return insights
    
    def _create_pattern_summary(self, patterns: List[ProductivityPattern]) -> Dict[str, Any]:
        """Create a summary of detected patterns"""
        summary = {
            "total_patterns": len(patterns),
            "pattern_types": list(set(p.pattern_type for p in patterns)),
            "strongest_pattern": None,
            "average_confidence": 0.0
        }
        
        if patterns:
            # Find strongest pattern
            strongest = max(patterns, key=lambda p: p.strength)
            summary["strongest_pattern"] = {
                "type": strongest.pattern_type,
                "description": strongest.description,
                "strength": strongest.strength
            }
            
            # Calculate average confidence
            summary["average_confidence"] = statistics.mean(p.strength for p in patterns)
        
        return summary
    
    def _calculate_overall_confidence(self, insights: List[ProductivityInsight], 
                                    data: Dict[str, Any]) -> float:
        """Calculate overall confidence in the insights"""
        if not insights:
            return 0.0
        
        # Base confidence on individual insight confidences
        avg_confidence = statistics.mean(insight.confidence for insight in insights)
        
        # Adjust based on data quality
        tasks = data.get('tasks', [])
        data_quality_factor = min(1.0, len(tasks) / 20)  # Full confidence with 20+ tasks
        
        # Boost confidence if we have diverse insights
        categories = set(insight.category for insight in insights)
        diversity_factor = min(1.0, len(categories) / 3)  # Full boost with 3+ categories
        
        final_confidence = avg_confidence * data_quality_factor * (0.8 + 0.2 * diversity_factor)
        
        return max(0.0, min(1.0, final_confidence))
    
    def _determine_analysis_period(self, data: Dict[str, Any]) -> str:
        """Determine the time period covered by the analysis"""
        tasks = data.get('tasks', [])
        
        if not tasks:
            return "No data"
        
        # Find date range
        dates = []
        for task in tasks:
            created_at = task.get('created_at')
            if created_at:
                try:
                    if isinstance(created_at, str):
                        dt = datetime.fromisoformat(created_at.replace('Z', '+00:00'))
                    else:
                        dt = created_at
                    dates.append(dt)
                except (ValueError, TypeError):
                    continue
        
        if not dates:
            return f"Last {len(tasks)} tasks"
        
        earliest = min(dates)
        latest = max(dates)
        days = (latest - earliest).days
        
        if days <= 7:
            return "Last week"
        elif days <= 30:
            return "Last month"
        elif days <= 90:
            return "Last 3 months"
        else:
            return f"Last {days} days"
    
    def _assess_data_quality(self, data: Dict[str, Any]) -> str:
        """Assess the quality of the input data"""
        tasks = data.get('tasks', [])
        
        if len(tasks) < 5:
            return "Limited data - insights may be less reliable"
        elif len(tasks) < 15:
            return "Moderate data quality - insights are reasonably reliable"
        else:
            return "Good data quality - insights are highly reliable"
    
    def _generate_fallback_insights(self, reason: str) -> ProductivityInsights:
        """Generate fallback insights when analysis fails"""
        return ProductivityInsights(
            insights=[
                "Focus on completing one task at a time to build momentum",
                "Break large tasks into smaller, manageable pieces",
                "Set specific time blocks for different types of work"
            ],
            recommendations=[
                "Track your tasks consistently to enable better insights",
                "Note completion times to improve future planning",
                "Categorize tasks to identify patterns over time"
            ],
            patterns={
                "total_patterns": 0,
                "pattern_types": [],
                "strongest_pattern": None,
                "average_confidence": 0.0
            },
            confidence=0.3,
            detailed_insights=[],
            detected_patterns=[],
            analysis_period="Unknown",
            data_quality=f"Analysis failed: {reason}"
        )
    
    def to_dict(self, result: ProductivityInsights) -> Dict[str, Any]:
        """Convert ProductivityInsights to dictionary"""
        return {
            "insights": result.insights,
            "recommendations": result.recommendations,
            "patterns": result.patterns,
            "confidence": result.confidence,
            "analysis_period": result.analysis_period,
            "data_quality": result.data_quality,
            "detailed_insights": [
                {
                    "insight": insight.insight,
                    "category": insight.category,
                    "confidence": insight.confidence,
                    "supporting_data": insight.supporting_data,
                    "recommendation": insight.recommendation
                }
                for insight in result.detailed_insights
            ],
            "detected_patterns": [
                {
                    "pattern_type": pattern.pattern_type,
                    "description": pattern.description,
                    "strength": pattern.strength,
                    "data_points": pattern.data_points,
                    "trend": pattern.trend
                }
                for pattern in result.detected_patterns
            ]
        }