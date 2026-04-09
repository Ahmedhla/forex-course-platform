# Dockerfile
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the jar file
COPY target/*.jar app.jar

# Create upload directories
RUN mkdir -p /app/uploads/videos /app/uploads/thumbnails

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/courses/public || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]