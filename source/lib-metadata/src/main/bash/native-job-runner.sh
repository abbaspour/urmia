#!/bin/bash

#
# Copyright 2014 by Amin Abbaspour
#
# This file is part of Urmia.io
#
# Urmia.io is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Urmia.io is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Urmia.io.  If not, see <http://www.gnu.org/licenses/>.
#

ROOT=$(dirname "${BASH_SOURCE[0]}")

function usage() {
    cat <<END >&2
USAGE: job-runner.sh [-p pwd] [-u user] [-j id] [-c command1] [-c command2]
        -p  folder      # pwd for path
        -u  username    # username
        -c  command     # command(s)
        -h|?            # usage
        -v              # verbose

eg,
     phase-runner.sh -j 932990 -c "ls -1" -c wc
END
    exit $1
}

function remove_all_fifos() {
    [ -e "$1" ] || return
    find "$1" -type p -exec rm -f {} \; 2>/dev/null 1>/dev/null
    rmdir "$1" 2>/dev/null 1>/dev/null
}

#declare -r PARENT='/tmp/manta'

declare -a commands=()
declare dir=''
declare id=''
declare output=''

declare name

while getopts "p:u:j:c:o:vh" name
do
    case ${name} in
        p) parent=${OPTARG};;
        u) user=${OPTARG};;
        j) id=${OPTARG};;
        o) output=${OPTARG};;
        c) commands+=( "${OPTARG}" );;
        #v) verb=1;;
        h|?) usage 0;;
        *) usage 1;;
    esac
done

[ ! -z "$parent" ] || { echo >&2 "working folder not supplied"; exit 2; }
[ ! -z "$user" ] || { echo >&2 "user not supplied"; exit 2; }
[ ! -z "$id" ] || { echo >&2 "jobId not supplied"; exit 2; }

dir="${parent}/${user}/jobs/${id}"
mkdir -p "${dir}"

[ ! -z "$output" ] || output="$dir/output"

cat /dev/null > ${output}

declare -r verbose="$dir/verbose"

#trap 'remove_all_fifos $dir' EXIT SIGTERM SIGKILL SIGQUIT

first="$dir/input"
mkfifo ${first} 2>&1 1>/dev/null

prev=${first}

declare -r -i count=${#commands[@]}
declare -r -i last=count-1

declare -i pid
declare -i ec

declare -i phases_ok=1

for (( c = 0; c < count; c++ )); do
    cmd=${commands[$c]}

    if [ ${c} -eq ${last} ]; then
        next=${output}
    else
        next="$dir/$c"
        mkfifo "${next}" 2>>${verbose} 1>>${verbose}
    fi

    if [ ${c} -eq 0 ]; then args="-x"; else args=''; fi

    echo >>${verbose} -n "starting phase $c/$count -> ${ROOT}/phase-runner.sh ${args} -c \"${cmd}\" -i ${prev} -o ${next} -v ${verbose}" >>${verbose}

    ${ROOT}/phase-runner.sh ${args} -c "${cmd}" -i ${prev} -o ${next} -v ${verbose} 2>>${verbose} 1>>${verbose} &

    pid=$!
    ec=$?

    if [ ${ec} -eq 0 ]; then
        echo " ==> PID: $pid" >>${verbose}
    else
        echo " ==> Exit code: $ec" >>${verbose}
        phases_ok=0
        break
    fi

    prev=${next}
done

[ ${phases_ok} -eq 1 ] || { echo >>${verbose} "failed to start phase: $c"; exit ${ec}; }

echo >>${verbose} "started all phases. waiting for input"
echo "input  pipe: ${first}" >>${verbose}
echo "output file: ${output}" >>${verbose}

for job in `jobs -p`; do
    echo "waiting for ${job}"  >>${verbose}
    wait ${job} || let "FAIL+=1"
done