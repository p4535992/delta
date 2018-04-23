SET JAVA_HOME=\usr\local\Java\jdk1.7.0_80
SET PATH=%JAVA_HOME%\bin;%PATH%

SET ANT_HOME=\usr\local\apache-ant-1.9.6
SET PATH=%ANT_HOME%\bin;%PATH%

SET ANT_OPTS=-Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=1024m -DrevisionNumber=123456789 -DbuildNumber=1

SET JAVA_OPTS=-Djavax.net.ssl.trustStore=cacerts -Djavax.net.ssl.keyStore=cacerts

ant clean-all war -Dconf.name=sim-example -Dconf.organization.name=ppa -Dappserver=tomcat
