#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

declare -r user='tck'
export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=${user}
export MANTA_KEY_ID=$(ssh-keygen -l -f $HOME/.ssh/id_rsa.pub | awk '{print $2}')

. ${ROOT}/../lib/tap.sh
. ${ROOT}/../lib/util.sh

## setup
test_count 1

## put a file and should apper in list (1k)
f1k=$(random_file 1k)
mput -q -f ${f1k} /${user}/stor
f1kb=`basename ${f1k}`
f1kb_md5=$(mmd5 -u ${MANTA_URL} /${user}/stor/${f1kb} | awk '{print $1}')
local_md5=$(md5sum ${f1k} | awk '{print $1}')

if [ "$f1kb_md5" == "$local_md5" ]; then
    ok "mmd5 -u ${MANTA_URL} /$user/stor/${f1kb} equal to local file ($local_md5)"
else
    fail "md5s don't match remote ($f1kb_md5) != local ($local_md5)"
fi
