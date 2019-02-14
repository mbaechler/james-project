# syntax=docker/dockerfile:experimental
FROM openjdk:8u181-jdk as builder

ENV GIT_VERSION 1:2.11.0-3

# Install Maven
WORKDIR /root
RUN wget https://archive.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz
RUN tar -xvf apache-maven-3.5.4-bin.tar.gz
RUN ln -s /root/apache-maven-3.5.4/bin/mvn /usr/bin/mvn

# Install git
RUN apt-get update
RUN apt-get install -y git

RUN apt-get install -y strace

COPY . .

RUN --mount=type=cache,target=/root/.m2/,id=m2_repository mvn -Dexcludes=:::*-SNAPSHOT -T1C org.apache.maven.plugins:maven-dependency-plugin:3.1.1:go-offline

RUN --mount=type=cache,target=/m2/,id=m2_repository cp -a -v /m2/repository /root/.m2

RUN mvn install -T1C -DskipTests

FROM openjdk:8u181-jre as jre

FROM jre as run-base

WORKDIR /root

COPY --from=builder /root/server/container/cli/target/james-server-cli.jar /root/james-cli.jar
COPY --from=builder /root/server/container/cli/target/james-server-cli.lib /root/james-server-cli.lib

# volume to get access to log files
VOLUME /logs
# volume to mount configuration to be used by james
VOLUME /root/conf
# volume to provide some extensions jar to load into james
VOLUME /root/extensions-jars

FROM run-base as cassandra-guice

# Ports that are used
#
# 25   SMTP without authentication
# 110  POP3
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 993  IMAP with socketTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 110 143 465 587 993 8000

COPY --from=builder /root/server/container/guice/cassandra-guice/target/james-server-cassandra-guice.jar /root/james-server.jar
COPY --from=builder /root/server/container/guice/cassandra-guice/target/james-server-cassandra-guice.lib /root/james-server-cassandra-guice.lib

ENV JVM_OPTIONS=""
ENTRYPOINT java -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ $JVM_OPTIONS -jar james-server.jar

FROM run-base as cassandra-ldap-guice

# Ports that are used
#
# 25   SMTP without authentication
# 110  POP3
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 993  IMAP with socketTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 110 143 465 587 993 8000

COPY --from=builder /root/server/container/guice/cassandra-guice/target/james-server-cassandra-ldap-guice.jar /root/james-server.jar
COPY --from=builder /root/server/container/guice/cassandra-guice/target/james-server-cassandra-ldap-guice.lib /root/james-server-cassandra-ldap-guice.lib

ENV JVM_OPTIONS=""
ENTRYPOINT java -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ $JVM_OPTIONS -jar james-server.jar

FROM run-base as cassandra-rabbitmq-guice

# Ports that are used
#
# 25   SMTP without authentication
# 110  POP3
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 993  IMAP with socketTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 110 143 465 587 993 8000

COPY --from=builder /root/server/container/guice/cassandra-rabbitmq-guice/target/james-server-cassandra-rabbitmq-guice.jar /root/james-server.jar
COPY --from=builder /root/server/container/guice/cassandra-rabbitmq-guice/target/james-server-cassandra-rabbitmq-guice.lib /root/james-server-cassandra-rabbitmq-guice.lib

ENV JVM_OPTIONS=""
ENTRYPOINT java -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ $JVM_OPTIONS -jar james-server.jar

FROM run-base as cassandra-rabbitmq-ldap-guice

# Ports that are used
#
# 25   SMTP without authentication
# 110  POP3
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 993  IMAP with socketTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 110 143 465 587 993 8000

COPY --from=builder /root/server/container/guice/cassandra-rabbitmq-guice/target/james-server-cassandra-rabbitmq-ldap-guice.jar /root/james-server.jar
COPY --from=builder /root/server/container/guice/cassandra-rabbitmq-guice/target/james-server-cassandra-rabbitmq-ldap-guice.lib /root/james-server-cassandra-rabbitmq-ldap-guice.lib

ENV JVM_OPTIONS=""
ENTRYPOINT java -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ $JVM_OPTIONS -jar james-server.jar



