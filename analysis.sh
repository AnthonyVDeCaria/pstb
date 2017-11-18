#!/bin/bash
memory=$1
file=$2

java -Xmx"$memory"M -cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar pstb.analysis.Analyzer -f "$file"
