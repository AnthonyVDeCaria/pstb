#!/bin/bash

#NOTE: This code will not work on other machines
#		Replace this code with your own

user=$1

echo "Delpoying files to nodes"	
for i in $(seq 1 24)
do
	if [ "$i" != "10" ]
	then
		echo "Deploying files to node $i"
		scp -r ~/PSTB $user@n$i:~
		if [ "$?" != "0" ]; 
		then
		    echo "[Error] deploying PSTB failed!"
		    exit 1
		else
			echo "Deploying PSTB on node $i successful."
		fi
	fi
done
exit


