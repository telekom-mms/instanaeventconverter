#!/bin/bash


start() {

cd /opt/InstanaEventConverter/config
#./appliationservices.sh 
#./servicesapplications.sh

cd /opt/InstanaEventConverter/

# check if servicesapplication.json 

nohup /usr/lib/jvm/jre/bin/java -jar -Djavax.net.ssl.trustStore=/opt/instanaEventconverter/myTrustStore instanaEventConverter.jar >/dev/null 2>&1 &
echo $! > instanaEventConverter.pid

}

stop() {
cd /opt/instanaEventConverter/
kill -9 `cat instanaEventConverter.pid`
rm instanaEventConverter.pid

}

case "$1" in 
    start)
       start
       ;;
    stop)
       stop
       ;;
    *)
       echo "Usage: $0 {start|stop}"
esac

exit 0 
