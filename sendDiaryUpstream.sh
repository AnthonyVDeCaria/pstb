#!/bin/bash

error()
{ 
	echo "ERROR:	Improper arguments provided! "
	echo "<user> <machineName> <diaryPath>"
	exit 10 #N_ARGS 
}

user=$1
machine=$2
diaryName="$3"

numArgs=3

if [ $# -ne $numArgs ] ; 
then
    error
fi

echo "Sending diaries upstream"
sftp $user@$machine << EOF
	cd PSTB
	put -r $diaryName
	exit
EOF
exitVal=$?
echo "$exitVal"
exit $exitVal
