#!/bin/bash

if [ $# -lt 3 ]; 
then
    echo "[Error] Not enough arguments provided! <user> <machine> <process>"
    exit 1
fi

user=$1
machine=$2
process=$3

ssh -t -t $user@$machine <<-EOF
	cd ~/PSTB/scripts
	./checkProcess.sh $process
	exit
EOF
