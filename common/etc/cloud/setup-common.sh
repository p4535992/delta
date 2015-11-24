#!/bin/bash
set -eux

# CAN BE RUN MULTIPLE TIMES

pwd=$(dirname "${0}")
sed -i -e 's/ZONE=".*"/ZONE="Europe\/Tallinn"/' /etc/sysconfig/clock
ln -sf /usr/share/zoneinfo/Europe/Tallinn /etc/localtime
yum -y update
yum -y install mc dstat
chkconfig sendmail off
service sendmail stop

cp "${pwd}/set-nickname.sh" /etc/rc.d/set-nickname.sh
chmod +x /etc/rc.d/set-nickname.sh
/etc/rc.d/set-nickname.sh
if ! grep -q -F set-nickname.sh /etc/rc.d/rc.local
then
	echo /etc/rc.d/set-nickname.sh >> /etc/rc.d/rc.local
fi
cp "${pwd}/prompt.sh" /etc/profile.d/prompt.sh

#yum -y install http://download.newrelic.com/pub/newrelic/el5/x86_64/newrelic-repo-5-3.noarch.rpm
#yum -y install newrelic-sysmond
#nrsysmond-config --set license_key=0000000000000000000000000000000000000000
#service newrelic-sysmond start
