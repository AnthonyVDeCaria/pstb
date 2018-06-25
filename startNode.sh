#!/bin/bash
. scripts/functions.sh

error() 
{ 
	echo "ERROR:	Not enough arguments provided! "
	echo "<memory> <name> <context> <ipAddress> <objectPort> <engine> <nodetype> <sendDiary>"
	echo "(OPTIONAL) -d <username> (this is distributed)"
	exit 10 #N_ARGS 
}

if [ $# -lt 8 ]; 
then
    error
fi

memory=$1
shift

name=$1
shift

context=$1
shift

ipAddress=$1
shift

objectPort=$1
shift

engine=$1
shift

nodetype=$1
shift

sendDiary=$1
shift

while getopts "d:" opt; do
    case "${opt}" in
        d)
        	username=${OPTARG}
            ;;
        \?)
        	error
        	;;
    esac
done


checkIfInt $memory
memCheck=$?
if [[ $memCheck -ne 0 ]] ; 
then
   echo "ERROR:	Memory is not an integer!"
   exit 10 #N_ARGS
fi

checkIfInt $objectPort
opCheck=$?
if [[ $opCheck -ne 0 ]] ; 
then
   echo "ERROR:	ObjectPort is not an integer!"
   exit 10 #N_ARGS
fi

java -Xmx"$memory"M \
			-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
			-Djava.rmi.server.codebase=file:target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
			-Djava.security.policy=etc/java.policy \
			-Djava.awt.headless=true \
			pstb.benchmark.process.PSTBProcess \
			$name $context $ipAddress $objectPort $engine $nodetype $sendDiary $username
	
exitVal=$?
echo "$exitVal"
exit $exitVal

