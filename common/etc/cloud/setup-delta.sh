#!/bin/bash
set -eux

# CAN BE RUN MULTIPLE TIMES

pwd=$(dirname "${0}")
chmod +x "${pwd}/setup-common.sh"
"${pwd}/setup-common.sh"

yum -y install java-1.7.0-openjdk-devel tomcat7 ImageMagick nginx
# TODO openoffice 3.3.0

perl -0777 -i -pe 's/\n    server {.*\n    }//igs' /etc/nginx/nginx.conf
cp "${pwd}/dhs.conf" /etc/nginx/conf.d
cp "${pwd}/generate-ssl-cert.sh" /etc/nginx/generate-ssl-cert.sh
chmod +x /etc/nginx/generate-ssl-cert.sh
/etc/nginx/generate-ssl-cert.sh
# TODO execute ^ on every boot
chkconfig nginx on
service nginx start

mkdir -p /media/ephemeral0/dhs
chown tomcat:tomcat /media/ephemeral0/dhs
sed -i -e 's|redirectPort="8443" />|redirectPort="8443" scheme="https" secure="true" />|' /etc/tomcat7/server.xml
sed -i -e 's/.*LANG=.*/LANG="et_EE"/' /etc/tomcat7/tomcat7.conf
cp "${pwd}/jvm-error.sh" /etc/tomcat7/jvm-error.sh
chmod +x /etc/tomcat7/jvm-error.sh
if ! grep -q -F MaxPermSize /etc/tomcat7/tomcat7.conf
then
	cat <<'EOF' >> /etc/tomcat7/tomcat7.conf
JAVA_OPTS="-XX:ReservedCodeCacheSize=72m -XX:MaxPermSize=192m -Xmx768m"
JAVA_OPTS="${JAVA_OPTS} -server -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/media/ephemeral0/dhs -XX:OnError=/etc/tomcat7/jvm-error.sh -XX:OnOutOfMemoryError=/etc/tomcat7/jvm-error.sh"
JAVA_OPTS="${JAVA_OPTS} -verbose:gc -Xloggc:${CATALINA_BASE}/logs/gc.log.$(date +%Y-%m-%d-%H-%M-%S) -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+PrintHeapAtGC -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps"

# YourKit profiler
#JAVA_OPTS="${JAVA_OPTS} -agentpath:/data/dhs/yourkit/bin/linux-x86-64/libyjpagent.so"

# NewRelic agent
#JAVA_OPTS="${JAVA_OPTS} -javaagent:/data/dhs/newrelic/newrelic.jar"

# If HTTPS certificate of CAS server is self-signed, then you need to add it to truststore, otherwise HTTPS connection fails
# You can generate truststore with the following command:
# keytool -v -importcert -keystore truststore.jks -file myserver.crt
JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=/etc/tomcat7/truststore.jks"
#keytool -v -importcert -noprompt -trustcacerts -keystore /data/dhs/newrelic/nrcerts -storepass changeit -file /etc/pki/tls/certs/public-hostname.crt
#JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=/data/dhs/newrelic/nrcerts"

# JMX access for remote administration
# If you enable this, then also copy catalina-jmx-remote.jar to tomcat/lib folder and add the following line to tomcat/conf/server.xml
# <Listener className="org.apache.catalina.mbeans.JmxRemoteLifecycleListener" rmiRegistryPortPlatform="8686" rmiServerPortPlatform="8687" />
#JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=$(hostname -f)"

# Debugger
JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
EOF
    # TODO tomcat7 -Xmx
    # TODO tomcat7 JMX
    # TODO tomcat7 YourKit
    # TODO tomcat7 NewRelic
fi
mkdir -p /usr/share/tomcat7/webapps/ROOT
echo '<?xml version="1.0" encoding="UTF-8"?>
<%@ page session="false" %>
<% response.sendRedirect("/dhs/"); %>' > /usr/share/tomcat7/webapps/ROOT/index.jsp

if [[ ! -e /data ]]
then
	mkdir /data
	echo "/dev/sdf	/data	auto	defaults,nofail	0	2" >> /etc/fstab
	#echo "/dev/sdg	/data/dhs/delta/local/lucene-indexes	auto	defaults,nofail	0	2" >> /etc/fstab
	mount /data
fi
if ! [[ -e /data/lost+found ]]
then
	mke2fs /dev/sdf
	mount /data && [[ -e /data/lost+found ]]
fi
if [[ ! -e /data/dhs ]]
then
	mkdir -p /data/dhs/logs
	chown -R tomcat:tomcat /data/dhs
fi
ln -sfT /data/dhs/logs /usr/share/tomcat7/logs

# TODO cas.war
# TODO dhs.war
# TODO alfresco-global.properties and merge props on every boot and replace cas.casServerUrl and server.url on every boot
chkconfig tomcat7 on
service tomcat7 start

set +x
echo -e "Execute \e[1;33msource /etc/profile\e[0m to refresh prompt of existing shell"
echo -e "Execute \e[1;33mtail -F /usr/share/tomcat7/alfresco.log /usr/share/tomcat7/logs/catalina.out\e[0m to follow DHS logs"
