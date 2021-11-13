FROM openjdk:17-slim

RUN mkdir /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY deployment/build/docker /

ENTRYPOINT ["java","-Dspring.profiles.active=deployed","-cp","app:app/lib/*","com.github.pacificengine.Application"]