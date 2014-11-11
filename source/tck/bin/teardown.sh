#!/bin/bash

#set -euo pipefail

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")
declare -r CONFIG=${ROOT}/../etc/urmia.conf

declare -r -i ods_count=2

## MDS
${ROOT}/../../srv-obj-api/bin/stop-mds.sh -c ${CONFIG}

## ODS
for o in `seq 1 ${ods_count}`; do
    echo "checking ODS: $o"
    ${ROOT}/../../srv-obj-store/bin/stop-ods.sh -c ${CONFIG}
done

## JDS
${ROOT}/../../srv-job-api/bin/stop-job-api.sh -c ${CONFIG}

## JRS
for o in `seq 1 ${ods_count}`; do
    echo "checking JRS: $o"
    ${ROOT}/../../srv-job-run/bin/stop-job-run.sh -c ${CONFIG}
done
