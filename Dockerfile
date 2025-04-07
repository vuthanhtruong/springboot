# Giai đoạn Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Sao chép chỉ những tệp cần thiết thay vì toàn bộ thư mục để giảm kích thước Docker image
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Giai đoạn Chạy Ứng Dụng
FROM openjdk:21-jdk-slim
WORKDIR /app

# Sao chép ứng dụng đã build từ giai đoạn trước vào giai đoạn chạy
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Cấu hình port cho ứng dụng
EXPOSE 8080

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
