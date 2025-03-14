# Sử dụng Maven với JDK 21 làm base image
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Thiết lập thư mục làm việc
WORKDIR /app

# Sao chép toàn bộ mã nguồn vào container
COPY . .

# Cấp quyền thực thi cho mvnw và chạy build
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Tạo image runtime từ JDK 21
FROM eclipse-temurin:21-jdk

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy file JAR từ bước build trước
COPY --from=build /app/target/*.jar app.jar

# Expose cổng chạy web (thay đổi tùy app)
EXPOSE 8080

# Lệnh chạy ứng dụng
CMD ["java", "-jar", "app.jar"]
