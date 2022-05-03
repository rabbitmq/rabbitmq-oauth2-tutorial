############################
# STEP 1 build executable binary
############################

FROM openjdk:8

WORKDIR /uaa

COPY uaa ./

RUN ./gradlew cleanCargoConfDir assemble
RUN ./gradlew assemble

ENTRYPOINT ["./gradlew"]
CMD ["run"]
