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
test_count 1

##
file=$(random_file 100)
name=$(basename ${file})
mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}
[[ $? -eq 0 ]] || { fail "mmput failed: mput -c 1 -u ${MANTA_URL} -f ${file} ${stor}"; }
ln_name=link_${name}

mln -u ${MANTA_URL} ${stor}/${name} ${stor}/${ln_name}
[[ $? -eq 0 ]] || fail "mln -u ${MANTA_URL} ${stor}/${name} ${stor}/${ln_name}"

ok "mln ${stor}/${name} --> ${stor}/${ln_name}"
