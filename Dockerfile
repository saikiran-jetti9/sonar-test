FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon 

FROM openjdk:17
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/bmg-deliver-backend-0.0.1-SNAPSHOT.jar /app
ENTRYPOINT ["java","-jar","/app/bmg-deliver-backend-0.0.1-SNAPSHOT.jar"]