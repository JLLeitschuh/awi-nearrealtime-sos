FROM maven:3-jdk-8 AS BUILD

RUN mkdir -p /app/
WORKDIR /app/

COPY pom.xml etc/ /app/

RUN mvn -f pom.xml -T1.0C -B -P no-download \
  dependency:resolve dependency:resolve-plugins

COPY src /app/src

RUN mvn -f pom.xml -B -P no-download install

FROM jetty:jre8-alpine

ENV JAVA_OPTIONS="-Djava.awt.headless=true"

COPY --from=BUILD /app/target/de.awi.sos.ui /var/lib/jetty/webapps/ROOT
