#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

declare -r user='tck'
export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=${user}
export MANTA_KEY_ID=$(ssh-keygen -l -f $HOME/.ssh/id_rsa.pub | awk '{print $2}')

. ${ROOT}/../lib/tap.sh
. ${ROOT}/../lib/util.sh

declare -r stor=/${user}/stor

## setup
test_count 2

## put a file and get it immidicately
file=$(random_file)
base=$(basename ${file})
mput -q -f ${file} ${stor}
[[ $? -eq 0 ]] || fail "unable to mput -f $file"

output=$(random_file 0)
mget -q -o ${output} ${stor}/${base}
[[ $? -eq 0 ]] || fail "unable to mget ${stor}/${base} -> $output"

diff ${file} ${output} 2>/dev/null 1>/dev/null
[[ $? -eq 0 ]] || fail "mput/mget files differ $file vs $output"
ok "mget success $file -> $output"


## -c 1 and get several times
file=$(random_file)
base=$(basename ${file})
mput -c 1 -q -f ${file} ${stor}
[[ $? -eq 0 ]] || fail "unable to mput -f $file"

output=$(random_file 0)

for i in `seq 1 1`; do
    mget -q -o ${output} ${stor}/${base}
    [[ $? -eq 0 ]] || fail "unable to mget ${stor}/${base} -> $output"
    diff ${file} ${output} 2>/dev/null 1>/dev/null
    [[ $? -eq 0 ]] || fail "mput/mget files differ $file vs $output"
done
ok "mget success of durability 1 $file -> $output"
