/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include <stdarg.h>
/* Header for class net_techcable_accelerated_java_jni_Native */

#ifndef _Included_net_techcable_accelerated_java_jni_Native
#define _Included_net_techcable_accelerated_java_jni_Native
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_net_techcable_accelerated_1java_jni_Native_getNativeAddress0(JNIEnv *, jclass, jobject);

// "Constants" (initialized during JNI_OnLoad)

// Classes

extern jclass BYTE_BUFFER_CLASS;
// Exceptions
extern jclass CLASS_CAST_EXCEPTION;
extern jclass CLASS_NOT_FOUND_ERROR;
extern jclass ILLEGAL_ARGUMENT_EXCEPTION;
extern jclass ILLEGAL_STATE_EXCEPTION;
extern jclass INVALID_DATA_EXCEPTION;
extern jclass NULL_POINTER_EXCEPTION;
extern jclass OUT_OF_MEMORY_ERROR;
extern jclass RUNTIME_EXCEPTION;
extern jclass UNSUPPORTED_OPERATION_EXCEPTION;

// Helper functions

void findAndThrow(JNIEnv*, char*, char*);

inline void throw(JNIEnv* env, jclass class, char* message) {
    (*env)->ThrowNew(env, class, message);
}

void throwf(JNIEnv* env, jclass class, char* format, ...);

void vthrowf(JNIEnv* env, jclass class, char* format, va_list args);

int vasprintf(char** result, char* format, va_list args);

int asprintf(char** result, char* format, ...);

#ifdef __cplusplus
}
#endif
#endif
