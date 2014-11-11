#!/bin/sh

if [ $# -ne 1 ]; then
    echo "$0 <file>"
    exit 1
fi

## http://stackoverflow.com/questions/4583967/how-to-encode-md5-sum-into-base64-in-bash
openssl dgst -md5 -binary $1 | openssl enc -base64

