package com.taskmanagement.task.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskMetrics {

    private final Counter tasksCreated;
    private final Counter tasksCompleted;
    private final Counter tasksDeleted;
    private final Counter subtasksCreated;
    private final Counter searchQueries;
    private final Timer taskCreationTime;
    private final Timer searchTime;
    private final MongoTemplate mongoTemplate;

    public TaskMetrics(MeterRegistry meterRegistry, MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        
        this.tasksCreated = Counter.builder("tasks.created")
                .description("Total number of tasks created")
                .register(meterRegistry);
        
        this.tasksCompleted = Counter.builder("tasks.completed")
                .description("Total number of tasks completed")
                .register(meterRegistry);
        
        this.tasksDeleted = Counter.builder("tasks.deleted")
                .description("Total number of tasks deleted")
                .register(meterRegistry);
        
        this.subtasksCreated = Counter.builder("subtasks.created")
                .description("Total number of subtasks created")
                .register(meterRegistry);
        
        this.searchQueries = Counter.builder("tasks.search.queries")
                .description("Total number of search queries")
                .register(meterRegistry);
        
        this.taskCreationTime = Timer.builder("tasks.creation.time")
                .description("Time taken to create a task")
                .register(meterRegistry);
        
        this.searchTime = Timer.builder("tasks.search.time")
                .description("Time taken to execute search queries")
                .register(meterRegistry);
        
        // Gauge for total active tasks
        Gauge.builder("tasks.active.total", this, TaskMetrics::getActiveTasks)
                .description("Total number of active tasks")
                .register(meterRegistry);
        
        // Gauge for total completed tasks
        Gauge.builder("tasks.completed.total", this, TaskMetrics::getCompletedTasks)
                .description("Total number of completed tasks")
                .register(meterRegistry);
    }

    public void incrementTasksCreated() {
        tasksCreated.increment();
    }

    public void incrementTasksCompleted() {
        tasksCompleted.increment();
    }

    public void incrementTasksDeleted() {
        tasksDeleted.increment();
    }

    public void incrementSubtasksCreated() {
        subtasksCreated.increment();
    }

    public void incrementSearchQueries() {
        searchQueries.increment();
    }

    public Timer.Sample startTaskCreationTimer() {
        return Timer.start();
    }

    public void recordTaskCreationTime(Timer.Sample sample) {
        sample.stop(taskCreationTime);
    }

    public Timer.Sample startSearchTimer() {
        return Timer.start();
    }

    public void recordSearchTime(Timer.Sample sample) {
        sample.stop(searchTime);
    }

    private double getActiveTasks() {
        try {
            return mongoTemplate.getCollection("tasks")
                    .countDocuments(org.bson.Document.parse("{\"status\": {\"$ne\": \"COMPLETED\"}}"));
        } catch (Exception e) {
            return 0;
        }
    }

    private double getCompletedTasks() {
        try {
            return mongoTemplate.getCollection("tasks")
                    .countDocuments(org.bson.Document.parse("{\"status\": \"COMPLETED\"}"));
        } catch (Exception e) {
            return 0;
        }
    }
}