#!/bin/bash

set -euo pipefail
#IFS=$'\n\t'

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")
declare -r PKG=srv-job-api

declare -r -i RETRY=3
declare -r SLEEP=0.5

declare CONFIG=${ROOT}/../etc/urmia.conf
[[ -e ${CONFIG} ]] || CONFIG=/opt/urmia/${PKG}/etc/urmia.conf

declare opt=''

while getopts "c:" opt
do
    case ${opt} in
        c) CONFIG=${OPTARG};;
    esac
done

declare RUN_PATH=/var/run/urmia
[[ -e ${RUN_PATH} ]] || RUN_PATH=${ROOT}/../var/run

if [ -e ${CONFIG} ]; then
    . ${CONFIG}
    #CFG_PATH_RUN=$(awk -F= '/^path_run=/{print $2}' ${CONFIG})
    #[[ -z "$CFG_PATH_RUN" ]] || RUN_PATH=${CFG_PATH_RUN}
fi

declare -r PID_FILE=$(ls -1 -t ${RUN_PATH}/job-*.pid 2>/dev/null | head -1)

[[ ! -z "${PID_FILE}" ]] || { echo >&2 "no run.pid file"; exit 1; }
[[ -e "${PID_FILE}" ]] || { echo >&2 "no run.pid file"; exit 1; }

declare -r pid=`cat ${PID_FILE}`

[[ ! -z ${pid} ]] || { echo >&2 "no pid found"; exit 1; }

echo -n "pid is: $pid"

for i in `seq 1 ${RETRY}`; do
    set +e
    kill -15 ${pid} 2>/dev/null 1>/dev/null
    ps -p ${pid} 2>/dev/null 1>/dev/null

    if [[ $? -eq 1 ]]; then
        echo "done"; rm -f "${PID_FILE}"; exit 0;
    else
        echo -n ".";
        sleep ${SLEEP}
    fi
    set -e
done

exit 1