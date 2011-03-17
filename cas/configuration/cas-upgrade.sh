#!/bin/sh
for i in $(find src/main/webapp -type f); do [[ -f ../cas-server-3.4.5/cas-server-webapp/$i ]] && diff -urN ../cas-server-3.4.3.1/cas-server-webapp/$i ../cas-server-3.4.5/cas-server-webapp/$i >cas-upgrade-$(basename $i).patch; done
