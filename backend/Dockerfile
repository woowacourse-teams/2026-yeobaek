# 배포용 런타임 이미지. jar 빌드는 CI(GitHub Actions)에서 수행하고 여기서는 담기만 한다.
# 빌드: ./gradlew bootJar && docker build -t <계정>/yeobaek-backend .
FROM eclipse-temurin:21-jre

COPY build/libs/backend-0.0.1-SNAPSHOT.jar /app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
