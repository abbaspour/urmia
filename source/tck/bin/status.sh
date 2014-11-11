#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

. ${ROOT}/../lib/util.sh

declare -r -i mds_port=8085
declare -r -i ods_count=2

## ods 1
ods_port[1]=8081
ods_fs[1]=/var/tmp/urmia/tck/ods/1

## ods 2
ods_port[2]=8082
ods_fs[2]=/var/tmp/urmia/tck/ods/2

declare -r -i jds_port=8086

## check MDS
pid=$(pid_on_port ${mds_port})
if [[ -z "$pid" ]]; then
    echo -e "MDS is not up";
else
    echo -e "MDS up on port: $mds_port with PID: $pid"
fi


## check ODS
echo "ODS count: $ods_count"

for o in `seq 1 $ods_count`; do
    echo "checking ODS: $o"
    port=${ods_port[$o]}
    fs=${ods_fs[$o]}

    pid=$(pid_on_port $port)
    if [[ -z "$pid" ]]; then
        echo -e "\tODS $o is not up";
    else
        echo -e "\tODS $o up on port: $port with PID: $pid fs:$fs"
    fi

done

## check JDS
pid=$(pid_on_port ${jds_port})
if [[ -z "$pid" ]]; then
    echo -e "JDS is not up";
else
    echo -e "JDS up on port: $jds_port with PID: $pid"
fi


## check JRS
echo "ODS count: $ods_count"

for o in `seq 1 ${ods_count}`; do
    echo "checking JDS: $o"
    #location=${ods_location[$o]}
    fs=${ods_fs[$o]}
    # todo: check the pid.file not ps
    pid=$(ps -ef | grep java | grep srv-job-run | awk '{print $2}' | head -1)
    if [[ -z "$pid" ]]; then
        echo -e "\tJDS $o is not up";
    else
        echo -e "\tJDS $o up on with PID: $pid fs:$fs"
    fi

done

