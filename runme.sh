#!/bin/bash 
JARFILE=target/scala-2.12/akka-http-prometheus-javaagent-assembly-0.1.jar
AGENT=jmx_prometheus_javaagent-0.12.0.jar

if [ ! -e ${AGENT} ];
then
  wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.12.0/jmx_prometheus_javaagent-0.12.0.jar
fi

if [ ! -e ${JARFILE} ];
then
  sbt assembly
fi

java -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -Djava.awt.headless=true \
     -javaagent:jmx_prometheus_javaagent-0.12.0.jar=127.0.0.1:8081:config.yaml -jar $JARFILE
