#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

declare -r user='tck'
export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=${user}
export MANTA_KEY_ID=$(ssh-keygen -l -f $HOME/.ssh/id_rsa.pub | awk '{print $2}')

. ${ROOT}/../etc/tck.conf
. ${ROOT}/../lib/tap.sh
. ${ROOT}/../lib/util.sh

stor=/${user}/stor

## setup
test_count 7

## simple file put
file=$(random_file)
name=$(basename ${file})
mput -q -u ${MANTA_URL} -f ${file} ${stor}
[[ $? -eq 0 ]] || { fail "mmput failed: mput -u ${MANTA_URL} -f ${file} ${stor}"; }
ok "simple mput success: mput -u ${MANTA_URL} -f ${file} ${stor}"

## uploaded file should come up in mls
mls_out=$(mls -l ${name})
[[ $? -eq 0 ]] || { fail "mls not showing up new file: $name"; }
ok "mls shows new file: $name"

## durability of default mput is 2
durability=$(mdb_durability ${user} '/' ${name})
[[ ${durability} -eq 2 ]] || fail "default durability of mput ($durability) is not 2"
ok "default durability is 2"

## md5 calculated correctly
md5=$(mdb_md5 ${user} '/' ${name})
[[ ! -z ${md5} ]] || fail "no md5 for mput object"
local_md5=$(local_md5 ${file})
[[ "x${md5}" == "x$local_md5" ]] || fail "local ($local_md5) and remote($md5) md5 sum not match"
ok "local ($local_md5) and remote($md5) md5 sum match"

## etag allocated and stored
etag=$(mdb_etag ${user} '/' ${name})
[[ ! -z ${etag} ]] || fail "no etag for mput object"
ok "has etag: $etag"

## entry count with this etag
st_count=$(mdb_storage_count ${etag})
[[ st_count -eq 2 ]] || fail "rows count in storage ($st_count) table do not match durability(2)"
ok "has storage row count: $st_count"


## with durability 1
file=$(random_file)
name=$(basename ${file})
mput -c 1 -q -u ${MANTA_URL} -f ${file} ${stor}
[[ $? -eq 0 ]] || { fail "mmput failed: mput -c 1 -u ${MANTA_URL} -f ${file} ${stor}"; }
durability=$(mdb_durability ${user} '/' ${name})
[[ ${durability} -eq 1 ]] || fail "durability of mput ($durability) is not 1"
ok "default durability is 1"

## todo: mime type is correct. use -j
## todo: mput should overwrite the same ODS
## todo: md5 of the file match
