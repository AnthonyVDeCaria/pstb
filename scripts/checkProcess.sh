#!/bin/bash

if [ $# -ne 1 ]; 
then
    echo "[Error] Not enough arguments provided! <process_regex>"
    exit 1
fi

process_regex=$1

echo "Seeing if we're running $process_regex";

ps -ef | grep -v grep | grep -v checkProcess.sh | grep "$process_regex"

if [ "$?" != "0" ]; 
then
    echo "[Error] process isn't running!"
    exit 1
else
	echo "Process is running."
fi