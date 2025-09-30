FROM azul/zulu-openjdk-alpine:21-jre-headless

RUN apk update && apk add --no-cache curl

ARG PROJECT_JAR_NAME
ENV PROJECT_JAR_NAME=${PROJECT_JAR_NAME}

COPY ./target/${PROJECT_JAR_NAME}.jar /opt/api-gateway/api-gateway.jar
COPY ./src/main/resources/logback.xml /api-gateway/conf/logback.xml

EXPOSE 7000 8080
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && chown -R appuser:appgroup /opt/api-gateway/
USER appuser

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/api-gateway/api-gateway.jar"]
