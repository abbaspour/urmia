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
test_count 5

## 1) simple mrm
file=$(random_file)
name=$(basename ${file})
mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}
[[ $? -eq 0 ]] || { fail "mmput failed: mput -u ${MANTA_URL} -f ${file} ${stor}"; }
mrm -u ${MANTA_URL} ${stor}/${name}
[[ $? -eq 0 ]] || fail "mrm -u ${MANTA_URL} ${stor}/${name}"
ok "mrm ${stor}/${name}"

## 2) not in mls anymore
ls_count=$(mls -u ${MANTA_URL} ${stor}/${name} 2>/dev/null | wc -l)
[[ $? -eq 0 ]] || fail "mls -u ${MANTA_URL} ${stor}/${name} | wc -l"
[[ ${ls_count} -eq 0 ]] || fail "mls -u ${MANTA_URL} ${stor}/${name} |  wc-l"
ok "not in mls result"

## 3) mrm folder should fail
folder=$(random_string)
mmkdir -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mmkdir -u ${MANTA_URL} ${stor}/${folder}"
result=$(mrm -u ${MANTA_URL} ${stor}/${folder} 2>&1)
[[ $? -ne 0 ]] || fail "mrm removed a dir mrm -u ${MANTA_URL} ${stor}/${folder}"
[[ "$result" == "${stor}/${folder} is not an object" ]] || fail "not received 'is not an object reponse'"
ok "mrm on folder ${stor}/${folder}"

## 4) mrm on folder
folder=$(random_string)
mmkdir -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mmkdir -u ${MANTA_URL} ${stor}/${folder}"
mrm -r -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mrm -r -u ${MANTA_URL} ${stor}/${folder}"
ok "mrm -r ${stor}/${folder}"

## 5) mrm on folder with content
folder=$(random_string)
mmkdir -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mmkdir -u ${MANTA_URL} ${stor}/${folder}"
file=$(random_file)
name=$(basename ${file})
mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}/${folder}
mrm -r -u ${MANTA_URL} ${stor}/${folder}
[[ $? -eq 0 ]] || fail "mrm -r -u ${MANTA_URL} ${stor}/${folder}"
ls_count=$(mls -u ${MANTA_URL} ${stor}/${folder}/${name} 2>/dev/null | wc -l)
[[ $? -eq 0 ]] || fail "mls -u ${MANTA_URL} ${stor}/${folder}/${name} | wc -l"
[[ ${ls_count} -eq 0 ]] || fail "mls -u ${MANTA_URL} ${stor}/${folder}/${name} |  wc-l"
ok "mrm -r ${stor}/${folder}/${name}"

