#!/bin/bash
. scripts/functions.sh

error()
{ 
	echo "ERROR:	Improper arguments provided! "
	echo "<memory>"
	echo "(OPTIONAL) -p (print diaries to a file)"
	echo "(OPTIONAL) -f <filePath> (analyze the data using this AnalysisFile)"
	echo "At least ONE of these flags must be set!"
	exit 10 #N_ARGS 
}

initFile="null"
initPrint="false"

minArgs=2
maxArgs=4
justPrntArgs=2
justFileArgs=3

if [ $# -lt $minArgs ] || [ $# -gt $maxArgs ] ; 
then
    error
fi

numArgs=$#

memory=$1
shift

file=$initFile
print=$initPrint

while getopts "pf:" opt; do
    case "${opt}" in
        p)
        	print="true"
        	;;
        f)
        	file=${OPTARG}
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

if [ "$file" = "$initFile" ] && [ "$print" = "$initPrint" ] ;
then
	error
fi

if [ "$file" = "$initFile" ] && [ "$print" != "$initPrint" ] && [ $numArgs -ne $justPrntArgs ] ;
then
	error
fi

if [ "$file" != "$initFile" ] && [ "$print" = "$initPrint" ] && [ $numArgs -ne $justFileArgs ] ;
then
	error
fi

java -Xmx"$memory"M -cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar pstb.analysis.Analyzer "$print" "$file"
