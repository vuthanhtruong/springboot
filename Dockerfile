# Sử dụng Maven với JDK 21 để build
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Thiết lập thư mục làm việc
WORKDIR /app

# Sao chép mã nguồn vào container
COPY . .

# Build project, tạo WAR file
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Sử dụng Tomcat làm runtime để chạy WAR
FROM tomcat:10.1-jdk21

# Thiết lập thư mục làm việc
WORKDIR /usr/local/tomcat/webapps/

# Copy WAR file từ build trước vào thư mục webapps của Tomcat
COPY --from=build /app/target/*.war app.war

# Mở cổng 8080
EXPOSE 8081


# Chạy Tomcat
CMD ["catalina.sh", "run"]
