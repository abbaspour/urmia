#!/bin/bash

set -euo pipefail

declare -r VER=0.1.0
declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")
declare LIB=${ROOT}/../lib
[[ -e ${LIB} ]] || LIB=${ROOT}/../target/urmia/srv-job-api/lib
declare -r JAR=${LIB}/srv-job-api-${VER}.jar

[[ -d ${LIB} ]] || { echo >&2 "lib folder file not found: $JAR"; exit 2; }
[[ -e ${JAR} ]] || { echo >&2 "main jar file not found: $JAR"; exit 2; }

declare CONFIG=${ROOT}/../etc/urmia.conf
[[ -e ${CONFIG} ]] || CONFIG=/opt/urmia/etc/urmia.conf

declare opt=''

while getopts "c:" opt
do
    case ${opt} in
        c) CONFIG=${OPTARG};;
    esac
done

declare LOGBACK=${ROOT}/../etc/logback.xml
[[ -e ${LOGBACK} ]] || LOGBACK=/opt/urmia/etc/logback.xml

declare HOST=${HOSTNAME}
declare ZOOKEEPER=${HOSTNAME}:2181

declare JAVA=`which java`
[[ $? -eq 0 ]] || JAVA=/opt/local/java/openjdk7/bin/java

declare LOG_PATH=/var/log/urmia
[[ -e ${LOG_PATH} ]] || LOG_PATH=${ROOT}/../var/log

declare RUN_PATH=/var/run/urmia
[[ -e ${RUN_PATH} ]] || RUN_PATH=${ROOT}/../var/run

if [ -e ${CONFIG} ]; then
    . ${CONFIG}
else
    echo "using default values. not found: $CONFIG"
fi

mkdir -p ${LOG_PATH}
mkdir -p ${RUN_PATH}

JAVA_ARGS="-Djava.net.preferIPv4Stack=true"

LOGBACK_ABS=$(readlink -m ${LOGBACK})
[[ ! -e "${LOGBACK_ABS}" ]] || JAVA_ARGS+=" -Dlogback.configurationFile=${LOGBACK_ABS}"

${JAVA} ${JAVA_ARGS} -jar ${JAR} -z ${ZOOKEEPER} -h ${HOST} &

pid=$!

echo "started process at pid: $pid"

echo ${pid} > ${RUN_PATH}/job-${pid}.pid
