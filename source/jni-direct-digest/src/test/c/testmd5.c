#include <stdlib.h>
#include <stdio.h>
#include "md5.h"

int main(int argc, char** argv) {
    MD5_CTX* ctx = (MD5_CTX*) malloc(sizeof(MD5_CTX));

    int len = 2;
    char* data = (char*) malloc(sizeof(char));
    unsigned char* result = (unsigned char*) malloc(16 * sizeof(unsigned char));

    data[0] = 'A';
    data[1] = 'B';

    MD5_Init(ctx);
    MD5_Update(ctx, data, len);
    MD5_Final(result, ctx);

    printf("result: ");
    for(int i = 0; i < 16; i++) {
        printf("%c", result[i]);
    }

    return 0;
}