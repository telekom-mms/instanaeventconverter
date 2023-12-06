#!/bin/bash
instanaurl="load-tsystems.instana.io"
apitoken="e-5XkRzLR7CABC8XCypGUg"


result=$(curl --location --request GET "https://$instanaurl/api/application-monitoring/applications?nameFilter=&windowSize=&to=&page=&pageSize=1000&applicationBoundaryScope=" --header "Authorization: apiToken $apitoken")

OIFS="$IFS"
IFS=$'\n'
  
result=$(curl --location --request GET "https://$instanaurl/api/application-monitoring/applications?nameFilter=&windowSize=&to=&page=&pageSize=&applicationBoundaryScope=" --header "Authorization: apiToken $apitoken")
list=$(echo "$result" | jq .items | jq '.[]' |jq -rc '{id: .id, application: .label, service: ._links.services}')

rm -rf servicesapplications.json

 for row in $list; 
 do
      _jq() {
      echo "${row}" | jq -r "${1}" 
     }
     id=$(_jq '.id')
     application=$(_jq '.application')
     serviceURL=$(_jq '.service')
     servicesJson=$(curl --silent --location --request GET $serviceURL --header "Authorization: apiToken $apitoken")
     services=$(echo "$servicesJson" | jq .items | jq '.[]' | jq -c '.label')
     for service in $services;
     do
      if [[ "[]" != "$service" ]];
      then
       service=$(echo $service | sed ':a;N;$!ba;s/\n/ /g')
       echo "{\"service\": $service, \"application\": \"$application\"}" >> servicesapplications.tmp
      fi
     done
  done

echo cat servicesapplications.tmp | jq -s >> servicesapplications.json

echo "{\"serviceapplicationmapping\":"  >> servicesapplications.json
cat servicesapplications.tmp | jq -s >> servicesapplications.json
echo "}" >> servicesapplications.json
rm -rf servicesapplications.tmp