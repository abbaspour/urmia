/**
 *
 * Copyright 2014 by Amin Abbaspour
 *
 * This file is part of Urmia.io
 *
 * Urmia.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Urmia.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Urmia.io.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "io_urmia_dd_DirectDigest.h"
#include "md5.h"

jmethodID limitMethodId;
jfieldID limitFieldId;
jfieldID positionFieldId;
jclass bufferCls;

// http://normanmaurer.me/blog/2014/01/07/JNI-Performance-Welcome-to-the-dark-side/
// http://stackoverflow.com/questions/11329519/manipulation-of-bytebuffer-from-jni

// Is automatically called once the native code is loaded via System.loadLibary(...);
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    //printf("JNI_OnLoad\n");

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    } else {
        jclass cls = (*env)->FindClass(env, "java/nio/Buffer");
        // Get the id of the Buffer.limit() method.
        limitMethodId = (*env)->GetMethodID(env, cls, "limit", "()I");

        // Get int limit field of Buffer
        limitFieldId = (*env)->GetFieldID(env, cls, "limit", "I");
        positionFieldId = (*env)->GetFieldID(env, cls, "position", "I");

        jclass localBufferCls = (*env)->FindClass(env, "java/nio/ByteBuffer");
        bufferCls = (jclass) (*env)->NewGlobalRef(env, localBufferCls);
    }
    return JNI_VERSION_1_6;
}

// Is automatically called once the Classloader is destroyed
void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    //printf("JNI_OnUnload\n");

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        // Something is wrong but nothing we can do about this :(
        return;
    } else {
        // delete global references so the GC can collect them
        if (bufferCls != NULL) {
            (*env)->DeleteGlobalRef(env, bufferCls);
        }
    }
}


/*
 * Class:     io_urmia_dd_DirectDigest
 * Method:    md5_init
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_io_urmia_dd_DirectDigest_md5_1init
  (JNIEnv *env, jclass clz) {
    MD5_CTX* ctx = (MD5_CTX*) malloc(sizeof(MD5_CTX));
    MD5_Init(ctx);
    jlong ref = (long)ctx;
    //printf("MD5_Init (ctx: %lu)\n", ref);

    return ref;
}

/*
 * Class:     io_urmia_dd_DirectDigest
 * Method:    md5_update
 * Signature: (JLjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_io_urmia_dd_DirectDigest_md5_1update__JLjava_nio_ByteBuffer_2
  (JNIEnv *env, jclass clz, jlong ctxRef, jobject buffer) {

    MD5_CTX* ctx = (MD5_CTX*) ctxRef;

    unsigned char *data = (unsigned char*) (*env)->GetDirectBufferAddress(env, buffer);

    jint limit = (*env)->GetIntField(env, buffer, limitFieldId);

    MD5_Update(ctx, data, limit);
}

/*
 * Class:     io_urmia_dd_DirectDigest
 * Method:    md5_update
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_io_urmia_dd_DirectDigest_md5_1update__J_3B
  (JNIEnv *env, jclass clz, jlong ctxRef, jbyteArray array) {

    MD5_CTX* ctx = (MD5_CTX*) ctxRef;

    jbyte *data = (*env)->GetByteArrayElements(env, array, 0);

    jint limit = (*env)->GetArrayLength(env, array);

    MD5_Update(ctx, data, limit);

}

/*
 * Class:     io_urmia_dd_DirectDigest
 * Method:    md5_final
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_io_urmia_dd_DirectDigest_md5_1final
  (JNIEnv *env, jclass clz, jlong ctxRef) {

    MD5_CTX* ctx = (MD5_CTX*) ctxRef;

    unsigned char* result = (unsigned char*) malloc(16 * sizeof(unsigned char));

    //fprintf(stderr, "MD5_Final %lu\n", ctxRef);
    MD5_Final(result, ctx);
    //fprintf(stderr, "MD5_Final done %lu\n", ctxRef);

    jbyteArray arr = (*env)->NewByteArray(env, 16);
    //fprintf(stderr, "MD5_Final NewByteArray\n");

    jboolean isCopy;
    jbyte* data = (*env)->GetByteArrayElements(env, arr, &isCopy);
    //fprintf(stderr, "MD5_Final GetByteArrayElements\n");

    memcpy(data, result, 16);
    //fprintf(stderr, "MD5_Final memcpy\n");

    (*env)->ReleaseByteArrayElements(env, arr, data, 0);
    //fprintf(stderr, "MD5_Final ReleaseByteArrayElements\n");

    //fprintf(stderr, "MD5_Final free result\n");
    free(result);

    //fprintf(stderr, "MD5_Final free ctx\n");
    free(ctx);

    return arr;
}

