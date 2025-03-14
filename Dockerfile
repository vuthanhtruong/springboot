# Sử dụng OpenJDK 17 làm nền tảng
FROM openjdk:17-jdk

# Đặt thư mục làm việc trong container
WORKDIR /app

# Sao chép file JAR đã build vào container
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# Chạy ứng dụng
CMD ["java", "-jar", "app.jar"]
