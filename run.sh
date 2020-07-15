#!/bin/bash

HOST=$1; shift
TRANSPORT_REQUEST_ID=$1; shift

CONTENT=content.txt
COOKIES=mycookies


rm -rf ${COOKIES}
rm -rf ${CONTENT}

cat << EOF > ${CONTENT}
<?xml version="1.0" encoding="utf-8"?>
<entry xml:base="${HOST}/sap/opu/odata/UI5/ABAP_REPOSITORY_SRV/"
       xmlns="http://www.w3.org/2005/Atom" xmlns:m="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata"
       xmlns:d="http://schemas.microsoft.com/ado/2007/08/dataservices">
    <id>${HOST}/sap/opu/odata/UI5/ABAP_REPOSITORY_SRV/Repositories('%2FUI5%2FMHOLL')</id>
    <title type="text">Repositories('%2FUI5%2FMHOLL')</title>
    <updated>2020-07-14T11:40:33Z</updated>
    <category term="/UI5/ABAP_REPOSITORY_SRV.Repository" scheme="http://schemas.microsoft.com/ado/2007/08/dataservices/scheme"/>
    <link href="Repositories('%2FUI5%2FMHOLL')" rel="edit" title="Repository"/>
    <content type="application/xml">
      <m:properties>
        <d:Name>/UI5/MHOLL</d:Name>
        <d:Package>/UI5/UI5_INFRA_APP</d:Package>
        <d:Description>Hello World</d:Description>
        <d:ZipArchive>UEsDBAoAAAAAAI1t7lAHoerdAgAAAAIAAAAIABwAdGVzdC50eHRVVAkAA5qaDV+bmg1fdXgLAAEEmudsSAQUAAAAYQpQSwECHgMKAAAAAACNbe5QB6Hq3QIAAAACAAAACAAYAAAAAAABAAAApIEAAAAAdGVzdC50eHRVVAUAA5qaDV91eAsAAQSa52xIBBQAAABQSwUGAAAAAAEAAQBOAAAARAAAAAAA</d:ZipArchive>
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
