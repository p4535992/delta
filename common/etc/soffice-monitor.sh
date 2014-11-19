#!/bin/bash
set -eu

# DESCRIPTION
# When Alfresco launches OpenOffice.org, its full command line is like:
#    /opt/openoffice.org3/program/soffice "-accept=socket,host=localhost,port=8100;urp;StarOffice.ServiceManager" "-env:UserInstallation=file:///vol/dhs01/oouser" -nologo -headless -nofirststartwizard -nocrashrep -norestore
# Sometimes OpenOffice.org process crashes (exits) and sometimes it hangs / gets stuck (and consumes 100% CPU constantly).
# These things happen especially under heavy use of OpenOffice.org (for example a big files import to Delta etc).

# Maybe newer LibreOffice / Apache OpenOffice versions crash less, but newer versions cannot be used currently.
# For now, DHS only works with OpenOffice.org versions up to 3.3.x; DHS does not currently work with Apache OpenOffice / LibreOffice 3.4 or newer versions.

# Tested on Amazon Linux AMI release 2012.03 (64-bit) with OOo_3.3.0_Linux_x86-64_install-rpm-wJRE_en-US.tar.gz
# If OOo was under significant load (for example DHS test data document generator was running with 5 parallel threads), then:
# * OOo was restarted 3 times in an hour by this script (because it hung)
# * OOo was started 3 times in an hour by this script (because it crashed and exited)
<<<<<<< HEAD
#
# @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

# USAGE
#   soffice-monitor.sh PORT WORKDIR >> soffice-monitor.log &
# for example
#   /path/to/soffice-monitor.sh 8100 /home/dhs/data/local/oouser >> /home/dhs/data/local/soffice-monitor.log &

# SETTINGS
# Set these according to your needs

# OOo executable, which is executed by this script. Can be just executable name (soffice) if it is in your PATH; if it is not in your
# PATH, then specify full path to it, for example /opt/openoffice.org3/program/soffice
ooo_exe=/opt/openoffice.org3/program/soffice

# TCP port on which OOo process listens, for example 8100. Taken from first command line argument (if given).
ooo_port=${1:-8100}

# OOo working dir, ususally ${dir.root}/oouser, for example /home/dhs/data/local/oouser. Taken from second command line argument (if given).
ooo_user=${2:-/home/dhs/data/local/oouser}

# OOo process name that is searched from process list when performing health checks.
ooo_procname=soffice.bin

# If OOo process is at least that many consecutive seconds busy, then it counts as "stuck" and OOo process is restarted
ooo_consecutivechecksbusycountsasstuck=15

# ------------------------------------------------------------------------------

# soffice is wrapper process, its wchan is always wait
# soffice.bin is real OOo process, its wchan is poll_s if idle and futex_ if working

ooo_exe=$(which "${ooo_exe}")
if [[ ! -f "${ooo_exe}" ]] || [[ ! -x "${ooo_exe}" ]]
then
    echo "OpenOffice.org executable '${ooo_exe}' does not exist or is not a file or does not have execute permission"
    exit 1
fi
if [[ ! -d "${ooo_user}" ]]
then
    echo "OpenOffice.org working directory '${ooo_user}' does not exist or is not a directory"
    exit 1
fi
echo "$(date --rfc-3339=ns) Monitoring started for OpenOffice.org process '${ooo_procname}' with port ${ooo_port}. If process is running normally, nothing is logged. Settings:"
echo "    ooo_exe=${ooo_exe}"
echo "    ooo_port=${ooo_port}"
echo "    ooo_user=${ooo_user}"
echo "    ooo_procname=${ooo_procname}"
echo "    ooo_consecutivechecksbusycountsasstuck=${ooo_consecutivechecksbusycountsasstuck}"
status=
statusbusy=
for i in $(seq 1 ${ooo_consecutivechecksbusycountsasstuck})
do
    statusbusy=B${statusbusy}
done
while true
do
	info=$(ps -C "${ooo_procname}" -o pid,wchan,args|grep "${ooo_port}" || true)
	if [[ "${info}" != "" ]]
	then
		#echo "$(date --rfc-3339=ns) Process ${ooo_procname} is running"
		if echo "${info}"|grep -F -q "futex_"
		then
			status=B${status}
		else
			status=I${status}
		fi
		# Take substring 0..x
		status=${status:0:${ooo_consecutivechecksbusycountsasstuck}}
		#echo "$(date --rfc-3339=ns) status=${status}"
		if [[ "${status}" = "${statusbusy}" ]]
		then
			pid=$(echo "${info}"|sed -e 's/^ *//' -e 's/ .*//')
			echo "$(date --rfc-3339=ns) Process ${ooo_procname} has been BUSY for ${ooo_consecutivechecksbusycountsasstuck} seconds, sending TERM signal (normal exit) to PID=${pid} and waiting 5 seconds"
			kill ${pid}
			sleep 5
			if kill -9 ${pid} 2>/dev/null
			then
			    echo "$(date --rfc-3339=ns) Process did not exit after 5 seconds, so forcefully killed process, waiting 240 seconds"
			    sleep 239
			fi
			status=
		fi
	else
		echo "$(date --rfc-3339=ns) Process ${ooo_procname} not running, launching with command line: \"${ooo_exe}\" \"-accept=socket,host=localhost,port=${ooo_port};urp;StarOffice.ServiceManager\" \"-env:UserInstallation=file://${ooo_user}\" -nologo -headless -nofirststartwizard -nocrashrep -norestore"
		"${ooo_exe}" "-accept=socket,host=localhost,port=${ooo_port};urp;StarOffice.ServiceManager" "-env:UserInstallation=file://${ooo_user}" -nologo -headless -nofirststartwizard -nocrashrep -norestore &
		# Would be nice to prevent child process from dying if monitor script exits... but nohup doesn't seem to work :(
		echo "$(date --rfc-3339=ns) Process launched with wrapper PID=$!, waiting 20 seconds"
		sleep 19
	fi
	sleep 1
done
