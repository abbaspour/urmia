#!/bin/bash

set -euo pipefail

declare -r VER=0.1.0
declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")
declare -r PKG=srv-job-run
declare LIB=${ROOT}/../lib
[[ -e ${LIB} ]] || LIB=${ROOT}/../target/urmia/${PKG}/lib
declare -r JAR=${LIB}/${PKG}-${VER}.jar

[[ -d ${LIB} ]] || { /bin/echo >&2 "lib folder file not found: $JAR"; exit 2; }
[[ -e ${JAR} ]] || { /bin/echo >&2 "main jar file not found: $JAR"; exit 2; }

declare CONFIG=${ROOT}/../etc/urmia.conf
[[ -e ${CONFIG} ]] || CONFIG=/opt/urmia/${PKG}/etc/urmia.conf

declare opt=''

while getopts "c:" opt
do
    case ${opt} in
        c) CONFIG=${OPTARG};;
    esac
done

declare LOGBACK=${ROOT}/../etc/logback.xml
[[ -e ${LOGBACK} ]] || LOGBACK=/opt/urmia/${PKG}/etc/logback.xml

declare HOST=${HOSTNAME}
declare ZOOKEEPER=${HOSTNAME}:2181

declare JAVA=`/usr/bin/which java`
#[[ $? -eq 0 ]] || JAVA=/opt/local/java/openjdk7/bin/java
[[ $? -eq 0 ]] || JAVA=/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java

declare LOG_PATH=/var/log/urmia
[[ -e ${LOG_PATH} ]] || LOG_PATH=${ROOT}/../var/log

declare RUN_PATH=/var/run/urmia
[[ -e ${RUN_PATH} ]] || RUN_PATH=${ROOT}/../var/run

if [ -e ${CONFIG} ]; then
    . ${CONFIG}
else
    /bin/echo "using default values. not found: $CONFIG"
fi

/bin/mkdir -p ${LOG_PATH}
/bin/mkdir -p ${RUN_PATH}

JAVA_ARGS="-Djava.net.preferIPv4Stack=true"

LOGBACK_ABS=$(readlink -m ${LOGBACK})
[[ ! -e "${LOGBACK_ABS}" ]] || JAVA_ARGS+=" -Dlogback.configurationFile=${LOGBACK_ABS}"

${JAVA} ${JAVA_ARGS} -jar ${JAR} -z ${ZOOKEEPER} -h ${HOST} &

pid=$!

/bin/echo "started process at pid: $pid"

/bin/echo ${pid} > ${RUN_PATH}/run-${pid}.pid
/bin/rm -f ${RUN_PATH}/run.pid
/bin/ln -s ${RUN_PATH}/run-${pid}.pid ${RUN_PATH}/run.pid
