#!/bin/bash

user=$1
machine=$2
request=$3

name="$(hostname)"

echo "Sending data upstream"

if [ "$request" = "logs" ];
then
	echo "Sending logs upstream"
	scp -r ~/PSTB/logs $user@$machine:~/nodelogs/$name
elif [ "$request" = "dia" ];
then
	echo "Sending diaries upstream"
	scp -r ~/PSTB/*.dia $user@$machine:~/PSTB
else
	echo "[Error] Invalid request"
	exit 1
fi
