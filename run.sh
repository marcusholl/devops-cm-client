#!/bin/bash

HOST=$1; shift
TRANSPORT_REQUEST_ID=$1; shift
APP=$1; shift

[ -z "${HOST}" ] && { echo "No host provided";  exit 1; }
[ -z "${TRANSPORT_REQUEST_ID}" ] && { echo "No transport request id  provided";  exit 1; }
[ -z "${APP}" ] && { echo "No application file provided."; exit 1; }
[ ! -e "${APP}" ] && { echo "Application file ${APP} does not exist"; exit 1; }

CONTENT=content.txt
COOKIES=mycookies

rm -rf ${COOKIES}
rm -rf ${CONTENT}

APP_ENC=$(cat "${APP}" |base64)

cat << EOF > ${CONTENT}
<?xml version="1.0" encoding="utf-8"?>
<entry xml:base="${HOST}/sap/opu/odata/UI5/ABAP_REPOSITORY_SRV/"
       xmlns="http://www.w3.org/2005/Atom" xmlns:m="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata"
       xmlns:d="http://schemas.microsoft.com/ado/2007/08/dataservices">
    <id>${HOST}/sap/opu/odata/UI5/ABAP_REPOSITORY_SRV/Repositories('MHOLL')</id>
    <title type="text">MHOLL</title>
    <updated>2020-07-14T11:40:33Z</updated>
    <category term="/UI5/ABAP_REPOSITORY_SRV.Repository" scheme="http://schemas.microsoft.com/ado/2007/08/dataservices/scheme"/>
    <link href="Repositories('MHOLL')" rel="edit" title="Repository"/>
    <content type="application/xml">
      <m:properties>
        <d:Name>MHOLL</d:Name>
        <d:Package>MHOLL</d:Package>
        <d:Description>Hello World</d:Description>
        <d:ZipArchive>${APP_ENC}</d:ZipArchive>
        <d:Info/>
      </m:properties>
    </content>
</entry>
EOF

CSRF_TOKEN=$(
curl --netrc \
     --request GET \
     -b ${COOKIES} \
     -c ${COOKIES} \
     --header "X-CSRF-Token: Fetch" \
     -vvv \
     "${HOST}/sap/opu/odata/UI5/ABAP_REPOSITORY_SRV/" 2>&1 \
     |grep x-csrf-token \
     |sed "s/.*: //")

echo "[INFO] CSRF-Token: ${CSRF_TOKEN}"

# I switched off the virus check on the ABAP stack via /N/IWFND/VIRUS_SCAN

curl --netrc \
     --request POST\
     -b ${COOKIES} \
     -c ${COOKIES} \
     --data @${CONTENT} \
     --header "x-csrf-token: ${CSRF_TOKEN}" \
     --header "Content-Type: application/atom+xml;type=entry; charset=utf-8" \
     -vvv \
     "${HOST}/sap/opu/odata/UI5/ABAP_REPOSITORY_SRV/Repositories?TransportRequest=${TRANSPORT_REQUEST_ID}&CodePage=UTF8"
