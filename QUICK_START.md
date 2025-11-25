# 🚀 **Quick Start Guide**

## **Prerequisites Check**
```bash
# Check Java version (need 17+)
java -version

# Check Maven version (need 3.8+)
mvn -version

# Check Docker version
docker --version
docker-compose --version
```

## **1. Build the Project**
```bash
# Build all services
mvn clean package -DskipTests
```

## **2. Start the System**

### **Option A: Using Scripts (Recommended)**
```bash
# Linux/Mac
chmod +x scripts/start-services.sh
./scripts/start-services.sh

# Windows
scripts\start-services.bat
```

### **Option B: Manual Docker Compose**
```bash
# Start infrastructure first
docker-compose up -d mongodb redis rabbitmq

# Wait 30 seconds, then start services
docker-compose up -d eureka-server
sleep 30
docker-compose up -d api-gateway auth-service task-service notification-service
docker-compose up -d collaboration-service analytics-service ai-service
docker-compose up -d todo-react
```

## **3. Verify Services**

### **Check Service Health**
```bash
# Eureka Dashboard
curl http://localhost:8761

# API Gateway Health
curl http://localhost:8080/actuator/health

# Individual Service Health
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Task
curl http://localhost:8083/actuator/health  # Notification
curl http://localhost:8084/actuator/health  # Collaboration
curl http://localhost:8085/actuator/health  # Analytics
curl http://localhost:8086/actuator/health  # AI
```

### **Access Points**
- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Eureka Dashboard**: http://localhost:8761
- **RabbitMQ Management**: http://localhost:15672 (admin/password)

## **4. Test the APIs**

### **Register a User**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### **Login**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### **Create a Task** (use JWT token from login)
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "My First Task",
    "description": "Testing the task management system",
    "priority": "HIGH"
  }'
```

## **5. Test Advanced Features**

### **AI Service**
```bash
curl -X POST http://localhost:8086/api/ai/parse-task \
  -H "Content-Type: application/json" \
  -d '{"text": "Schedule meeting with team tomorrow at 2 PM"}'
```

### **Analytics Service**
```bash
curl http://localhost:8085/api/analytics/productivity/user123
```

### **Collaboration Service**
```bash
curl -X POST http://localhost:8084/api/collaboration/share-task \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "task123",
    "userId": "user456",
    "permission": "EDIT"
  }'
```

## **6. Monitor the System**

### **Service Discovery**
Visit http://localhost:8761 to see all registered services

### **Health Monitoring**
```bash
# Check all services are UP
curl http://localhost:8080/actuator/health | jq
curl http://localhost:8081/actuator/health | jq
curl http://localhost:8082/actuator/health | jq
curl http://localhost:8083/actuator/health | jq
curl http://localhost:8084/actuator/health | jq
curl http://localhost:8085/actuator/health | jq
curl http://localhost:8086/actuator/health | jq
```

### **Message Queue**
Visit http://localhost:15672 (admin/password) to monitor RabbitMQ

## **7. Stop the System**
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## **🔧 Troubleshooting**

### **Common Issues**

1. **Port Already in Use**
   ```bash
   # Check what's using the port
   netstat -tulpn | grep :8080
   
   # Kill the process
   kill -9 <PID>
   ```

2. **Services Not Registering with Eureka**
   - Wait 30-60 seconds for registration
   - Check Eureka dashboard at http://localhost:8761
   - Verify network connectivity

3. **Database Connection Issues**
   - Ensure MongoDB Atlas credentials are correct
   - Check network connectivity
   - Verify connection string format

4. **Memory Issues**
   ```bash
   # Increase Docker memory limit
   # Docker Desktop -> Settings -> Resources -> Memory (8GB recommended)
   ```

### **Logs**
```bash
# View service logs
docker-compose logs -f eureka-server
docker-compose logs -f api-gateway
docker-compose logs -f auth-service
docker-compose logs -f task-service
docker-compose logs -f notification-service
docker-compose logs -f collaboration-service
docker-compose logs -f analytics-service
docker-compose logs -f ai-service
docker-compose logs -f todo-react
```

## **🎯 Success Indicators**

✅ All 8 services show "UP" in Eureka dashboard
✅ API Gateway health check returns 200
✅ Frontend loads at http://localhost:3000
✅ Can register and login users
✅ Can create and manage tasks
✅ All service health endpoints return 200

## **🚀 Next Steps**

1. **Explore the Frontend** - http://localhost:3000
2. **Test API Endpoints** - Use Postman or curl
3. **Monitor Services** - Check Eureka and health endpoints
4. **Review Code** - Understand the microservices architecture
5. **Customize Features** - Add your own enhancements

---

**Your enterprise-grade microservices system is now running! 🎉**