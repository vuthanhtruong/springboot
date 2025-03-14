# Sử dụng Maven với JDK 21 làm base image
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Thiết lập thư mục làm việc
WORKDIR /app

# Sao chép toàn bộ mã nguồn vào container
COPY . .

# Cấp quyền thực thi cho mvnw (nếu có)
RUN chmod +x ./mvnw

# Chạy Maven để build
RUN ./mvnw -B -DskipTests clean install
