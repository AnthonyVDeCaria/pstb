#!/bin/bash

user=$1
machine=$2
isBroker=$3
memory=$4
name=$5
context=$6
master=$7
port=$8

if [ $# -ne 8 ]; 
then
    echo "[Error] Not enough arguments provided! <user> <machine> <isBroker> <memory> <name> <context> <master> <port>"
    exit 10 #N_ARGS
fi

ssh -t -t $user@$machine <<-EOF
	cd ~/PSTB
	./startNode.sh "true" $isBroker $memory $name $context $master $port $user
	exit
EOF

