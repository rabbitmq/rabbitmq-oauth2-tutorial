############################
# STEP 1 build executable binary
############################

FROM openjdk:8

WORKDIR /uaa

COPY uaa ./

RUN ./gradlew cleanCargoConfDir assemble
RUN ./gradlew assemble

ENV CLOUDFOUNDRY_CONFIG_PATH /etc/uaa
ENV CLOUD_FOUNDRY_CONFIG_PATH /etc/uaa

ENTRYPOINT ["./gradlew"]
CMD ["run"]
