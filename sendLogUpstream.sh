#!/bin/bash

user=$1
machine=$2

name="$(hostname)"

echo "Sending log upstream"
scp -r ~/PSTB/logs $user@$machine:~/nodelogs/$name
