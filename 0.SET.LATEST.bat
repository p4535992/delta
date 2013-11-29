REM @echo off
REM setlocal

:SetJava
REM # JAVA 1.6
SET JAVA_HOME=C:\Program Files\Java\jdk1.6.0_45
REM SET JAVA_HOME=C:\Program Files\Java\jdk1.7.0_25
set PATH=%JAVA_HOME%\bin;%PATH%

:SetMaven2
REM # MAVEN 2
set M2_HOME=D:\My Programs\apache-maven-2.2.1
set PATH=%M2_HOME%\bin;%PATH%

REM set MAVEN_OPTS=-Xmx1024m

:SetGrails
SET GRAILS_HOME=C:\springsource\grails-2.3.4
set PATH=%GRAILS_HOME%\bin;%PATH%

:SetAnt
SET ANT_HOME=C:\My Programs\apache-ant-1.8.2
REM SET ANT_HOME=C:\My Programs\apache-ant-1.9.3
set PATH=%ANT_HOME%\bin;%PATH%

REM set ANT_OPTS=
REM set ANT_OPTS=-Dhttp.proxyHost=proxy.smit -Dhttp.proxyPort=8080 -DrevisionNumber=123456789 -DbuildNumber=1

:antOpts
SET ANT_OPTS=-Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=1024m -DrevisionNumber=123456789 -DbuildNumber=1
REM ANT_OPTS="-Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=1024m -Dhttp.proxyHost=proxy.smit -Dhttp.proxyPort=8080"
REM -Dhttp.proxyHost=proxy.smit -Dhttp.proxyPort=8080

:mavenOpts
SET MAVEN_OPTS=-Xmx1024m -DrevisionNumber=123456789 -DbuildNumber=1
REM -Dhttp.proxyHost=proxy.smit -Dhttp.proxyPort=8080
SET MAVEN_BATCH_ECHO=on
SET MAVEN_BATCH_PAUSE=off

REM mvn clean install -DrevisionNumber=103507 -DbuildNumber=20
REM ant clean build war -DrevisionNumber=103507 -DbuildNumber=20

:edhs
ECHO ant clean-all war -Dconf.name=smit-test -Dconf.organization.name=default -Dappserver=tomcat
ECHO ant clean-all war -Dconf.name=smit-test -Dconf.organization.name=ppa -Dappserver=tomcat
ECHO ant clean-all war -Dconf.name=jum-example -Dconf.organization.name=jum -Dappserver=tomcat
ECHO ant clean-all war -Dconf.name=sim-example -Dconf.organization.name=ppa -Dappserver=tomcat > delta.build.log

