#!/bin/bash

if [ $# -ne 10 ]; 
then
	echo "ERROR:	Not enough arguments provided! "
	echo "<username> <machine> <memory> <name> <context> <ipAddress> <objectPort> <engine> <nodetype> <sendDiary>"
    exit 10 #N_ARGS
fi

user=$1
machine=$2
memory=$3
name=$4
context=$5
ipAddress=$6
objectPort=$7
engine=$8
nodetype=$9
shift

sendDiary=$9

echo "$user $machine $memory $name $context $ipAddress $objectPort $engine $nodetype $sendDiary"

ssh -t -t $user@$machine <<-EOF
	cd ~/PSTB
	./startNode.sh $memory $name $context $ipAddress $objectPort $engine $nodetype $sendDiary -d $user
	exit
EOF

