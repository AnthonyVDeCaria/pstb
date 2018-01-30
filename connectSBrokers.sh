#!/bin/bash

broker1URI=$1
broker2ID=$2
broker2URI=$3

if [ $# -ne 3 ]; 
then
    echo "[Error] Not enough arguments provided! <broker1URI> <broker2ID> <broker2URI>"
    exit 10 #N_ARGS
fi

echo "Connecting SIENA brokers $broker1URI and $broker2URI"
	
java -cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar siena.DVDRPControl $broker1URI connect $broker2ID $broker2URI

exitVal=$?
echo "$exitVal"
exit $exitVal


