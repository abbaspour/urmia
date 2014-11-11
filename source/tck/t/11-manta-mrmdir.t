#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

declare -r user='tck'
export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=${user}
export MANTA_KEY_ID=$(ssh-keygen -l -f $HOME/.ssh/id_rsa.pub | awk '{print $2}')

. ${ROOT}/../lib/tap.sh
. ${ROOT}/../lib/util.sh

stor=/${user}/stor

## setup
test_count 3

## 1) mrmdir sunny day
folder=$(random_string)
mmkdir -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mmkdir ${stor}/${folder}"
mrmdir -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mrmdir ${stor}/${folder}"
ok "mrmdir ${stor}/${folder}"

## 2) non empty folder should fail
folder=$(random_string)
mmkdir -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mmkdir -u ${MANTA_URL} ${stor}/${folder}"
file=$(random_file)
name=$(basename ${file})
mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}/${folder}/${name}
[[ $? -eq 0 ]] || fail "mput -f ${file} ${stor}/${folder}/${name}"
mrmdir -u ${MANTA_URL} ${stor}/${folder} 2>/dev/null
[[ $? -ne 0 ]] || fail "mrmdir ${stor}/${folder}"
ok "no mrmdir none empty ${stor}/${folder}"

## 3) mrmdir should work if folder becomes empty
mrm -u ${MANTA_URL} ${stor}/${folder}/${name}
[[ $? -eq 0 ]] || fail "mrm ${stor}/${folder}/${name}"
mrmdir -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mrmdir ${stor}/${folder}"
ok "mrmdir empty ${stor}/${folder}"
