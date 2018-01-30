#!/bin/bash

user=$1
machine=$2
memory=$3
name=$4
context=$5
ipAddress=$6
objectPort=$7
engine=$8
nodetype=$9

if [ $# -ne 9 ]; 
then
	echo "ERROR:	Not enough arguments provided! "
	echo "<username> <machine> <memory> <name> <context> <ipAddress> <objectPort> <engine> <nodetype>"
    exit 10 #N_ARGS
fi

ssh -t -t $user@$machine <<-EOF
	cd ~/PSTB
	./startNode.sh $memory $name $context $ipAddress $objectPort $engine $nodetype -d $user
	exit
EOF

