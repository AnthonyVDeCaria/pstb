## @file
## This file contains several functions and constants used throughout the scripts.

## An integer Regular Expression
intRegEx='^[0-9]+$'

## Sees if a given value is an integer
## @param the value to check
## @return 0 if successful; 1 otherwise
checkIfInt() {
	if [[ $1 =~ $intRegEx ]] ; 
	then
		return 0
	else
		return 1
	fi
}
