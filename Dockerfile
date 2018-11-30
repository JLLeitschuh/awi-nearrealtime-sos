FROM maven:3-jdk-8 AS BUILD

RUN mkdir -p /app/
WORKDIR /app/

COPY pom.xml /app
COPY etc /app/etc

RUN mvn dependency:resolve dependency:resolve-plugins

COPY src /app/src

RUN mvn install

FROM jetty:jre8-alpine
COPY --from=BUILD /app/target/de.awi.sos.ui /var/lib/jetty/webapps/ROOT
