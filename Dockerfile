FROM openjdk:17-alpine
RUN adduser -S -u 1000 springboot && \
    mkdir -p /app/logs && \
    chown -R springboot /app
USER springboot
ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar
WORKDIR /app
VOLUME /tmp /app/logs
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dlogging.path=/app/logs -jar /app/app.jar
