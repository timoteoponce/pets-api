FROM eclipse-temurin:21 as jre-build
# Create a custom Java runtime
RUN $JAVA_HOME/bin/jlink \
         --add-modules java.base \
         --add-modules java.sql \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime
# Define your base image
FROM debian:buster-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME
# Continue with your application deployment
RUN mkdir /opt/app
COPY target/app.jar /opt/app
EXPOSE 3000
CMD ["java", "-jar", "/opt/app/app.jar"]
