# Sử dụng Maven và OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy toàn bộ mã nguồn vào container
COPY . .

# Cấu hình Java version trong Maven build
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
RUN export JAVA_HOME

# Chạy Maven để build
RUN chmod +x ./mvnw && ./mvnw -DskipTests clean install
