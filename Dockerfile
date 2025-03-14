# Sử dụng Ubuntu làm nền tảng
FROM ubuntu:latest

# Cập nhật package list và cài đặt OpenJDK 17
RUN apt-get update && apt-get install -y openjdk-17-jdk

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# Sao chép file JAR vào container (cần build trước khi chạy Docker)
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# Mở cổng 8080 cho ứng dụng Spring Boot
EXPOSE 8080

# Lệnh chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]
