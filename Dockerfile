# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17.0.2-slim-buster

# Create a non-root user
RUN groupadd -g 10240 worker && \
    useradd -r -u 10240 -g worker worker

# Set the working directory
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership of the jar file
RUN chown worker:worker /app/app.jar

# Switch to non-root user
USER worker:worker

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-Xmx2g", "-jar", "/app/app.jar"]