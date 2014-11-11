#!/bin/bash

declare -r ROOT=$(dirname "${BASH_SOURCE[0]}")

. ${ROOT}/../lib/tap.sh

## setup
test_count 1

##
ok "empty test"
