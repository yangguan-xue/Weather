# 使用国内镜像源避免连接问题
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 设置工作目录
WORKDIR /app

# 暴露端口（根据您的应用实际端口修改）
EXPOSE 8080

# 复制具体的JAR文件到容器中并重命名为app.jar
COPY ./target/weather-broadcast-1.0.0.jar app.jar

# 启动应用
CMD ["java", "-jar", "app.jar"]