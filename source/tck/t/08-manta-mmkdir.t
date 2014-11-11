#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

declare -r user='tck'
export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=${user}
export MANTA_KEY_ID=$(ssh-keygen -l -f $HOME/.ssh/id_rsa.pub | awk '{print $2}')

. ${ROOT}/../lib/tap.sh
. ${ROOT}/../lib/util.sh

## setup
test_count 7

## create
folder=$(random_string)
mmkdir -u ${MANTA_URL} /${user}/stor/${folder}
if [ $? -eq 0 ]; then
    ok "mmkdir successful for /${user}/stor/${folder}"
else
    fail "failed to mmkdir -u ${MANTA_URL} /${user}/stor/${folder}. exit code: $?";
fi

## list it
mls -u ${MANTA_URL} /${user}/stor/${folder}
if [ $? -eq 0 ]; then
    ok "mmkdir then mls worked for /${user}/stor/${folder}"
else
    fail "failed to mls -u ${MANTA_URL} /${user}/stor/${folder}. exit code: $?"
fi

## remove after create
mrmdir -u ${MANTA_URL} /${user}/stor/${folder}
if [ $? -eq 0 ]; then
    ok "mrmdir worked for /${user}/stor/${folder}"
else
    fail "failed to mrmdir -u ${MANTA_URL} /${user}/stor/${folder} after mmkdir. exit code: $?"
 fi

## create and upload files into folder
folder=$(random_string)
mmkdir -u ${MANTA_URL} /${user}/stor/${folder}

[[ $? -eq 0 ]] || { fail "failed to mmkdir -u ${MANTA_URL} /${user}/stor/${folder}. exit code: $?"; }
file=$(random_file)
name=$(basename ${file})
mput -u ${MANTA_URL}  -f ${file} /${user}/stor/${folder}
[[ $? -eq 0 ]] || { fail "failed to mput into /${user}/stor/${folder}. exit code: $?"; }
ok "mput $file into folder /${user}/stor/${folder}"

## try to remove unempty folder
output=$(mrmdir -u ${MANTA_URL} /${user}/stor/${folder} 2>&1)
[[ $? -ne 0 ]] || { fail "removed unempty folder: /${user}/stor/${folder}"; }
err_code=$(echo ${output} | awk -F: '{print $2}')
err_msg=$(echo ${output} | awk -F: '{print $3}')
if [ "x$err_code" == "x DirectoryNotEmptyError" ] && [ "x$err_msg" == "x /$user/stor/${folder} is not empty" ]; then
    ok "rejected mrmdir on unempty folder: /${user}/stor/${folder}"
else
    fail "rejected mrmdir on unempty folder: /${user}/stor/${folder}. code: $err_code, msg: $err_msg"
fi

## remove the file from folder
mrm -u ${MANTA_URL} /${user}/stor/${folder}/${name}
[[ $? -eq 0 ]] || { fail "unable to mrm newly uploaded file: /${user}/stor/${folder}/${name}"; }
ok "mrm newly uploaded file: /${user}/stor/${folder}/${name}"

## mrmdir empty folder now
mrmdir -u ${MANTA_URL} /${user}/stor/${folder}
[[ $? -eq 0 ]] || { fail "failed to mrmdir -u ${MANTA_URL} /${user}/stor/${folder}. exit code: $?"; }
ok "mrmdir -u ${MANTA_URL} /${user}/stor/${folder}"
