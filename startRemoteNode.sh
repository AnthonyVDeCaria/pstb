#!/bin/bash

machine=$1
isBroker=$2
memory=$3
name=$4
context=$5
master=$6
port=$7
user=$8

if [ $# -ne 8 ]; 
then
    echo "[Error] Not enough arguments provided! <machine> <isBroker> <memory> <name> <context> <master> <port> <user>"
    exit 10 #N_ARGS
fi

ssh -t -t $user@$machine <<-EOF
	cd ~/PSTB
	./startNode.sh "true" $isBroker $memory $name $context $master $port $user
	exit
EOF

