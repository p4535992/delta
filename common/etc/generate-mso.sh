#!/bin/bash
set -eu
if [[ $# -ne 1 ]]
then
	echo "Regenerate Mso webservice client code from WSDL"
	echo
	echo "Usage: $(basename $0) <WSDL_URI>"
	exit 1
fi

src=$(dirname "$0")/../source/java
pkg=${src}/ee/webmedia/mso

rm -f ${pkg}/*.{java,wsdl,xsd}

wget -O "${pkg}/MsoService.wsdl" "$1"

xsd=$(grep schemaLocation "${pkg}/MsoService.wsdl"|sed -e 's/.*schemaLocation="//' -e 's/".*//')
wget -O "${pkg}/MsoService.xsd" "${xsd}"

sed -i -e 's/ schemaLocation=".*"/ schemaLocation="MsoService.xsd"/' -e 's/ location=".*"/ location="ENDPOINT_ADDRESS"/' "${pkg}/MsoService.wsdl"

${JAVA_HOME}/bin/wsimport -s "${src}" -Xnocompile "${pkg}/MsoService.wsdl"

sed -i -e 's/file:.*MsoService\.wsdl/MsoService.wsdl/' "${pkg}/MsoService.java"

exit 0
