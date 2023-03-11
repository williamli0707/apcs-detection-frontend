FROM maven:3.8.7-openjdk-18-slim

WORKDIR /app

COPY . .

RUN mvn clean package -Pproduction

RUN ls

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "./target/mytodo-1.0-SNAPSHOT.jar"]
