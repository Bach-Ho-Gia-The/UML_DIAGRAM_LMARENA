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

# Tối ưu RAM cho gói 512MB: Dùng SerialGC, giới hạn Heap 256MB, Metaspace 128MB
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms100m -Xmx256m -Xss256k -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]