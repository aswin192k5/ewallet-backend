FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy everything
COPY . .

# Install bash (needed for mvnw)
RUN apk add --no-cache bash

# Fix permissions
RUN chmod +x mvnw
RUN chmod +x .mvn/wrapper/maven-wrapper.jar || true

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# Run the jar explicitly
CMD ["java", "-jar", "target/backend-1.0.0.jar"]
