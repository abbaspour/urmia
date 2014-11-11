#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

. ${ROOT}/../lib/tap.sh

declare -r user='tck'
declare -r JOB_URL="http://`hostname`:8086"
export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=${user}
export MANTA_KEY_ID=$(ssh-keygen -l -f $HOME/.ssh/id_rsa.pub | awk '{print $2}')

. ${ROOT}/../lib/tap.sh
. ${ROOT}/../lib/util.sh

stor=/${user}/stor

## setup
test_count 7

## mput then mjob
file=$(random_file)
name=$(basename ${file})
mput -c 1 -u ${MANTA_URL} -f ${file} ${stor}
jobId=$(echo "$stor/${name}" | mjob create -q -a ${user} -u ${JOB_URL} -m "ls -1" | tail -1)
[[ -z ${jobId} ]] && fail "no jobId in result of create"
ok "mjob created . file: $stor/${name}, job: mjob create -a ${user} -u ${JOB_URL} -m \"ls -1\", id: ${jobId}"

## mjob outputs
outputs=$(mjob outputs -a ${user} -u ${JOB_URL} ${jobId})
[[ -z ${outputs} ]] && fail "no outputs for job: ${jobId}"
ok "mjob outputs ${jobId} -> ${outputs}"

## mget result (should be file name)
content=$(mget -u ${JOB_URL} ${outputs})
[[ -z ${content} ]] && fail "mget of job result failed. is empty"
[[ ${content} != */${name} ]] && fail "results is incorrect for ls -1"
ok "result of 'ls -1' is expected: $content"

## multiple map phase
file=$(random_file 100)
name=$(basename ${file})
mput -c 1 -u ${MANTA_URL} -f ${file} ${stor}

jobId=$(echo "$stor/${name}" | mjob create -q -a ${user} -u ${JOB_URL} -m "cat" -m "wc -c" | tail -1)
[[ -z ${jobId} ]] && fail "no jobId in result of create"

outputs=$(mjob outputs -a ${user} -u ${JOB_URL} ${jobId})
[[ -z ${outputs} ]] && fail "no outputs for job: ${jobId}"

content=$(mget -u ${JOB_URL} ${outputs} | tr -d ' ')
[[ -z ${content} ]] && fail "mget of job result failed. is empty"

[[ ${content} != "100" ]] && fail "expected 100, received: ${content}"
ok "mjob with 2 map successfull: ${content}"

## multiple map phase
file=$(random_file)
cat >${file}<<EOL
some random text here
find me 1
find me 2
find me 3
EOL

name=$(basename ${file})
mput -c 1 -u ${MANTA_URL} -f ${file} ${stor}

jobId=$(echo "$stor/${name}" | mjob create -q -a ${user} -u ${JOB_URL} -m "cat" -m "grep -c find" | tail -1)
[[ -z ${jobId} ]] && fail "no jobId in result of create"

outputs=$(mjob outputs -a ${user} -u ${JOB_URL} ${jobId})
[[ -z ${outputs} ]] && fail "no outputs for job: ${jobId}"

content=$(mget -u ${JOB_URL} ${outputs} | tr -d ' ')
[[ -z ${content} ]] && fail "mget of job result failed for id: ${jobId} is empty. input: $file"

[[ ${content} != "3" ]] && fail "expected 100, received: ${content}"
ok "mjob cat+grep successfull: ${content}"

## multiple map phase pipe
jobId=$(echo "$stor/${name}" | mjob create -q -a ${user} -u ${JOB_URL} -m "cat | grep -c find" | tail -1)
[[ -z ${jobId} ]] && fail "no jobId in result of create"

outputs=$(mjob outputs -a ${user} -u ${JOB_URL} ${jobId})
[[ -z ${outputs} ]] && fail "no outputs for job: ${jobId}"

content=$(mget -u ${JOB_URL} ${outputs} | tr -d ' ')
[[ -z ${content} ]] && fail "mget of job result failed for id: ${jobId} is empty. input: $file"

[[ ${content} != "3" ]] && fail "expected 100, received: ${content}"
ok "mjob cat+grep successfull: ${content}"

## mjob with reduce
file1=$(random_file 100)
name1=$(basename ${file1})
mput -c 1 -u ${MANTA_URL} -f ${file1} ${stor}

file2=$(random_file 100)
name2=$(basename ${file2})
mput -c 1 -u ${MANTA_URL} -f ${file2} ${stor}

jobId=$(echo -e "$stor/${name1}\n${stor}/${name2}" | mjob create -q -a ${user} -u ${JOB_URL} -m "cat" -r "wc -c" | tail -1)
[[ -z ${jobId} ]] && fail "no jobId in result of create"

sleep 1

outputs=$(mjob outputs -a ${user} -u ${JOB_URL} ${jobId})
[[ -z ${outputs} ]] && fail "no outputs for job: ${jobId}"

content=$(mget -u ${JOB_URL} ${outputs}| tr -d ' ')
[[ $? -eq 0 ]] || fail "failed mget -u ${JOB_URL} ${outputs} -> $content"
[[ -z ${content} ]] && fail "mget of job result failed for id: ${jobId} is empty. input: $file"

[[ ${content} != "200" ]] && fail "expected 200, received: ${content}"
ok "mjob reduce cat+wc successfull: ${content}"
