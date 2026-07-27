FROM openjdk:17-jdk-slim
LABEL maintainer="fennemalakeshia@gmail.com"
WORKDIR /app
COPY target/campus-trade-api-6773-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]