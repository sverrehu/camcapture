#!/bin/bash

OBJ_OUTPUT_DIR="target/obj"
LIB_OUTPUT_DIR="src/main/resources/lib"
KERNEL="$(uname -s | tr A-Z a-z)"
SO_BASE_NAME="native-${KERNEL}"
INC="-I${JAVA_HOME}/include -I${JAVA_HOME}/include/${KERNEL} -I/opt/local/include -I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks/AVFoundation.framework/Versions/A/Headers"
SO="so"
if test "${KERNEL}" = "darwin"
then
  SO="dylib"
fi
mkdir -p "${OBJ_OUTPUT_DIR}" "${LIB_OUTPUT_DIR}"
MY_SOURCES="../../src/main/c/*.c ../../src/main/c/*.m"
(cd "${OBJ_OUTPUT_DIR}"; gcc -c -Wall -Werror -fpic -O3 ${INC} ${MY_SOURCES} ${EXTERNAL_SOURCES})
gcc -shared \
  -framework Foundation \
  -framework AVFoundation \
  -framework CoreMedia \
  -framework CoreVideo \
  "${OBJ_OUTPUT_DIR}"/*.o \
  -o "${LIB_OUTPUT_DIR}/${SO_BASE_NAME}.${SO}"
