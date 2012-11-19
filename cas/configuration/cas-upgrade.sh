#!/bin/sh
for i in $(find src/main/webapp -type f)
do
	if [[ -f ../../workspace/cas-server-3.4.5/cas-server-webapp/$i ]]
	then
		echo "diff $i"
		diff -urN ../../workspace/cas-server-3.4.5/cas-server-webapp/$i ../../workspace/cas-server-3.4.12/cas-server-webapp/$i >cas-upgrade-$(basename $i).patch
		patch -p5 <cas-upgrade-$(basename $i).patch
	fi
done
