FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR file
COPY build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8083

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 