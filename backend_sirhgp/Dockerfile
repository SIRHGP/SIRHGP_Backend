FROM maven:3.9.12-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    for attempt in 1 2 3; do \
      mvn -B -Dmaven.wagon.http.retryHandler.count=5 -DskipTests dependency:go-offline && exit 0; \
      if [ "$attempt" -eq 3 ]; then exit 1; fi; \
      sleep 2; \
    done
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    for attempt in 1 2 3; do \
      mvn -B -Dmaven.wagon.http.retryHandler.count=5 -DskipTests package && exit 0; \
      if [ "$attempt" -eq 3 ]; then exit 1; fi; \
      sleep 2; \
    done

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 app

WORKDIR /app
COPY --from=build /workspace/target/backend-sirhgp-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /app/uploads && chown -R app:app /app

USER app
EXPOSE 8080
ENV UPLOAD_DIR=/app/uploads
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
