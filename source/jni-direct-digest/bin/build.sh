#!/bin/sh

cc -Wall -c -I${JAVA_HOME}/include  -I${JAVA_HOME}/include/darwin io_urmia_dd_DirectDigest.c
cc -Wall -c -I/usr/include md5.c

cc -dynamiclib -o libdirectdigest.jnilib io_urmia_dd_DirectDigest.o md5.o -framework JavaVM

cc testmd5.c md5.o -o testmd5