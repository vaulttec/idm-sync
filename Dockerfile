FROM eclipse-temurin:17-jre-alpine
RUN adduser -S -u 1000 spring && \
    mkdir -p /app/logs && \
    chown -R spring /app
USER spring
ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar
WORKDIR /app
VOLUME /tmp /app/logs
EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -Dlogging.file.path=/app/logs -jar /app/app.jar
