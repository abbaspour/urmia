#!/bin/sh

## TAP compatible output
# http://en.wikipedia.org/wiki/Test_Anything_Protocol
#

ECHO=`which echo`
## todo: detech if prove or bash (look at PPID) and output color in direct run

## -- colors --
RED='\033[00;31m'
GREEN='\033[00;32m'
YELLOW='\033[00;33m'
BLUE='\033[00;34m'
WHITE='\033[01;37m'
WIPE="\033[1m\033[0m"

function test_count() {
    echo "1..$1"
}

no=0

function ok() {
    no=$(($no + 1))
    ${ECHO} "ok $no => $1"
    #${ECHO} "${GREEN}ok${WIPE} $no => $1"
}

function fail() {
    no=$(($no + 1))
    ${ECHO} "not ok $no => $1"
    #echo "${RED}not ok${WIPE} $no => $1"
}

