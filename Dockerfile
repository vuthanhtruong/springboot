# Giai đoạn Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Sao chép mã nguồn và build ứng dụng
COPY . .
RUN mvn clean package -DskipTests

# Giai đoạn chạy ứng dụng
FROM openjdk:21-jdk-slim
WORKDIR /app

# Sao chép ứng dụng đã build vào Docker image
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Mở port 8080
EXPOSE 8080

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
