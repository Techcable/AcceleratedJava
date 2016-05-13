cmake_minimum_required(VERSION 3.5)
project(AcceleratedJava)
include(FindJNI)


if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    set(SYSTEM linux)
elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    set(SYSTEM windows)
elseif (CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    set(SYSTEM osx)
else ()
    message(FATAL_ERROR "Unknown system ${CMAKE_SYSTEM_NAME}")
endif ()

# Determine architecture
if (WIN32)
    execute_process(COMMAND wmic os get osarchitecture OUTPUT_VARIABLE RAW_ARCH OUTPUT_STRIP_TRAILING_WHITESPACE)
    if ("${RAW_ARCH}" STREQUAL "64-bit")
        set(ARCH "x86_64")
    elseif ("${RAW_ARCH}" STREQUAL "32-bit")
        set(ARCH "x86")
    else ()
        message(FATAL_ERROR "Unknown architecture ${RAW_ARCH}")
    endif ()
elseif (UNIX)
    # Future hint: Whitespace (including newlines!) is considered in string equality
    execute_process(COMMAND uname -m OUTPUT_VARIABLE RAW_ARCH OUTPUT_STRIP_TRAILING_WHITESPACE)
    if (RAW_ARCH STREQUAL "x86_64")
        set(ARCH "x86_64")
    elseif (RAW_ARCH STREQUAL "i686" OR RAW_ARCH STREQUAL "i386")
        set(ARCH "x86")
    else ()
        message(FATAL_ERROR "Unknown architecture ${RAW_ARCH}")
    endif ()
else ()
    message(FATAL_ERROR Unable to determine architecture for system ${CMAKE_SYSTEM_NAME})
endif ()

if (ARCH STREQUAL "x86_64")
    set(SYSTEM_ID "${SYSTEM}_64")
elseif (ARCH STREQUAL "x86")
    set(SYSTEM_ID "${SYSTEM}_32")
else ()
    message(FATAL_ERROR "Unable to determine system id for (known) architecture ${ARCH}")
endif ()

if (NOT JNI_FOUND)
    message(FATAL_ERROR JNI not found)
endif ()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror -std=c11 -pedantic")

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
