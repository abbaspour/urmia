#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

declare -r user='tck'
export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=${user}
export MANTA_KEY_ID=$(ssh-keygen -l -f $HOME/.ssh/id_rsa.pub | awk '{print $2}')

. ${ROOT}/../lib/tap.sh
. ${ROOT}/../lib/util.sh

## setup
test_count 9

## mls connects
mls -u ${MANTA_URL} 2>&1 1>/dev/null
if [ $? -eq 0 ]; then
    ok "mls -u $MANTA_URL"
else
    fail "mls -u $MANTA_URL"
fi

## mls work with env variable
mls 2>&1 1>/dev/null
if [ $? -eq 0 ]; then
    ok "mls"
else
    fail "mls"
fi

## mls pointing to root of stor ns
mls -u ${MANTA_URL} /${user}/stor 2>&1 1>/dev/null
if [ $? -eq 0 ]; then
    ok "mls -u $MANTA_URL /$user/stor"
else
    fail "mls -u $MANTA_URL /$user/stor"
fi

## TODO mls pointing to invalid ns (mls: ResourceNotFoundError: /abbaspour/invalid does not exist)
# err":{"message":"/abbaspour/invalid does not exist","name":"ResourceNotFoundError"}
err_msg=$(mls -u ${MANTA_URL} /${user}/invalid 2>&1)
if [ $? -ne 0 ]; then
    code=$(echo ${err_msg} | awk -F: '{print $2}')
    msg=$(echo ${err_msg} | awk -F: '{print $3}')
    if [ "x${code}" == "x ResourceNotFoundError" ] && [ "x${msg}" == "x /${user}/invalid does not exist" ]; then
        ok "mls -u $MANTA_URL /$user/invalid. code: $code, msg: $msg"
    else
        fail "mls -u $MANTA_URL /$user/invalid. code: $code, msg: $msg"
    fi
else
    fail "mls -u $MANTA_URL /$user/invalid"
fi


ns_count=`mls -l -u ${MANTA_URL} /${user} | grep -e ^d | wc -l`
if [ $? -eq 0 ]; then
    if [ ${ns_count} -eq 4 ]; then
        ok "mls -l -u $MANTA_URL /$user returned 4 public namespaces"
    else
        fail "mls -l -u $MANTA_URL /$user returned $ns_count public namespaces. expected 4"
    fi
else
    fail "mls -u $MANTA_URL /$user/ not returned namespaces"
fi

## put a file and should apper in list
f1=$(random_file 1k)
mput -q -f ${f1} /${user}/stor
f1b=`basename ${f1}`
f1c=$(mls -u ${MANTA_URL} /${user}/stor | grep -c "${f1b}")
if [ ${f1c} -eq 1 ]; then
    ok "mls -u ${MANTA_URL} /$user/stor contains mput file ($f1b)"
else
    fail "mls -u ${MANTA_URL} /$user/stor not showing $f1"
fi

## size of mls matches uploaded file
msize=`mls -l -u ${MANTA_URL} /${user}/stor | grep "$f1b" | awk '{print $4}'`
lsize=`ls -l ${f1} | awk '{print $5}'`

if [ ${msize} -eq ${lsize} ]; then
    ok "local size ($lsize) == uploaded size ($msize)"
else
    fail "local size ($lsize) != uploaded size ($msize)"
fi

## it's a file not directory
type=`mls -l -u ${MANTA_URL} /${user}/stor | grep "$f1b" | awk '{print $1}'`

if [[ ${type} == -* ]]; then
    ok "type is file"
else
    fail "type is not file"
fi

## delete the file and not listed again
mrm -u ${MANTA_URL} /${user}/stor/${f1b}
[[ $? -eq 0 ]] || { fail "not able to remove file: mrm -u ${MANTA_URL} /${user}/stor/${f1b}"; }
f1c=$(mls -u ${MANTA_URL} /${user}/stor | grep -c "${f1b}")
if [ ${f1c} -eq 0 ]; then
    ok "mls -u ${MANTA_URL} /$user/stor doesn't contains file after remove ($f1b)"
else
    fail "mls -u ${MANTA_URL} /$user/stor showing $f1b after mrm"
fi

## TODO: create dirs and list then the mrmdir
