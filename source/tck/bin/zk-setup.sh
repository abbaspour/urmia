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

## check ZK nodes. create is missing
[ -e ${CLI_ADMIN} ] || { echo >&2 "cli-admin not built at: $CLI_ADMIN"; exit 1; }

## mdb zk
declare -i -r zk_mdb_count=$(node_count -t MDB -h ${mdb_host}) ## todo: consider mdb_host for query
if [ ${zk_mdb_count} == 0 ]; then
    echo "MDB node not defined in zk. adding"
    #node_add MDB 5432 ${mdb_db}
    ${CLI_ADMIN}/add-node.sh -t MDB -h $mdb_host -p 5432 -u ${mdb_db}
else
    echo "MDB node count: $zk_mdb_count"
fi

## mds zk
declare -i -r zk_mds_count=$(node_count -t MDS -p ${mds_port})
if [ ${zk_mds_count} == 0 ]; then
    echo "MDS node not defined in zk. adding"
    node_add MDS ${mds_port}
else
    echo "MDS node count on port $mds_port: $zk_mds_count"
fi

## jds zk
declare -i -r zk_jds_count=$(node_count -t JDS -p ${jds_port})
if [ ${zk_jds_count} == 0 ]; then
    echo "JDS node not defined in zk. adding"
    node_add JDS ${jds_port}
else
    echo "JDS node count on port $jds_port: $zk_jds_count"
fi

## ods zk
for((o=1; o<=$ods_count; o++)); do
    port=${ods_port[$o]}
    fs=${ods_fs[$o]}

    declare -i zk_ods_count=$(node_count -t ODS -p ${port})
    if [ ${zk_ods_count} -ge 1 ]; then
        echo "ODS node count on port $port: $zk_ods_count"
    else
        echo "not found. adding ODS node on port $port and fs: $fs"
        node_add ODS ${port} ${fs}
    fi

done

## jrs zk
declare -i zk_jrs_count=$(node_count -t JRS)
if [ ${zk_jrs_count} -ge ${ods_count} ]; then
    echo "JRS node count: $zk_jrs_count"
else
    delta=$(($ods_count - $zk_jrs_count))
    for((i=0; i<$delta; i++)); do
        echo "adding JRS node: $i"
        node_add JRS 0
    done
fi
