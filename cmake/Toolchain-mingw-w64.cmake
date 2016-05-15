set(CMAKE_SYSTEM_PROCESSOR "${CMAKE_HOST_SYSTEM_PROCESSOR}")

#Determine architecture
if (CMAKE_SYSTEM_PROCESSOR STREQUAL "x86_64")
  set(ARCH "x86_64")
elseif (CMAKE_SYSTEM_PROCESSOR STREQUAL "amd64")
  set(ARCH "x86_64")
elseif (CMAKE_SYSTEM_PROCESSOR STREQUAL "AMD64")
  # cmake reports AMD64 on Windows, but we might be building for 32-bit.
  if (CMAKE_CL_64)
    set(ARCH "x86_64")
  else()
    set(ARCH "x86")
  endif()
elseif (CMAKE_SYSTEM_PROCESSOR STREQUAL "x86")
  set(ARCH "x86")
elseif (CMAKE_SYSTEM_PROCESSOR STREQUAL "i386")
  set(ARCH "x86")
elseif (CMAKE_SYSTEM_PROCESSOR STREQUAL "i686")
  set(ARCH "x86")
else()
  message(FATAL_ERROR "Unknown processor: ${CMAKE_SYSTEM_PROCESSOR}")
endif()

# the name of the target operating system
set(CMAKE_SYSTEM_NAME Windows)
if (ARCH STREQUAL "x86_64")
    set(TOOLCHAIN_PREFIX "x86_64-w64-mingw32")
elseif (ARCH STREQUAL "x86")
    set(TOOLCHAIN_PREFIX "i686-w64-mingw32")
else ()
    message(FATAL_ERROR "Unable to determine toolchain prefix for ${ARCH}")
endif ()

# cross compilers to use for C and C++
set(CMAKE_C_COMPILER ${TOOLCHAIN_PREFIX}-gcc)
set(CMAKE_CXX_COMPILER ${TOOLCHAIN_PREFIX}-g++)
set(CMAKE_RC_COMPILER ${TOOLCHAIN_PREFIX}-windres)


# target environment on the build host system
#   set 1st to dir with the cross compiler's C/C++ headers/libs
set(CMAKE_FIND_ROOT_PATH "/usr/${TOOLCHAIN_PREFIX};")

# adjust the default behaviour of the FIND_XXX() commands:
# search headers and libraries in the target environment, search
# programs in the host environment
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
