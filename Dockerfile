# Stage 1: Build JAR
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Run App
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Tối ưu RAM: Bỏ giới hạn cứng Metaspace, Heap 200MB, nạp đúng Timezone VN
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms64m -Xmx200m -Xss256k -XX:+UseSerialGC -Duser.timezone=Asia/Ho_Chi_Minh -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]