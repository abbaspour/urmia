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
test_count 8

## 1. by name
file=$(random_file 100)
name=$(basename ${file})
mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}
[[ $? -eq 0 ]] || { fail "mmput failed: mput -u ${MANTA_URL} -f ${file} ${stor}"; }

declare -i count=$(mfind -u ${MANTA_URL} -n ${name} ${stor} | wc -l)
[[ $? -eq 0 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -n name ${stor}"; }
[[ ${count} -eq 1 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -n ${name} ${stor}"; }
ok "mfind by /name/"

## 2. by limit
declare -i count=$(mfind -u ${MANTA_URL} -l 1 ${stor} | wc -l)
[[ $? -eq 0 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -l 1 ${stor}"; }
[[ ${count} -eq 1 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -l 1 ${stor}"; }
ok "mfind by limit"

## 3. by type
declare find_name=$(mfind -u ${MANTA_URL} -t o -n ${name} ${stor})
[[ $? -eq 0 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -t o -n ${name} ${stor}"; }
[[ "${find_name}" == *${name} ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -t o -n ${name} ${stor}"; }
ok "mfind by type (object)"

ok "todo: mfind by type (directory)"

## 4. by size
declare cound_big=$(mfind -u ${MANTA_URL} -t o -n ${name} -s 200 ${stor} | wc -l)
[[ $? -eq 0 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -t o -n ${name} -s 200 ${stor}"; }
[[ ${cound_big} -eq 0 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -t o -n ${name} -s 200 ${stor}"; }
declare cound_ok=$(mfind -u ${MANTA_URL} -t o -n ${name} -s 50 ${stor} | wc -l)
[[ ${cound_ok} -eq 1 ]] || { fail "mfind failed: mfind -u ${MANTA_URL} -t o -n ${name} -s 50 ${stor}"; }
ok "mfind by size"

## 5. by max depth
folder=$(random_string)
mmkdir -u ${MANTA_URL} ${stor}/${folder}/
[[ $? -eq 0 ]] || { fail "mmkdir -u ${MANTA_URL} /${user}/stor/${folder}"; }
file=$(random_file 100)
name=$(basename ${file})
mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}/${folder}/${name}
[[ $? -eq 0 ]] || { fail "mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}/${folder}/${name}"; }

declare -i max_depth_count=$(mfind -u ${MANTA_URL} -t o -n ${name} --maxdepth 1 ${stor} | wc -l)
[[ $? -eq 0 ]] || { fail "exit fail for mfind -u ${MANTA_URL} -t o -n ${name} --maxdepth 1 ${stor} | wc -l)"; }
[[ ${max_depth_count} -eq 0 ]] || { fail "result count fail for mfind -u ${MANTA_URL} -t o -n ${name} --maxdepth 1 ${stor} | wc -l)"; }
ok "mfind my max depth"

declare -i max_depth_count=$(mfind -u ${MANTA_URL} -t o -n ${name} --maxdepth 2 ${stor} | wc -l)
[[ $? -eq 0 ]] || { fail "exit fail for mfind -u ${MANTA_URL} -t o -n ${name} --maxdepth 2 ${stor} | wc -l)"; }
[[ ${max_depth_count} -eq 1 ]] || { fail "result count fail for mfind -u ${MANTA_URL} -t o -n ${name} --maxdepth 2 ${stor} | wc -l)"; }
ok "mfind my max depth"

## 6. by min depth
ok "mfind my min depth"
