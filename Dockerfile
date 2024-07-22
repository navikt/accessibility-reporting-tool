FROM openjdk:17-alpine as builder
COPY . .
RUN ./gradlew build -i -x test

FROM gcr.io/distroless/java17-debian11
COPY --from=builder app/build/libs/app-all.jar app/app.jar
ENV PORT=8080
EXPOSE $PORT
WORKDIR app
CMD ["app.jar"]