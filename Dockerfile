FROM eclipse-temurin:21
RUN mkdir /opt/app
COPY target/app.jar /opt/app
EXPOSE 3000
CMD ["java", "-jar", "/opt/app/app.jar"]
