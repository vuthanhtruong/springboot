# Sử dụng OpenJDK 17 làm nền tảng
FROM openjdk:17-jdk

# Đặt thư mục làm việc trong container
WORKDIR /app

# Sao chép toàn bộ mã nguồn vào container
COPY . .

# Cài đặt Maven và build ứng dụng
RUN apt-get update && apt-get install -y maven && mvn clean package

# Chạy ứng dụng
CMD ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]
