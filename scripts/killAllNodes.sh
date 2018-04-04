#!/bin/bash

user=$1

echo "Killing nodes on all machines"	
for i in $(seq 1 24)
do
	if [ "$i" != "10" ]
	then
		echo "Killing nodes on machine n$i"
		ssh $user@n$i bash <<-EOF
			./PSTB/scripts/killAllNodesOnThisMachine.sh
			exit
EOF
	fi
done
