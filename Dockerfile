# Sử dụng Maven với JDK 21 làm base image
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Thiết lập thư mục làm việc
WORKDIR /app

# Sao chép toàn bộ mã nguồn vào container
COPY . .

# Đặt JAVA_HOME cho Maven
ENV JAVA_HOME=/usr/local/openjdk-21
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Cấp quyền thực thi cho mvnw và chạy build
RUN chmod +x ./mvnw && ./mvnw -DskipTests clean install
