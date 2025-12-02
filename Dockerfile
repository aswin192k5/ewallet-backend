FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

RUN apk add --no-cache bash
RUN chmod +x mvnw
RUN chmod +x .mvn/wrapper/maven-wrapper.jar || true

RUN ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/*.jar"]
