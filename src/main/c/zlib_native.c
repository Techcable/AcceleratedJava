#include <stdlib.h>
#include <stdint.h>
#include <zlib.h>
#include "native.h"
#include "zlib_native.h"

jlong JNICALL Java_net_techcable_accelerated_1java_jni_ZLibNative_createStream0(JNIEnv* env, jclass type, jint level, jint typeId) {
    z_stream* stream = malloc(sizeof(z_stream));
    stream->zalloc = Z_NULL;
    stream->zfree = Z_NULL;
    stream->opaque = Z_NULL;
    int ret;
    switch (typeId) {
        case DEFLATE_TYPE_ID: // Deflate
            ret = deflateInit(stream, level);
            break;
        case INFLATE_TYPE_ID: // Inflate
            if (level != -1) {
                throw(env, ILLEGAL_ARGUMENT_EXCEPTION, "Level must be always be -1 for inflating");
                return -1;
            }
            ret = inflateInit(stream);
            break;
        default:
            throwf(env, ILLEGAL_ARGUMENT_EXCEPTION, "Invalid stream type with id %d", typeId);
            return -1;
    }
    if (ret != Z_OK) {
        switch (ret) {
            case Z_MEM_ERROR:
                throw(env, OUT_OF_MEMORY_ERROR, "Not enough memory to initalize stream");
                break;
            case Z_STREAM_ERROR:
                throwf(env, ILLEGAL_ARGUMENT_EXCEPTION, "Invalid stream level %d", level);
                break;
            default:
                throwf(env, RUNTIME_EXCEPTION, "Unknown error code %d", ret);
                break;
        }
        return -1;
    }
    return (jlong) (uintptr_t) stream;
}

void prepareStream(z_stream* stream, jlong inAddress, jint inLength, jlong outAddress, jint outLength) {
    unsigned char* in = (unsigned char*) (uintptr_t) inAddress;
    unsigned char* out = (unsigned char*) (uintptr_t) outAddress;

    stream->next_in =  in;
    stream->avail_in = (uInt) inLength;

    stream->next_out = out;
    stream->avail_out = (uInt) outLength;
}

jint handleErrors(JNIEnv* env, z_stream* stream, jint inLength, int outLength, jint* communicationArray, int code) {
    int consumed = inLength - stream->avail_in;
    int written = outLength - stream->avail_out;

    communicationArray[0] = consumed;
    communicationArray[1] = written;

    if (code == Z_OK) {
        return 0;
    } else if (code == Z_STREAM_END) {
        return 1;
    } else {
        char* msg = stream->msg == NULL ? "unknown" : stream->msg;
        switch (code) {
            case Z_STREAM_END:
                return 1;
            case Z_DATA_ERROR:
                throwf(env, INVALID_DATA_EXCEPTION, "Invalid input data: %s)", msg);
                break;
            case Z_BUF_ERROR:
                throw(env, ILLEGAL_STATE_EXCEPTION, "Buffer is empty/no progress possible");
                break;
            case Z_STREAM_ERROR:
                throwf(env, ILLEGAL_STATE_EXCEPTION, "Internal error! Stream in invalid state: %s", msg);
                break;
            case Z_MEM_ERROR:
                throw(env, OUT_OF_MEMORY_ERROR, "Zlib ran out of memory");
                break;
            default:
                throwf(env, RUNTIME_EXCEPTION, "Unknown error code %d: %s", code, msg);
                break;
        }
        return -1;
    }
}

jint JNICALL Java_net_techcable_accelerated_1java_jni_ZLibNative_inflate0(JNIEnv* env, jclass class, jlong streamAddress, jlong communicationAddress, jlong inAddress, jint inLength, jlong outAddress, jint outLength) {
    z_stream* stream = (z_stream*) (uintptr_t) streamAddress;
    jint* communicationArray = (jint*) (uintptr_t) communicationAddress;
    prepareStream(stream, inAddress, inLength, outAddress, outLength);

    int code = inflate(stream, Z_SYNC_FLUSH);

    return handleErrors(env, stream, inLength, outLength, communicationArray, code);
}

jint JNICALL Java_net_techcable_accelerated_1java_jni_ZLibNative_deflate0(JNIEnv* env, jclass class, jlong streamAddress, jlong communicationAddress, jlong inAddress, jint inLength, jlong outAddress, jint outLength, jboolean finish) {
    z_stream* stream = (z_stream*) (uintptr_t) streamAddress;
    jint* communicationArray = (jint*) (uintptr_t) communicationAddress;
    prepareStream(stream, inAddress, inLength, outAddress, outLength);

    int code = deflate(stream, finish ? Z_FINISH : Z_SYNC_FLUSH);

    return handleErrors(env, stream, inLength, outLength, communicationArray, code);
}

void JNICALL Java_net_techcable_accelerated_1java_jni_ZLibNative_free0(JNIEnv* env, jclass class, jlong streamAddress, jint typeId) {
    z_stream* stream = (z_stream*) (uintptr_t) streamAddress;
    int code;
    switch (typeId) {
        case DEFLATE_TYPE_ID: // Deflate
            code = deflateEnd(stream);
            break;
        case INFLATE_TYPE_ID: // Inflate
            code = inflateEnd(stream);
            break;
        default:
            throwf(env, ILLEGAL_ARGUMENT_EXCEPTION, "Invalid stream type with id %d", typeId);
            return;
    }
    char* msg = stream->msg == NULL ? "unknown" : stream->msg;
    free(stream);
    switch (code) {
        case Z_OK:
            break; // Happy day :D
        case Z_STREAM_ERROR:
            throwf(env, ILLEGAL_STATE_EXCEPTION, "Zlib stream in bad state: %s", msg);
            break;
        default:
            throwf(env, RUNTIME_EXCEPTION, "Unknown error code %d: (%s)", code, msg);
            break;
    }
}
