FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY build/libs/*.jar mate-service.jar
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","mate-service.jar"]