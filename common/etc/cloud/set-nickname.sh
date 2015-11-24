#!/bin/bash
set -eu
line=$(curl -s http://169.254.169.254/latest/user-data|grep -E '^nickname='|tail -n 1)
if [[ -n "${line}" ]]
then
	nickname=$(echo "${line}"|sed -e 's/^nickname=//' -e "s/'//g")
else
	nickname=$(curl -s http://169.254.169.254/latest/meta-data/public-hostname|sed -e 's/\..*//')
fi
echo "export NICKNAME='${nickname}'" > /etc/profile.d/nickname.sh
