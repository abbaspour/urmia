#!/bin/bash

#echo "iscript: $@"

[[ $# -eq 2 ]] || { echo >&2 "iscipt not invoked correctly. args: $@"; exit 1; }

pkg=$1
phase=$2

if [ "x$phase" == "xPRE-INSTALL" ]; then
    #echo "pre-installing: $pkg"

    if test `egrep -c "^urmia:" /etc/group` -eq 0 ; then
        echo "creating urmia group"
        groupadd -g 1359 urmia
    fi

    if test `egrep -c "^urmia:" /etc/passwd` -eq 0 ; then
        echo "creating urmia user"
        useradd -u 1359 -g urmia -d /nonexistent -s /usr/bin/false urmia
    fi

    ## todo: enable cron for urmia user (cron gc)
elif [ "x$phase" == "xPOST-INSTALL" ]; then
    #echo "post-installing: $pkg"

    echo "making var folders"
    mkdir -p /opt/urmia/srv-obj-api/var/run
    mkdir -p /opt/urmia/srv-obj-api/var/log

    chown -R urmia:urmia /opt/urmia/srv-obj-api/var

    echo "installing SMF service"
    /usr/sbin/svccfg import /opt/urmia/srv-obj-api/svc/urmia-api-rest.xml

else
    echo >&2 "unknown phase: $phase"
    exit 1
fi

exit 0