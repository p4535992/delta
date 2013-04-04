#!/bin/sh
# Start or stop Tomcat server

# Set the following to where Tomcat is installed
cd "$(dirname $0)"

# Set locale. Do not change, we need Estonian locale.
export LANG="et_EE.UTF-8"

export CATALINA_OUT="$(pwd)/logs/catalina.out.$(date +%Y-%m-%d)"
export CATALINA_PID="$(pwd)/tomcat.pid"

# JVM settings
export JAVA_OPTS="${JAVA_OPTS} -XX:MaxPermSize=192m -Xmx1024m"
export JAVA_OPTS="${JAVA_OPTS} -server -verbose:gc -XX:+HeapDumpOnOutOfMemoryError -XX:OnError=./jvm-error.sh -XX:OnOutOfMemoryError=./jvm-error.sh"

# If HTTPS certificate of CAS server is self-signed, then you need to add it to truststore, otherwise HTTPS connection fails
# You can generate truststore with the following command:
# keytool -v -importcert -keystore truststore.jks -file myserver.crt
export JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=truststore.jks"

if [ "$1" = "start" ]; then
  #export JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=mvdhs.webmedia.int"

  # Enable in development only !!!
  #export JAVA_OPTS="${JAVA_OPTS} -noverify -javaagent:/home/programs/jrebel/jrebel.jar -Drebel.spring_plugin=false -Drebel.hibernate_plugin=false -Drebel.log4j-plugin=true -Dproject.root=/home/user/workspace/delta"
  #export JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009"

  exec bin/startup.sh

elif [ "$1" = "stop" ]; then
  exec bin/shutdown.sh 20 -force
fi
