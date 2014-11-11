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


function usage() {
    cat <<END >&2
USAGE: phase-runner.sh [-x] [-L number] [-c command] [-i input-pipe] [-o output-pipe] [-v verbose] [-p paralle-count] [-r retry-count]
        -c  command     # command
        -i  input       # input
        -o  outpu       # output
        -p  level       # parallel level (default is 1)
        -r  retry       # retry count (default is 0)
        -x              # use xargs
        -L  number      # xargs -L number
        -h|?            # usage
        -v              # verbose

eg,
     phase-runner.sh -c ls
END
    exit $1
}

declare opt_cmd
declare opt_inpp
declare opt_outp
declare verbose=''

declare -i opt_xargs=0
declare -i opt_lines=1
declare -i opt_par=1

declare name

while getopts "c:i:o:p:r:L:v:xh" name
do
    case ${name} in
        c) opt_cmd=${OPTARG};;
        i) opt_inpp=${OPTARG};;
        o) opt_outp=${OPTARG};;
        v) verbose=${OPTARG};;
        L) opt_lines=${OPTARG};;
        x) opt_xargs=1;;
        h|?) usage 0;;
        *) usage 1;;
    esac
done

[[ -z "$verbose" ]] || exec 2>>${verbose}


[ ! -z "$opt_cmd" ]  || { echo >&2 "command is required"; exit 2; }
[ ! -z "$opt_inpp" ] || { echo >&2 "input is required"; exit 2; }
[ ! -z "$opt_outp" ] || { echo >&2 "output is required"; exit 2; }

function on_exit() {
    #[ ! -p "$opt_inpp" ] || rm -f "$opt_inpp"
    echo >>${verbose} "[PHASE:$BASH_LINENO] on_exit. removing input: ${opt_inpp}"
    rm -f "$opt_inpp" 2>>${verbose} || echo >>${verbose} "[PHASE:$BASH_LINENO] unable to remove: $opt_inpp"
}

trap 'on_exit' EXIT

[ -e "$opt_inpp" ] || {
    echo >>${verbose} "[PHASE:$BASH_LINENO] input does not exist: $opt_inpp. creating one";
    mkfifo ${opt_inpp} || { echo >>${verbose} "[PHASE] mkfifo failed for: $opt_inpp. creating normal: ${opt_inpp}"; touch ${opt_inpp}; }
    [ -e "$opt_inpp" ] || { echo >>${verbose} "[PHASE] failed to mkfifo/touch input: ${opt_inpp}"; exit 3; }
}

#/usr/local/opt/coreutils/libexec/gnubin/dd if=${opt_inpp} iflag=nonblock of=/dev/null

#cat ${opt_inpp} >> ${opt_verb}
#cat /dev/null > ${opt_inpp}


exec <${opt_inpp}
exec 1>>${opt_outp}

#while read line; do
#    echo "input line: $line"
#done <${opt_inpp}

if [ ${opt_xargs} -ne 0 ]; then
    [ -z "$verbose" ] || echo "[PHASE] running: /usr/bin/xargs -L1 ${opt_cmd}. input: ${opt_inpp} output: ${opt_outp}" >>${verbose}
    /usr/bin/xargs -t -L1 ${opt_cmd} >>${opt_outp} 2>>${verbose}
else
    [ -z "$verbose" ] || echo "[PHASE] running: ${opt_cmd}" >>${verbose}
    ${opt_cmd}
fi

declare -r -i exitCode=$?
[ -z "$verbose" ] || echo "[PHASE] exit code: ${exitCode}" >>${verbose}

exit ${exitCode}
