FROM openjdk:11 AS BUILDER

ARG UAA_VERSION

WORKDIR /workspace

RUN git clone https://github.com/cloudfoundry/uaa.git \
    && cd uaa \
    && git checkout v${UAA_VERSION}

RUN cd uaa \
    && ./gradlew assemble -Pversion=${UAA_VERSION}

FROM tomcat:9

ARG UAA_VERSION

COPY --from=BUILDER \
    /workspace/uaa/statsd/build/libs/cloudfoundry-identity-statsd-${UAA_VERSION}.war \
    ${CATALINA_HOME}/webapps/statsd.war

COPY --from=BUILDER \
    /workspace/uaa/uaa/build/libs/cloudfoundry-identity-uaa-${UAA_VERSION}.war \
    ${CATALINA_HOME}/webapps/uaa.war

COPY tomcat-conf/context.xml \
    ${CATALINA_HOME}/conf/context.xml

COPY uaa/log4j2.properties /etc/uaa/log4j2.properties
COPY uaa/uaa.yml /etc/uaa/uaa.yml

ENV CLOUDFOUNDRY_CONFIG_PATH /etc/uaa
# ENV JAVA_OPTS "-Dlog4j.configurationFile=/etc/uaa/log4j2.properties"
