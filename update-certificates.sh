#!/bin/bash
# fail on error
set -e

: "${CA_PASSWORD?Need to set CA_PASSWORD environment variable}"
: "${CLIENT_PASSWORD?Need to set CLIENT_PASSWORD environment variable}"
: "${MODE?Need to set MODE environment variable}"

cd target

export SITE=beanstalk-template-${MODE}.ap-southeast-2.elasticbeanstalk.com
export CA_SUBJECT="/C=AU/ST=Your state/L=Your location/O=Your organisation/OU=Your org unit/emailAddress=youremail@gmail.com/CN=your.org.website.com"
export SERVER_SUBJECT="/C=AU/ST=Your state/L=Your location/O=Your organisation/OU=Your org unit/emailAddress=youremail@gmail.com/CN=$SITE"
export CLIENT_SUBJECT="/C=AU/ST=Client state/L=Client location/O=Client org/OU=Client org unit/CN=client.website.com"
export EXPIRY_DAYS=3650

# create ca certificate root 
openssl genrsa -des3 -out ca.key -passout "pass:$CA_PASSWORD" 4096
openssl req -new -x509 -days $EXPIRY_DAYS -key ca.key -out ca.crt -passin "pass:$CA_PASSWORD" -subj "$CA_SUBJECT"  
   
# create server certificates (public and private)
openssl genrsa -out server-private-key.pem 2048
openssl req -sha256 -new -key server-private-key.pem -out server.csr -subj "$SERVER_SUBJECT"
openssl x509 -req -days $EXPIRY_DAYS -in server.csr -signkey server-private-key.pem -out server.pem -passin "pass:$CA_PASSWORD"
rm server.csr 
   
# create client certificate with its private key
openssl genrsa -des3 -out client.key -passout "pass:$CLIENT_PASSWORD" 4096
openssl req -new -key client.key -out client.csr -passin "pass:$CLIENT_PASSWORD" -subj "$CLIENT_SUBJECT"
   
# include password in client certificate private key
openssl rsa -in client.key -out client.key -passin "pass:$CLIENT_PASSWORD"

# sign the client certificate with our ca
openssl x509 -req -days $EXPIRY_DAYS -in client.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out client.crt -passin "pass:$CA_PASSWORD"
rm client.csr
    
# convert client certificate to pkcs12 so can be installed in a browser (you'll need the password handy)
openssl pkcs12 -export -clcerts -in client.crt -inkey client.key -out client.p12 -passin "pass:$CLIENT_PASSWORD" -passout "pass:$CLIENT_PASSWORD"

echo created the following files:
ls ca* client* server*

SERVER_CERTS=../src/main/webapp/.ebextensions/certificates/$MODE/
mkdir -p $SERVER_CERTS
cp ca.crt $SERVER_CERTS
cp server.pem $SERVER_CERTS
CLIENT_CERTS=../src/main/client-certificates/$MODE/
mkdir -p $CLIENT_CERTS
cp client.crt $CLIENT_CERTS

rm ca.crt
rm server.pem
rm client.crt

echo --- ca.key ---
cat ca.key

echo --- server-private-key.pem ---
cat server-private-key.pem

echo --- client.key ---
cat client.key

cp ca.* server-private-key.* client.* /tmp

echo "$CLIENT_PASSWORD" >client-password.txt
echo "$CA_PASSWORD" >ca-password.txt

tar -cvzf keys.tar.gz ca-password.txt client.key client.p12 client-password.txt server-private-key.pem ca.key

echo ---------------------------------------
echo ---------- IMPORTANT! -----------------
echo ---------------------------------------

echo "You should now save the (text) contents of these files (in target directory) in a secure place:"
echo
echo "    ca.key"
echo "    server-private-key.pem"
echo "    client.key"
echo "    client.password=$CLIENT_PASSWORD"
echo "    ca.password=$CA_PASSWORD"
echo 
echo "The above files have also been tar gzipped into the file target/keys.tar.gz"
echo
echo "Note also:"
echo "    the public certificate for the client has been copied into the src/main/client-certificates/$MODE directory"
echo "    the public certificates for the server and ca have been copied into the src/main/webapp/.ebextensions/certificates/$MODE directory"
echo 
echo "The public certificates are safe for commiting to source control (the other files are not)."
echo 
echo "For your convenience you should also save a copy of client.p12 somewhere so you can import it to a browser if you wish. This file can be generated from the client.key and client.password at any time. When you submit the file to a browser as a personal certificate it will ask you for the client password used in this script ($CLIENT_PASSWORD)"
echo 
echo "Note that if you've lost the contents of target the files have also been copied to /tmp"
 
echo --------------------------------------

cd -
