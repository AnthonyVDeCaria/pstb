#!/bin/bash

user=$1

echo "Killing nodes on all machines"	
for i in $(seq 12 15)
do
	if [ "$i" != "10" ]
	then
		echo "Killing nodes on machine n$i"
		ssh -t -t $user@n$i<<-EOF
			cd PSTB/scripts
			./killAllNodesOnThisMachine.sh
			exit
		EOF
	fi
done
