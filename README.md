# AWI Nearrealtime SOS

Custom backend of the [52°North SOS](https://github.com/52North/SOS) for the AWI NearRealTime database.

## Configuration

The connection to the NRT database can be configured in `src/main/webapp/WEB-INF/hibernate.properties` or `<webapp>/WEB-INF/hibernate.properties`. A template can be found at [`src/main/webapp/WEB-INF/hibernate.properties.template`](https://github.com/52North/awi-nearrealtime-sos/blob/master/src/main/webapp/WEB-INF/hibernate.properties.example)

SOS metadata (like Service Provider and Service Identification or the external URL of the service can be configured in [`src/main/webapp/configuration.json`](https://github.com/52North/awi-nearrealtime-sos/blob/master/src/main/webapp/configuration.json) and `<webapp>/configuration.json` respectively

Logging is done using [Logback](https://logback.qos.ch/) and can be configured in [`src/main/resources/logback.xml`](https://github.com/52North/awi-nearrealtime-sos/blob/master/src/main/resources/logback.xml) and `<webapp>/classes/logback.xml` respectively.

## Building

The service requires Java 8 and [Maven](https://maven.apache.org/):

```
mvn clean install
```

The WAR file can be found at `target/de.awi.sos.ui.war`


## Deployment

The WAR file can be deployed in a Java Application Server of your choice. Please adjust the configuration files (especially `hibernate.properties` either prior to building or in the WAR file.

### Docker

There is a [`Dockerfile`](https://github.com/52North/awi-nearrealtime-sos/blob/master/Dockerfile) that creates a [Jetty](https://www.eclipse.org/jetty/) deployment:

```sh
docker build -t awi/nearrealtime-sos:latest .
```

```sh
docker run -it -p 8080:8080 \
  -v ./logback.xml:/var/lib/jetty/webapps/ROOT/WEB-INF/classes/logback.xml:ro
  -v ./hibernate.properties:/var/lib/jetty/webapps/ROOT/WEB-INF/hibernate.properties:ro
  -v ./configuration.json:/var/lib/jetty/webapps/ROOT/configuration.json:ro
  awi/nearrealtime-sos:latest
```

Be aware that you have to link the database to the container or have both containers on the same Docker network.

After this the SOS should be accessible at http://localhost:8080/service?service=SOS&request=GetCapabilities

A `docker-compose` example deployment can be found [here](https://github.com/52North/awi-nearrealtime).

## Development

An extract of the AWI NearRealTime database can be found [here](https://github.com/52North/awi-nearrealtime-example-db) as a Docker image.
