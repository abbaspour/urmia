#!/bin/bash

## includes
declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

. ${ROOT}/../etc/tck.conf
. ${ROOT}/../lib/tap.sh

export MANTA_URL="http://`hostname`:8085"
export MANTA_USER=tck

## setup
test_count 4

## mls in path
mls_path=`which mls`
if [ -z ${mls_path} ]; then
    fail "mls not found"
else
    ok "mls at: $mls_path"
fi

## manta tools setup ok
mls --help 2>/dev/null 1>/dev/null
if [ $? -le 1 ]; then
    ok "mls working fine"
else
    fail "mls not working. exit code: $?"
fi

## $MANTA_URL defined
if [ -z ${MANTA_URL} ]; then
    fail "MANTA_URL not defined"
else
    ok "MANTA_URL defined: $MANTA_URL"
fi

## ODB exists
db=$(psql -l -t -A -h ${mdb_host} -U ${mdb_user} | grep -c ^${mdb_db})

if [ $db -eq 1 ]; then
    ok "DB exists: psql:$mdb_user/$mdb_db"
else
    fail "DB not exists"
fi

## storage nodes exists
#for o in `seq 1 $ods_count`; do
#    echo "checking ODB for storage: $o"
#    host=${ods_host[$o]}
#    port=${ods_port[$o]}
#    sql="select 1 from storage_nodes where hostname='$host' and port='$port'"
#    exists=$(psql -t -A -U $mdb_user -d $mdb_db -c "$sql")
#    [[ $exists -eq 1 ]] || { fail "unable to find ODS ($host:$port) in DB"; exit 4;}
#done
#
#ok "all storage nodes are defined in DB"


## clean up storge nodes
#for o in `seq 1 $ods_count`; do
#    echo "cleaning ODB for storage: $o"
#    host=${ods_host[$o]}
#    port=${ods_port[$o]}
#    sql="select location from storage_nodes where hostname='$host' and port='$port'"
#    location=$(psql -t -A -U $mdb_user -d $mdb_db  -c "$sql")
#    [[ ! -z $location ]] || { fail "unable to clean up ODS ($host:$port) in DB"; exit 4;}
#    sql="delete from storage where location='$location'"
#    psql -t -A -U $mdb_user -d $mdb_db  -c "$sql"
#    [[ $? -eq 0 ]] || { fail "unable to clean up ODS ($host:$port) in DB"; exit 4;}
#done
#
#ok "all storage nodes cleaned up in DB"

## todo: delete etags from objects
