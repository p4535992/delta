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
