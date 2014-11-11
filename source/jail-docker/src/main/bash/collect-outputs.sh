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


# this scripts downloads the outputs of map jobs into a single box to perform reduce
# input is a URL.
# runner contructs the URL by finding storage node and appending http/port
# xarsg of job-runner causes automatic pipe functionality
# output is the result file name
#   wget output if remore
#   input file name if not remote

declare -r -i count=$#

[ ${count} -le 0 ] && exit 0

declare -r url=$1

if [[ ${url} == http://* ]]; then
    declare name=$(echo ${url} | awk -F'/' '{print $NF}')
    wget -q -O - ${url}
    #wget -q -O ${name} ${url}
    #echo ${name}
else
    cat ${url}
    #echo ${url}
fi

