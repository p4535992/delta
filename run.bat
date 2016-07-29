SET JAVA_HOME=C:\Program Files\Java\jdk1.7.0_80
SET PATH=%JAVA_HOME%\bin;%PATH%
SET ANT_HOME=C:\usr\local\apache-ant-1.9.6
SET PATH=%ANT_HOME%\bin;%PATH%
SET ANT_OPTS=-Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=1024m -DrevisionNumber=123456789 -DbuildNumber=1
SET JAVA_OPTS=-Djavax.net.ssl.trustStore=truststore.jks -Djavax.net.ssl.trustStorePassword=delta5 -Djavax.net.ssl.keyStoreType="jks" -Dorg.jboss.security.ignoreHttpsHost="true"
SET MAVEN_HOME=C:\usr\local\apache-maven-3.3.9
SET PATH=%MAVEN_HOME%\bin;%PATH%
ant clean-all war -Dconf.name=sim-example -Dconf.organization.name=ppa -Dappserver=tomcat
