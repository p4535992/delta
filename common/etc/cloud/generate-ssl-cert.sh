#!/bin/bash
set -eu

# http://wiki.centos.org/HowTos/Https

hostname=$(curl -s http://169.254.169.254/latest/meta-data/public-hostname)
echo "Generating self-signed SSL certificate for hostname ${hostname}"
tmpdir=$(mktemp -d)
cat <<EOF > "${tmpdir}/openssl.cnf"
[ req ]
distinguished_name = req_distinguished_name
prompt = no

[ req_distinguished_name ]
CN = ${hostname}
EOF
set -x
openssl genrsa -out "${tmpdir}/ca.key" 2048
openssl req -new -key "${tmpdir}/ca.key" -out "${tmpdir}/ca.csr" -config "${tmpdir}/openssl.cnf"
openssl x509 -req -days 3650 -in "${tmpdir}/ca.csr" -signkey "${tmpdir}/ca.key" -out "${tmpdir}/ca.crt"
cp -a "${tmpdir}/ca.crt" "/etc/pki/tls/certs/public-hostname.crt"
cp -a "${tmpdir}/ca.key" "/etc/pki/tls/private/public-hostname.key"
#cp -a "${tmpdir}/ca.csr" "/etc/pki/tls/private/${hostname}.csr"
chmod 600 "/etc/pki/tls/private/public-hostname.key"
rm -rf "${tmpdir}"
set +x
echo "Done! Files are located:
  /etc/pki/tls/certs/public-hostname.crt
  /etc/pki/tls/private/public-hostname.key"

if [[ -e /etc/tomcat7 ]]
then
	rm -f /etc/tomcat7/truststore.jks
	keytool -v -importcert -noprompt -keystore /etc/tomcat7/truststore.jks -storepass changeit -file /etc/pki/tls/certs/public-hostname.crt
fi
