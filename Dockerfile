# Sử dụng Maven với JDK 21 làm base image
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Thiết lập thư mục làm việc
WORKDIR /app

# Sao chép toàn bộ mã nguồn vào container
COPY . .

# Chạy Maven để build
RUN chmod +x ./mvnw && ./mvnw -B -DskipTests clean install
