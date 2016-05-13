#include <zlib.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <errno.h>
#include "native.h"

char* getNativeAddress(JNIEnv* env, jobject buffer) {
    char* address = (*env)->GetDirectBufferAddress(env, buffer);
    if (address == NULL) {
        if (buffer == NULL) {
            throw(env, NULL_POINTER_EXCEPTION, "Null buffer");
        } else if ((*env)->IsInstanceOf(env, buffer, BYTE_BUFFER_CLASS)) {
            throw(env, CLASS_CAST_EXCEPTION, "Buffer isn't a ByteBuffer");
        } else {
            throw(env, UNSUPPORTED_OPERATION_EXCEPTION, "Direct access to ByteBuffer doesn't appear to be supported");
        }
        return NULL;
    }
    return address;
}

jlong JNICALL Java_net_techcable_accelerated_1java_jni_Native_getNativeAddress0(JNIEnv* env, jclass class, jobject buffer) {
    return (uintptr_t) getNativeAddress(env, buffer);
}

// Classes

jclass BYTE_BUFFER_CLASS = NULL;
// Exceptions
jclass CLASS_CAST_EXCEPTION = NULL;
jclass CLASS_NOT_FOUND_ERROR = NULL;
jclass ILLEGAL_ARGUMENT_EXCEPTION = NULL;
jclass ILLEGAL_STATE_EXCEPTION = NULL;
jclass INVALID_DATA_EXCEPTION = NULL;
jclass NULL_POINTER_EXCEPTION = NULL;
jclass OUT_OF_MEMORY_ERROR = NULL;
jclass RUNTIME_EXCEPTION = NULL;
jclass UNSUPPORTED_OPERATION_EXCEPTION = NULL;

jclass** constantClasses = NULL;
uint32_t numConstantClasses = 0;

void createConstantClass(JNIEnv* env, jclass* variable, char* name) {
    jclass class = (*env)->FindClass(env, name);
    if (class == NULL) return; // Spec says we'll get a exception :D
    class = (*env)->NewGlobalRef(env, class); // Make it global
    if (class == NULL) {
        jclass outOfMemoryError = OUT_OF_MEMORY_ERROR == NULL ? (*env)->FindClass(env, "java/lang/OutOfMemoryError") : OUT_OF_MEMORY_ERROR;
        if (outOfMemoryError == NULL) return; // FindClass threw an error
        throw(env, OUT_OF_MEMORY_ERROR, "Unable to create global reference");
        return;
    }
    *variable = class;
    constantClasses = realloc(constantClasses, sizeof(jclass*) * ++numConstantClasses);
    constantClasses[numConstantClasses - 1] = variable;
}

jint JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env;
    (*jvm)->GetEnv(jvm, (void**)&env, JNI_VERSION_1_8);

    createConstantClass(env, &BYTE_BUFFER_CLASS, "java/nio/ByteBuffer");

    // Exceptions
    createConstantClass(env, &CLASS_CAST_EXCEPTION, "java/lang/ClassCastException");
    createConstantClass(env, &CLASS_NOT_FOUND_ERROR, "java/lang/NoClassDefFoundError");
    createConstantClass(env, &ILLEGAL_ARGUMENT_EXCEPTION, "java/lang/IllegalArgumentException");
    createConstantClass(env, &ILLEGAL_STATE_EXCEPTION, "java/lang/IllegalStateException");
    createConstantClass(env, &INVALID_DATA_EXCEPTION, "net/techcable/accelerated_java/InvalidDataException");
    createConstantClass(env, &NULL_POINTER_EXCEPTION, "java/lang/NullPointerException");
    createConstantClass(env, &OUT_OF_MEMORY_ERROR, "java/lang/OutOfMemoryError");
    createConstantClass(env, &RUNTIME_EXCEPTION, "java/lang/RuntimeException");
    createConstantClass(env, &UNSUPPORTED_OPERATION_EXCEPTION, "java/lang/UnsupportedOperationException");

    return JNI_VERSION_1_8;
}

void JNI_OnUnload(JavaVM* jvm, void* reserved) {
    JNIEnv *env;
    (*jvm)->GetEnv(jvm, (void**)&env, JNI_VERSION_1_8);

    for (int i = 0; i < numConstantClasses; i++) {
        jclass* constantClass = constantClasses[i];
        (*env)->DeleteGlobalRef(env, *constantClass);
        *constantClass = NULL; // Set the variable to null
    }
    free(constantClasses);
    numConstantClasses = 0;
}

void findAndThrow(JNIEnv* env, char* className, char* message) {
    jclass class = (*env)->FindClass(env, className);
    if (class == NULL) return; // JVM is expected to throw an error when a class isn't found
    (*env)->ThrowNew(env, class, message);
}

void throwf(JNIEnv* env, jclass class, char* format, ...) {
    va_list args;
    va_start(args, format);
    vthrowf(env, class, format, args);
    va_end(args);
}

void vthrowf(JNIEnv* env, jclass class, char* format, va_list args) {
    char* message = NULL;
    int result = vasprintf(&message, format, args);
    if (result < 0 || message == NULL) {
        if (errno == ENOMEM) {
            (*env)->ThrowNew(env, OUT_OF_MEMORY_ERROR, "Couldn't allocate memory for vasprintf");
        } else {
            (*env)->ThrowNew(env, class, "(Unable to format exception message)");
        }
    } else {
        (*env)->ThrowNew(env, class, message);
        free(message);
    }
}

int vasprintf(char** result, char* format, va_list args) {
    if (result == NULL) return -1;
    if (format == NULL) return -2;
    // Try to

    va_list args_copy;
    va_copy(args_copy, args);
    size_t size;
    char* buf;
    #ifdef _WIN32
    int needed = _vscprintf(format, args); // Windows-only function to get needed args
    size = needed;
    buf = malloc(size + 1);
    if (buf == NULL) return -3;
    #elif __STDC_VERSION__ >= 199901L
    size = 2048; // A reasonable temp buffer that should fit almost all format chars
    buf = malloc(size + 1); // Allocate an extra char for the null terminator
    if (buf == NULL) return -3;

    // C99 compliant 'vasprintf' implementations return the number of needed chars, even on failure
    // We can check
    int needed = vsnprintf(buf, size, format, args);

    if (needed < 0) { // Some other failure
        va_end(args_copy);
        free(buf);
        *result = NULL;
        return needed; // Error
    }

    size_t oldSize = size;
    buf = realloc(buf, (size_t) (needed + 1)); // Trim or expand
    size = (size_t) needed;
    if (buf == NULL) return -3;
    if (needed <= oldSize) { // It fit in our buffer
        va_end(args_copy);
        buf[size] = '\0';
        *result = buf;
        return needed; // How many
    }

    #else
    // We have no way to determine how many chars are needed :(
    #error C99 compliance or windows extensions are needed for vasprintf() support
    #endif

    needed = vsnprintf(buf, size, format, args_copy);
    va_end(args_copy);
    if (needed < 0) { // Error
        free(buf);
        *result = NULL;
        return needed;
    } else { // Success
        buf[size] = '\0';
        *result = buf;
        return needed;
    }
}

int asprintf(char** result, char* format, ...) {
    va_list args;
    va_start(args, format);
    int code = vasprintf(result, format, args);
    va_end(args);
    return code;
}