FROM run-base as jpa-smtp-mariadb-guice

# Ports that are used
#
# 25   SMTP without authentication
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 143 465 587 8000

COPY --from=builder /root/server/container/guice/jpa-smtp-mariadb/target/james-server-jpa-smtp-mariadb-guice.jar /root/james-server.jar
COPY --from=builder /root/server/container/guice/jpa-smtp-mariadb/target/james-server-jpa-smtp-mariadb-guice.lib /root/james-server-jpa-smtp-mariadb-guice.lib

ENV JVM_OPTIONS=""
ENTRYPOINT java -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ $JVM_OPTIONS -jar james-server.jar

FROM run-base as jpa-guice

# Ports that are used
#
# 25   SMTP without authentication
# 110  POP3
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 993  IMAP with socketTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 110 143 465 587 993 8000

COPY --from=builder /root/server/container/guice/jpa-guice/target/james-server-jpa-guice.jar /root/james-server.jar
COPY --from=builder /root/server/container/guice/jpa-guice/target/james-server-jpa-guice.lib /root/james-server-jpa-guice.lib

ENV JVM_OPTIONS=""
ENTRYPOINT java -classpath '/root/james-server.jar:/root/james-server-jpa-guice.lib/*' -javaagent:/root/james-server-jpa-guice.lib/openjpa-3.0.0.jar -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ $JVM_OPTIONS org.apache.james.JPAJamesServerMain

FROM run-base as jpa-smtp

# Ports that are used
#
# 25   SMTP without authentication
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 143 465 587 8000

COPY --from=builder /root/server/container/guice/jpa-guice/target/james-server-jpa-smtp-guice.jar /root/james-server.jar
COPY --from=builder /root/server/container/guice/jpa-guice/target/james-server-jpa-smtp-guice.lib /root/james-server-jpa-smtp-guice.lib

ENV JVM_OPTIONS=""
ENTRYPOINT java -classpath '/root/james-server-jpa-smtp-guice.jar:/root/james-server-jpa-smtp-guice.lib/*' -javaagent:/root/james-server-jpa-smtp-guice.lib/openjpa-3.0.0.jar -Dlogback.configurationFile=/root/conf/logback.xml -Dworking.directory=/root/ $JVM_OPTIONS org.apache.james.JPAJamesServerMain

FROM jpa-guice as provisioned-jpa-guice

# Install git
RUN apt-get update
RUN apt-get install -y git

WORKDIR /root

RUN git clone https://github.com/vishnubob/wait-for-it.git wait-for-it
RUN cp /root/wait-for-it/wait-for-it.sh /usr/bin/wait-for-it.sh

COPY dockerfiles/startup.sh /root
COPY dockerfiles/initialdata.sh /root

ENTRYPOINT ["sh", "./startup.sh"]

FROM jre as spring

# Ports that are used
#
# 25  SMTP without authentication
# 110 POP3
# 143 IMAP with startTLS enabled
# 465 SMTP with authentication and socketTLS enabled
# 587 SMTP with authentication and startTLS enabled
# 993 IMAP with socketTLS enabled

EXPOSE 25 110 143 465 587 993

WORKDIR /root

# Get data we need to run James : build results and configuration
COPY --from=builder /root/server/container/target/james-server-app-3.4.0-SNAPSHOT-app.zip /root/james-server-app-3.4.0-SNAPSHOT-app.zip

# Unzip build result
RUN unzip james-server-app-3.4.0-SNAPSHOT-app.zip

# volume to mount configuration to be used by james
VOLUME /root/james-server-app-3.4.0-SNAPSHOT/conf

WORKDIR /root/james-server-app-3.4.0-SNAPSHOT/bin
ENTRYPOINT ["./wrapper-linux-x86-64","/root/wrapper.conf", "wrapper.syslog.ident=james", "wrapper.pidfile=../var/james.pid", "wrapper.daemonize=FALSE"]

FROM builder as integration-tests

COPY integration_tests.sh /root/integration_tests.sh
COPY .testcontainers.properties /root/.testcontainers.properties

FROM builder as run-tests

RUN mvn test
