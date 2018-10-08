FROM maven:3.5-jdk-8-slim as builder

WORKDIR /app

COPY . .

RUN mvn -q clean package

FROM jboss/keycloak:3.4.3.Final

RUN curl -O -L 'https://github.com/vishnubob/wait-for-it/raw/master/wait-for-it.sh' && chmod +x wait-for-it.sh

USER root

# Install envsubst
RUN yum install -y gettext

USER jboss

RUN mkdir /opt/jboss/.ortolang
COPY --chown=jboss:jboss src/main/resources/discovery.properties.sample /opt/jboss/.ortolang/discovery.properties

COPY --from=builder /app/target/discovery.war /opt/jboss/keycloak/standalone/deployments/

ENTRYPOINT [""]

CMD cp /opt/jboss/.ortolang/discovery.properties /tmp/ && \
    envsubst < /tmp/discovery.properties > /opt/jboss/.ortolang/discovery.properties && \
    /opt/jboss/docker-entrypoint.sh -b 0.0.0.0