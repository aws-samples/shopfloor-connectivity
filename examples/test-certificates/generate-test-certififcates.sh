#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
#

rm *.pem
rm *.srl
rm *.cnf

C="US"
ST="WA"
L="SEATTLE"
O="AWS"
OU="AIP"

for i in $(ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
do
	IP_LIST+="IP:$i,"
done
IP_LIST+="IP:127.0.0.1,"
IP_LIST+="IP:0.0.0.0"

HOST=$(hostname -s)
DNS_NAMES="DNS:$HOST,DNS:localhost"
CN="/C=$C/ST=$ST/L=$L/O=$O/OU=$OU/CN=$HOST"

# CA
# Private key and self-signed certificate
openssl req -x509 -newkey rsa:4096 -days 365 -nodes -keyout ca-key.pem -out ca-cert.pem -subj "$CN"-CA""

echo "CA's self-signed certificate"
openssl x509 -in ca-cert.pem -noout -text



# SERVER
# Private key and certificate signing request
openssl req -newkey rsa:4096 -nodes -keyout server-key.pem -out server-req.pem -subj "$CN"-SERVER""

echo "subjectAltName=$DNS_NAMES,$IP_LIST" > server-ext.cnf

# Create certificate
openssl x509 -req -in server-req.pem -days 365 -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -extfile server-ext.cnf

echo "Server's signed certificate"
openssl x509 -in server-cert.pem -noout -text



# CLIENT
# Private key and certificate signing request
openssl req -newkey rsa:4096 -nodes -keyout client-key.pem -out client-req.pem -subj "$CN"-CLIENT""

echo "subjectAltName=$DNS_NAMES,$IP_LIST" > client-ext.cnf

# Create certificate
openssl x509 -req -in client-req.pem -days 365 -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out client-cert.pem -extfile client-ext.cnf

echo "Client's signed certificate"
openssl x509 -in client-cert.pem -noout -text