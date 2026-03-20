FROM eclipse-temurin:17-jre-alpine

EXPOSE 8080

COPY app.jar /app.jar

RUN apk update                  && \
    apk add --no-cache curl

ENTRYPOINT [ "java", "-jar", "/app.jar" ]
