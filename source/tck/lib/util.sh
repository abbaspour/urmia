#!/bin/bash

#declare -r mdb_user=uadmin
#mdb_db=udb01
#mdb_host=192.168.126.1

function pid_on_port() {
    echo $(lsof -n -i4TCP:$1 | tail -1 | awk '{print $2}');
}

function random_string() {
    len=$1
    [[ ! -z ${len} ]] || len=5;
    cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w ${len} | head -n 1
}

function random_file() {
    dir=/var/tmp/rf
    mkdir -p ${dir}
    size=$1
    [[ ! -z ${size} ]] || size=1k;
    rand=$(random_string)
    name=$$-${size}-${rand}.file
    path=${dir}/${name}
    dd if=/dev/random of=${path} bs=${size} count=1 2>/dev/null >/dev/null
    echo ${path}
}

function mdb_durability() {
    owner=$1
    parent=$2
    name=$3

    sql="select durability from objects where owner='${owner}' and ns='stor' and parent='${parent}' and name='${name}'"
    echo $(psql -t -A -h ${mdb_host} -U ${mdb_user} -d ${mdb_db} -c "$sql")
}


function mdb_etag() {
    owner=$1
    parent=$2
    name=$3

    sql="select etag from objects where owner='${owner}' and ns='stor' and parent='${parent}' and name='${name}'"
    echo $(psql -t -A -h ${mdb_host} -U ${mdb_user} -d ${mdb_db} -c "$sql")
}

function mdb_md5() {
    owner=$1
    parent=$2
    name=$3

    sql="select md5 from objects where owner='${owner}' and ns='stor' and parent='${parent}' and name='${name}'"
    echo $(psql -t -A -h ${mdb_host} -U ${mdb_user} -d ${mdb_db} -c "$sql")
}

function local_md5() {
    echo $(openssl dgst -md5 -binary $1 | openssl enc -base64)
}

function mdb_storage_count() {
    etag=$1

    sql="select count(*) from storage where etag='${etag}'"
    echo $(psql -t -A -h ${mdb_host} -U ${mdb_user} -d ${mdb_db} -c "$sql")
}

function node_count() {
    count=$(${CLI_ADMIN}/list-node.sh $@ -h $(hostname) | head -1 | awk '{print $NF}')
    echo ${count}
}

function node_add() {
    type=$1
    port=$2

    args="-t ${type} -h $(hostname) -p ${port}"
    [ $# == 2 ] || args+=" -u $3"

    ${CLI_ADMIN}/add-node.sh ${args}
}