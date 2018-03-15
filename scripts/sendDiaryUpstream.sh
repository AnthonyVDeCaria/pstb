#!/bin/bash

user=$1
machine=$2
diaryName=$3

echo "Sending diaries upstream"
scp -r ~/PSTB/$diaryName $user@$machine:~/PSTB
exitVal=$?
echo "$exitVal"
exit $exitVal
