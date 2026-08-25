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

# Giới hạn RAM tối đa 350MB và ưu tiên IPv4
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms150m -Xmx350m -XX:+UseG1GC -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]