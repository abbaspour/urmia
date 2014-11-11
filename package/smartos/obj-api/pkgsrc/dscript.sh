#!/bin/bash

#echo "dscript: $@"

[[ $# -eq 2 ]] || { echo >&2 "dscipt not invoked correctly. arg: $@"; exit 1; }

pkg=$1
phase=$2

if [ "x$phase" == "xDEINSTALL" ]; then
    #echo "pre-removing: $pkg"
    echo "disabling service: svc:/network/urmia/api:default"
    /usr/sbin/svcadm disable -s svc:/network/urmia/api:default
elif [ "x$phase" == "xPOST-DEINSTALL" ]; then
    #echo "post-removing: $pkg"
    echo "deleting service: svc:/network/urmia/api:default"
    /usr/sbin/svccfg delete svc:/network/urmia/api:default
else
    echo >&2 "unknown phase: $phase"
    exit 1
fi

exit 0