# First stage: Build the JAR file using Maven
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the code
COPY src ./src

# Build the application (skip tests for now)
RUN mvn clean package -DskipTests

# Second stage: Run the JAR
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from the first stage
COPY --from=build /app/target/*.jar app.jar

# Create upload directories
RUN mkdir -p /app/uploads/videos /app/uploads/thumbnails

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]