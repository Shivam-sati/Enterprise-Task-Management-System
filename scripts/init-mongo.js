// MongoDB initialization script
db = db.getSiblingDB('taskmanagement');

// Create collections
db.createCollection('users');
db.createCollection('tasks');
db.createCollection('subtasks');
db.createCollection('notifications');
db.createCollection('sharedTasks');
db.createCollection('activityFeed');
db.createCollection('activityLogs');
db.createCollection('productivityMetrics');

// Create indexes for users collection
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "userId": 1 }, { unique: true });
db.users.createIndex({ "provider": 1, "providerId": 1 });

// Create indexes for tasks collection
db.tasks.createIndex({ "userId": 1, "status": 1 });
db.tasks.createIndex({ "userId": 1, "dueDate": 1 });
db.tasks.createIndex({ "tags": 1 });
db.tasks.createIndex({ "title": "text", "description": "text" });
db.tasks.createIndex({ "createdAt": -1 });

// Create indexes for notifications collection
db.notifications.createIndex({ "userId": 1, "status": 1 });
db.notifications.createIndex({ "createdAt": -1 });
db.notifications.createIndex({ "type": 1, "status": 1 });

// Create indexes for activity logs
db.activityLogs.createIndex({ "userId": 1, "timestamp": -1 });
db.activityLogs.createIndex({ "entityType": 1, "entityId": 1 });

print('Database initialization completed successfully!');