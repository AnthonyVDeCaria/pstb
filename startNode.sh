#!/bin/bash

dis=$1
isBroker=$2
memory=$3
name=$4
context=$5
master=$6
port=$7
user=$8

if [ $# -ne 8 ]; 
then
    echo "[Error] Not enough arguments provided! <isDistributed> <isBroker> <memory> <name> <context> <master> <port> <user>"
    exit 10 #N_ARGS
fi

if [ $isBroker -eq 1 ];
then
	echo "Starting broker $name"
	
	java -Xmx"$memory"M \
			-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
			-Djava.rmi.server.codebase=file:lib/padres.jar \
			-Djava.security.policy=etc/java.policy \
			-Djava.awt.headless=true \
			pstb.benchmark.broker.PhysicalBroker \
			-n $name \
			-c $context \
			-m $master \
			-p $port \
			-d $dis \
			-u $user
	
	exitVal=$?
	echo "$exitVal"
	exit $exitVal
else
	echo "Starting client $name"
	
	java -Xmx"$memory"M \
			-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
			-Xverify:none \
			pstb.benchmark.client.PhysicalClient \
			-n $name \
			-c $context \
			-m $master \
			-p $port \
			-d $dis \
			-u $user
			
	exitVal=$?
	echo "$exitVal"
	exit $exitVal
fi

