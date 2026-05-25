# Build stage: source code -> executable JAR
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

# Runtime stage: only what is needed to run the API
FROM eclipse-temurin:25-jre AS runtime

RUN groupadd --system --gid 10001 festi \
    && useradd --system --uid 10001 --gid festi \
       --home-dir /app --create-home --no-log-init festi \
    && mkdir -p /data/festi/images \
    && chown -R 10001:10001 /app /data/festi/images

WORKDIR /app

COPY --from=build --chown=10001:10001 \
    /workspace/build/libs/festi-backend-*.jar /app/app.jar

LABEL org.opencontainers.image.source="https://github.com/Festi-app/Festi-backend"

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
