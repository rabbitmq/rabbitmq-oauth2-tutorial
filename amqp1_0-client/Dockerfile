############################
# STEP 1 build executable binary
############################

FROM maven:3.8.4-jdk-11 as builder

WORKDIR /workspace

COPY src ./src
COPY pom.xml ./

RUN mvn


############################
# STEP 2 build a small image
############################
FROM openjdk:11
WORKDIR /
# Copy our static executable.
COPY --from=builder /workspace/target .
ENTRYPOINT ["java", "-jar", "amqp1_0-client-1.0-SNAPSHOT-jar-with-dependencies.jar"]
