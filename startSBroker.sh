#!/bin/bash

brokerID=$1
brokerURI=$2

if [ $# -ne 2 ]; 
then
    echo "[Error] Not enough arguments provided! <brokerID> <brokerURI>"
    exit 10 #N_ARGS
fi

echo "Starting SIENA broker $brokerID"
	
java -cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar siena.StartDVDRPServer -id $brokerID -receiver $brokerURI &

exitVal=$?
echo "$exitVal"
exit $exitVal


