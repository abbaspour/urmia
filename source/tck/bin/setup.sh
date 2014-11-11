#!/bin/bash

set -euo pipefail

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")
declare -r CONFIG=${ROOT}/../etc/urmia.conf
declare -r CLI_ADMIN=${ROOT}/../../cli-admin/target/urmia/cli-admin/bin

. ${ROOT}/../etc/tck.conf

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

## requirments
which psql >/dev/null || { echo >&2 "psql command not in path" ; exit 1; }


## check MDS
pid=$(pid_on_port ${mds_port})
if [[ -z "$pid" ]]; then
    echo -e "MDS is not up";
    ${ROOT}/../../srv-obj-api/target/urmia/srv-obj-api/bin/start-mds.sh -c ${CONFIG}
else
    echo -e "MDS up on port: ${mds_port} with PID: $pid"
fi


## check ODS
echo "ODS count: $ods_count"

for o in `seq 1 ${ods_count}`; do
    echo "checking ODS: $o"
    port=${ods_port[$o]}
    fs=${ods_fs[$o]}

    echo -e "\tODS $o FS: $fs"
    [[ $fs != "/var/tmp/*" ]] || { echo >&2 "path is not in /var/tmp"; exit 3; }

    if [ ! -e $fs ]; then
        echo -e "\tmkdirs"
        mkdir -p $fs
    else
        echo -e "\tcleaing up"
        rm -rf $fs/*
    fi

    pid=$(pid_on_port $port)
    if [[ -z "$pid" ]]; then
        echo -e "\tODS $o is not up";
        ${ROOT}/../../srv-obj-store/target/urmia/srv-obj-store/bin/start-ods.sh -c ${CONFIG}
        sleep 5
    else
        echo -e "\tODS $o up on port: $port with PID: $pid"
    fi

done

## check job api (JDS)
pid=$(pid_on_port ${jds_port})
if [[ -z "$pid" ]]; then
    echo -e "JDS is not up";
    ${ROOT}/../../srv-job-api/target/urmia/srv-job-api/bin/start-job-api.sh -c ${CONFIG}
else
    echo -e "JDS up on port: $jds_port with PID: $pid"
fi

## check job runner
echo "Job runner count: $ods_count"

for o in `seq 1 ${ods_count}`; do
    echo "checking job runner: $o"
    fs=${ods_fs[$o]}

    echo -e "\tJob Runner $o FS: $fs"

    if [ ! -e ${fs} ]; then
        echo -e "\tmkdirs"
        mkdir -p ${fs}
    fi

    #pid=$(ps -ef | grep java | grep job | awk '{print $2}')
    #if [[ -z "$pid" ]]; then
        echo -e "\tJob Runner $o is not up";
        ${ROOT}/../../srv-job-run/target/urmia/srv-job-run/bin/start-job-run.sh -c ${CONFIG}
        sleep 1
    #else
    #    echo -e "\tJob Runner $o up on port: $port with PID: $pid"
    #fi

done
