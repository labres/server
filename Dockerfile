FROM openjdk:11-jre-slim

ARG JAR_FILE
ENV TZ Europe/Berlin

WORKDIR /app
RUN useradd --system --user-group hmx
USER hmx

COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT java -jar app.jar
