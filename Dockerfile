# --- build ---------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# 의존성 레이어 캐시: 빌드 스크립트가 안 바뀌면 재다운로드 안 함
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# --- runtime -------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

# StorageService type=local 업로드 경로 (compose에서 볼륨 마운트)
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
