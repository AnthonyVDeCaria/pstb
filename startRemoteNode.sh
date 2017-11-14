#!/bin/bash

user=$1
machine=$2
isBroker=$3
memory=$4
name=$5
context=$6
master=$7

if [ $# -ne 7 ]; 
then
    echo "[Error] Not enough arguments provided! <user> <machine> <isBroker> <memory> <name> <context/diary> <master>"
    exit 1
fi

ssh -t -t $user@$machine <<-EOF
	cd ~/PSTB
	./startNode.sh $isBroker $memory $name $context $master
	exit
EOF

