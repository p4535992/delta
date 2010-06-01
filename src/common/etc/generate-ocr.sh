#!/bin/bash
set -eu
if [[ $# -ne 1 ]]
then
	echo "Regenerate Ocr webservice client code from WSDL"
	echo
	echo "Usage: $(basename $0) <WSDL_URI>"
	exit 1
fi

src=$(dirname "$0")/../source/java
pkg=${src}/ee/webmedia/ocr

rm -f ${pkg}/*.{java,wsdl,xsd}

wget -O "${pkg}/OcrService.wsdl" "$1"

xsd=$(grep schemaLocation "${pkg}/OcrService.wsdl"|sed -e 's/.*schemaLocation="//' -e 's/".*//')
wget -O "${pkg}/OcrService.xsd" "${xsd}"

sed -i -e 's/ schemaLocation=".*"/ schemaLocation="OcrService.xsd"/' -e 's/ location=".*"/ location="ENDPOINT_ADDRESS"/' "${pkg}/OcrService.wsdl"

${JAVA_HOME}/bin/wsimport -s "${src}" -Xnocompile "${pkg}/OcrService.wsdl"

sed -i -e 's/file:.*OcrService\.wsdl/OcrService.wsdl/' "${pkg}/OcrService.java"

exit 0
