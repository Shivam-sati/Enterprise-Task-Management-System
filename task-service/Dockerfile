FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/task-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]