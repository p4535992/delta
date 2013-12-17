#!/bin/bash
out=
while read line
do
	if echo "${line}"|grep -q -E '^\(.*\)$'
	then
		if read line
		then
#			out=$(echo "${line}"|sed -e 's/^\([^ ]* [^ ]*\).*$/\1/'|tr " " _|tr A-Z a-z).csv
			out=$(echo "${line}"|sed -e 's/,.*//' -e 's/ where.*//' -e 's/ (.*//'|tr " " _|tr A-Z a-z).csv
			echo "out=${out}"
		else
			exit 0
		fi
	else
		if [[ "${out}" != "" ]]
		then
			echo "${line}" >>"${out}"
		fi
	fi
done
