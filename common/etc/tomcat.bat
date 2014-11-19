<<<<<<< HEAD
@echo off
set "JAVA_OPTS=%JAVA_OPTS% -verbose:gc -XX:+HeapDumpOnOutOfMemoryError -XX:MaxPermSize=192m -Xmx768m"

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop
goto end

:doStart
rem Enable in development only !!!
set "JAVA_OPTS=%JAVA_OPTS% -noverify -javaagent:c:/programs/jrebel/jrebel.jar -Drebel.spring_plugin=false -Drebel.hibernate_plugin=false -Drebel.log4j-plugin=true -Dproject.root=c:/workspace/mvdhs"
set "JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009"
bin\startup.bat
goto end

:doStop
bin\shutdown.bat
goto end

:end
=======
@echo off
set "CATALINA_PID=c:/programs/tomcat/tomcat.pid"
set "JAVA_OPTS=%JAVA_OPTS% -XX:ReservedCodeCacheSize=72m -XX:MaxPermSize=192m -Xmx768m"
set "JAVA_OPTS=%JAVA_OPTS% -server -XX:+HeapDumpOnOutOfMemoryError"

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop
goto end

:doStart
rem set "JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.trustStore=truststore.jks"
rem Enable in development only !!!
rem set "JAVA_OPTS=%JAVA_OPTS% -noverify -javaagent:c:/programs/jrebel/jrebel.jar -Drebel.spring_plugin=false -Drebel.hibernate_plugin=false -Drebel.log4j-plugin=true -Dproject.root=c:/workspace/delta"
rem set "JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009"
bin\startup.bat
goto end

:doStop
bin\shutdown.bat 20 -force
goto end

:end
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
