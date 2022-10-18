FROM gradle:7-jdk17-alpine AS nk2-pf-ng_builder

WORKDIR /srv

COPY . .
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
RUN gradle bootJar

FROM openjdk:17-alpine

EXPOSE 3030
WORKDIR /srv

COPY --from=nk2-pf-ng_builder /srv/build/libs/*.jar ./service.jar

ENTRYPOINT ["java", "-jar", "service.jar"]
