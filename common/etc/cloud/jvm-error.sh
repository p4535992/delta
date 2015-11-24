#!/bin/sh
# This script is tested on Linux+Tomcat
# But there is one remaining issue - sometimes when killing JVM, network ports or connections stay in
# use for some time and starting application again fails when ports are in use. This could hopefully be
# resolved by increasing sleep time before starting Tomcat to enough minutes that network ports and
# connections timeout in the OS.

# The only information we have is $PPID - ID of this script's parent process, which is the JVM process

# There is a weird behaviour, if tomcat is started from this shell script, then that JVM executes
# sh -c ./jvm-error.sh?./jvm-error.sh?./jvm-error.sh?...
# Then this script is called multiple times, but only the last time PPID is correct
# We can ignore the first false invocations like this:
if ! grep -q XX:OnError= /proc/$PPID/cmdline
then
	exit 0
fi

# Thread dump info to catalina.out
kill -3 $PPID

# Log the time and PPID to file
echo "$(date --rfc-3339=seconds) PID=$PPID" >> /usr/share/tomcat7/logs/jvm-error.log

# Send e-mail - test it before enabling
#echo "Killing JVM process PID=$PPID and starting Tomcat again..."|mailx -s "JVM error on $(hostname -f)" example@example.com

# Send TERM signal to JVM process
kill $PPID

# Wait some, maybe JVM process can exit on its own
sleep 15

# Kill JVM process by force
kill -9 $PPID

# Wait some more and start Tomcat - test it before enabling
#sleep 240
#./tomcat.sh start

# Exit with success
exit 0
