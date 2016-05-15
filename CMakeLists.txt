cmake_minimum_required(VERSION 3.5)
project(AcceleratedJava)
if (NOT CMAKE_CROSSCOMPILING)
    include(FindJNI)
else ()
    if (NOT CMAKE_SYSTEM_NAME STREQUAL "Windows")
        message(FATAL_ERROR "Can't crosscompile to ${CMAKE_SYSTEM_NAME}, since we don't have the jni headers for it!")
    endif()
    # WTF do we outut 'libacceleratedJava.dll'?
    set(CMAKE_SHARED_MODULE_PREFIX "")
    # We have the windows jni include files in the project
    set(JNI_INCLUDE_DIRS "includes/jni/;includes/jni/win32")
    set(JNI_FOUND TRUE)
endif ()

if (NOT JNI_FOUND)
    message(FATAL_ERROR JNI not found)
endif ()

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror -pedantic")

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "natives")

set(SOURCE_FILES
        src/main/c/native.c
        src/main/c/native.h
        src/main/c/zlib_native.c
        src/main/c/zlib_native.h)

add_library(acceleratedJava MODULE ${SOURCE_FILES})

#function(target_link_static_library TARGET LIBRARY_NAME)
#    if (WIN32)
#        set(STATIC_LIBRARY_SUFFIX ".lib")
#    else ()
#        set(STATIC_LIBRARY_SUFFIX ".a")
#    endif ()
#    target_link_libraries(${TARGET} ${LIBRARY_NAME}${STATIC_LIBRARY_SUFFIX})
#endfunction(target_link_static_library)

target_link_libraries(acceleratedJava z)
target_include_directories(acceleratedJava PUBLIC ${JNI_INCLUDE_DIRS})
