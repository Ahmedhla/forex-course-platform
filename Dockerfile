FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Create upload directories
RUN mkdir -p /app/uploads/videos /app/uploads/thumbnails

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/*.jar"]