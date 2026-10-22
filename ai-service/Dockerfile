FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system veggiepal \
    && useradd --system --gid veggiepal --home-dir /app --shell /usr/sbin/nologin veggiepal \
    && mkdir -p /app/tmp/veggiepal-ai-video \
    && chown -R veggiepal:veggiepal /app
WORKDIR /app
COPY --from=build --chown=veggiepal:veggiepal /app/target/*.jar app.jar
ENV AI_VIDEO_STORAGE_DIR=/app/tmp/veggiepal-ai-video
USER veggiepal
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
