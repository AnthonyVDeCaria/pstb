#!/bin/bash
. scripts/functions.sh

error()
{ 
	echo "ERROR:	Improper arguments provided! "
	echo "<memory>"
	echo "(OPTIONAL) -d (record diaries to a file)"
	echo "(OPTIONAL) -r (create a report on the data for each experiment)"
	echo "(OPTIONAL) -a <analysisPath> (analyze the data using this AnalysisFile)"
	echo "At least ONE of these flags must be set!"
	exit 10 #N_ARGS 
}

initDiary="false"
initReport="false"
initAnalysis="null"

minArgs=2
maxArgs=5
justDiaryArgs=2
justReporArgs=2
justAnalyArgs=3
diaryReporArgs=3
diaryAnalyArgs=4
reporAnalyArgs=4

numArgs=$#
if [ $numArgs -lt $minArgs ] || [ $numArgs -gt $maxArgs ] ; 
then
    error
fi

memory=$1
shift

diary=$initDiary
report=$initReport
analysis=$initAnalysis

while getopts "dra:" opt; do
    case "${opt}" in
        d)
        	diary="true"
        	;;
        r)
        	report="true"
        	;;
        a)
        	analysis=${OPTARG}
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

if [ "$diary" = "$initDiary" ] && [ "$report" = "$initReport" ] && [ "$analysis" = "$initAnalysis" ] ;
then
	echo "1"
	error
fi

if [ "$diary" != "$initDiary" ] && [ "$report" = "$initReport" ] && [ "$analysis" = "$initAnalysis" ] && [ $numArgs -ne $justDiaryArgs ] ;
then
	echo "2"
	error
fi

if [ "$diary" = "$initDiary" ] && [ "$report" != "$initReport" ] && [ "$analysis" = "$initAnalysis" ] && [ $numArgs -ne $justReporArgs ] ;
then
	echo "3"
	error
fi

if [ "$diary" = "$initDiary" ] && [ "$report" = "$initReport" ] && [ "$analysis" != "$initAnalysis" ] && [ $numArgs -ne $justAnalyArgs ] ;
then
	echo "4"
	error
fi

if [ "$diary" != "$initDiary" ] && [ "$report" != "$initReport" ] && [ "$analysis" = "$initAnalysis" ] && [ $numArgs -ne $diaryReporArgs ] ;
then
	echo "5"
	error
fi

if [ "$diary" != "$initDiary" ] && [ "$report" = "$initReport" ] && [ "$analysis" != "$initAnalysis" ] && [ $numArgs -ne $diaryAnalyArgs ] ;
then
	echo "6"
	error
fi

if [ "$diary" = "$initDiary" ] && [ "$report" != "$initReport" ] && [ "$analysis" != "$initAnalysis" ] && [ $numArgs -ne $reporAnalyArgs ] ;
then
	echo "7"
	error
fi

java -Xmx"$memory"M -cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar pstb.analysis.ThroughputAnalyzer "$diary" "$report" "$analysis"
