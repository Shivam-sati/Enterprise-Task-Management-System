package com.taskmanagement.auth.health;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

// TODO: Uncomment when Spring Boot Actuator dependencies are resolved
// import org.springframework.boot.actuator.health.Health;
// import org.springframework.boot.actuator.health.HealthIndicator;

@Component
public class DatabaseHealthIndicator /* implements HealthIndicator */ {

    private final MongoTemplate mongoTemplate;

    public DatabaseHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // TODO: Uncomment when actuator dependencies are resolved
    /*
    @Override
    public Health health() {
        try {
            // Test database connectivity
            mongoTemplate.getCollection("users").estimatedDocumentCount();
            return Health.up()
                    .withDetail("database", "MongoDB")
                    .withDetail("status", "Connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "MongoDB")
                    .withDetail("status", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
    */
}