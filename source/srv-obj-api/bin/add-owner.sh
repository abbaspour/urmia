#!/bin/bash

## todo: move this to cli-admin

opt_host=localhost
opt_port=5432
opt_user=uadmin
opt_db=udb01

opt_owner=''
opt_key=''
opt_drop=0

verbose=0

while getopts h:p:u:o:d:k:vD name
do
    case $name in
        h) opt_host=$OPTARG;;
        p) opt_port=$OPTARG;;
        u) opt_user=$OPTARG;;
        o) opt_owner=$OPTARG;;
        k) opt_key=$OPTARG;;
        d) opt_db=$OPTARG;;
        D) opt_drop=1;;
        v) verbose=1;; # set -x;;
        ?|*) cat <<END >&2
USAGE: add-owner.sh [-h db-host] [-p port] [-u db-user] -o owner [-k ssh-key-fingerprint] [-D]
            -h          # ODB host
            -p          # ODB port
            -u          # ODB user
            -o          # owner name
            -k          # owner ssh-key fingerprint
            -D          # drop user
            -v          # verbose mode
            -?          # display usage help
eg,
     add-owner.sh -o tck -k '00:00:00:2b:58:02:02:64:dd:41:38:94:0b:0a:41:06'
END
            exit 3
    esac
done

## requirments
which psql >/dev/null || { echo >&2 "psql not in path" ; exit 2; }

[[ ! -z $opt_owner ]] || { echo >&2 "owner is required"; exit 1; }

if [ $opt_drop -eq 1 ]; then
    sql_owners="delete from objects where owner='$opt_owner'"
    sql_objroot="delete from owners where username='$opt_owner'"
else
    [[ ! -z $opt_key ]] || { echo >&2 "fingerprint is required"; exit 1; }
    sql_owners="insert into owners(username, key_id) values ('$opt_owner', '$opt_key')"
    sql_objroot="insert into objects VALUES ('$opt_owner', 'stor', '/', '', 'directory', 0, now(), '1B2M2Y8AsgTpgAmY7PhCfg==', '34a00783-78ba-caef-b441-97fde435d157', 2, false)"
fi

[[ $verbose -eq 0 ]] || {
    echo "odb    : $opt_user@$opt_host:$opt_port"
    echo "owner  : $opt_owner"
    echo "owners : $sql_owners"
    echo "objects: $sql_objroot"
}

psql -U $opt_user -h $opt_host -p $opt_port -d $opt_db -c "$sql_owners"

psql -U $opt_user -h $opt_host -p $opt_port -d $opt_db -c "$sql_objroot"

