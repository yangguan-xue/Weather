# 多阶段构建：第一阶段构建 JAR
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制 Maven 配置文件
COPY pom.xml .

# 下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 编译并打包
RUN mvn clean package -DskipTests

# 第二阶段：运行
FROM openjdk:17-jre-alpine

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

WORKDIR /app

# 暴露端口（必须与微信云托管配置一致）
EXPOSE 8088

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/weather-broadcast-*.jar app.jar

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]