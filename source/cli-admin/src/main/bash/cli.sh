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

VER=0.1.0

LOG=${ROOT}/../var/log
LIB=${ROOT}/../lib
[[ -e ${LIB} ]] || LIB=${ROOT}/../../../target/urmia/cli-admin/lib

JAR=${LIB}/cli-admin-${VER}.jar

[[ -d ${LIB} ]] || { echo >&2 "lib folder file not found: $JAR"; exit 2; }
[[ -e ${JAR} ]] || { echo >&2 "main jar file not found: $JAR"; exit 2; }

mkdir -p ${LOG}

JAVA=`which java`
[[ $? -eq 0 ]] || JAVA=/opt/local/java/openjdk7/bin/java

JAVA_ARGS="-client"

${JAVA} ${JAVA_ARGS} -jar ${JAR} $@
