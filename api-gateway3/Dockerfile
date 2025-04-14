FROM azul/zulu-openjdk-alpine:17.0.14-17.56-jre-headless

RUN apk update && apk add --no-cache curl

ARG PROJECT_JAR_NAME
ENV PROJECT_JAR_NAME ${PROJECT_JAR_NAME}

ADD ./target/${PROJECT_JAR_NAME}.jar /opt/api-gateway3/api-gateway3.jar
ADD ./src/main/resources/logback.xml /api-gateway3/conf/logback.xml

EXPOSE 7000 8080
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown -R appuser:appgroup /opt/api-gateway3/
USER appuser

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/api-gateway3/api-gateway3.jar"]
