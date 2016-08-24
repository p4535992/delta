REM SET JAVA_HOME=C:\Program Files\Java\jdk1.6.0_45
SET JAVA_HOME=C:\Program Files\Java\jdk1.7.0_71
REM SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_72
SET PATH=%JAVA_HOME%\bin;%PATH%

SET ANT_HOME=C:\usr\local\apache-ant-1.9.6
SET PATH=%ANT_HOME%\bin;%PATH%

SET ANT_OPTS=-Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=1024m -DrevisionNumber=123456789 -DbuildNumber=1


REM export JAVA_HOME=/usr/lib/jvm/jdk1.7.0_71
REM export ANT_HOME=/home/aare/soft/apache-ant-1.9.4

REM export PATH=$PATH:$JAVA_HOME/bin
REM export PATH=$PATH:$ANT_HOME/bin

REM export ANT_OPTS="-Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=1024m -DrevisionNumber=123456789 -DbuildNumber=1"

SET JAVA_OPTS=-Djavax.net.ssl.trustStore=E:\WebROOT\TARKVARA.GIT\DELTA\bitbucket-delta\cacerts -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.keyStoreType="jks" -Dorg.jboss.security.ignoreHttpsHost="true" -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStore=E:\WebROOT\TARKVARA.GIT\DELTA\bitbucket-delta\cacerts -Dorg.jboss.security.ignoreHttpsHost="true"
REM ant clean-all war -Dconf.name=dev-example -Dconf.organization.name=mv -Dappserver=tomcat
REM ant clean-all war -Dconf.name=jum-example -Dconf.organization.name=jum -Dappserver=tomcat
ant clean-all war -Dconf.name=sim-example -Dconf.organization.name=ppa -Dappserver=tomcat -debug


exit;

--ADMIN CMD
SET JAVA_HOME=C:\Program Files\Java\jdk1.7.0_80
SET PATH=%JAVA_HOME%\bin;%PATH%
keytool -keystore "%JAVA_HOME%\jre\lib\security\cacerts" -importcert -alias digidocservice -file digidocservice.sk.ee.crt
keytool -keystore "C:\Program Files\Java\jdk1.7.0_71\jre\lib\security\cacerts" -importcert -alias digidocservice -file digidocservice.sk.ee.2.crt
keytool -keystore "C:\Program Files\Java\jdk1.8.0_31\jre\lib\security\cacerts" -importcert -alias digidocservice -file digidocservice.sk.ee.2.crt
keytool -keystore "C:\Program Files\Java\jre1.8.0_31\lib\security\cacerts" -importcert -alias digidocservice -file digidocservice.sk.ee.2.crt
keytool -keystore "C:\Program Files\Java\jre7\lib\security\cacerts" -importcert -alias digidocservice -file digidocservice.sk.ee.2.crt

password is: changeit