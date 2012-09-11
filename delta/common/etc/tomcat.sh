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

if [ "$1" = "start" ]; then

  # JVM settings
  export JAVA_OPTS="${JAVA_OPTS} -server -XX:+HeapDumpOnOutOfMemoryError -XX:OnError=./jvm-error.sh -XX:OnOutOfMemoryError=./jvm-error.sh"
  export JAVA_OPTS="${JAVA_OPTS} -verbose:gc -Xloggc:logs/gc.log.$(date +%Y-%m-%d-%H-%M-%S) -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+PrintHeapAtGC -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps"

  # If HTTPS certificate of CAS server is self-signed, then you need to add it to truststore, otherwise HTTPS connection fails
  # You can generate truststore with the following command:
  # keytool -v -importcert -keystore truststore.jks -file myserver.crt
  #export JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=truststore.jks"

  # JMX access for remote administration
  # If you enable this, then also copy catalina-jmx-remote.jar to tomcat/lib folder and add the following line to tomcat/conf/server.xml
  # <Listener className="org.apache.catalina.mbeans.JmxRemoteLifecycleListener" rmiRegistryPortPlatform="8686" rmiServerPortPlatform="8687" />
  #export JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=$(hostname -f)"

  # Enable in development only !!!
  #export JAVA_OPTS="${JAVA_OPTS} -noverify -javaagent:/home/programs/jrebel/jrebel.jar -Drebel.spring_plugin=false -Drebel.hibernate_plugin=false -Drebel.log4j-plugin=true -Dproject.root=/home/user/workspace/delta"
  #export JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009"

  exec bin/startup.sh

elif [ "$1" = "stop" ]; then
  exec bin/shutdown.sh 20 -force
fi
