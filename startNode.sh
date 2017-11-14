#!/bin/bash

isBroker=$1
memory=$2
name=$3
context=$4
master=$5

if [ $# -ne 5 ]; 
then
    echo "[Error] Not enough arguments provided! <isBroker> <memory> <name> <context/diary> <master>"
    exit 1
fi

if [ $isBroker -eq 1 ];
then
	echo "Starting broker $name"
	
	java -Xmx"$memory"M \
			-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar\
			-Djava.awt.headless=true \
			pstb.benchmark.PhysicalBroker \
			-n $name \
			-c $context \
			-m $master
			
	echo "$?"
else
	echo "Starting client $name"
	
	java -Xmx"$memory"M \
			-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar\
			-Xverify:none \
			pstb.benchmark.PhysicalClient \
			-n $name \
			-d $context \
			-m $master
			
	echo "$?"
fi

