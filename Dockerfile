FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY build/libs/*.jar petmate-service.jar
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","petmate-service.jar"]