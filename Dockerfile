FROM maven:3.6.0-jdk-11-slim as build-env
ADD . /build/touchmenu
WORKDIR /build/touchmenu
RUN mvn clean install
RUN mkdir -p /app
RUN mv target/touchmenu*.jar /app/touchmenu.jar

FROM gcr.io/distroless/java:11
COPY --from=build-env /app /app
WORKDIR /app
CMD ["touchmenu.jar"]