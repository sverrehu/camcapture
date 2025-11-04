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
mkdir -p "${LIB_OUTPUT_DIR}"
MY_SOURCES="../../src/main/c/*.c ../../src/main/c/*.m"
for ARCH in x86_64 arm64
do
  mkdir -p "${OBJ_OUTPUT_DIR}.${ARCH}"
  (cd "${OBJ_OUTPUT_DIR}.${ARCH}"; gcc -c -Wall -Werror -fpic -O3 -arch ${ARCH} ${INC} ${MY_SOURCES} ${EXTERNAL_SOURCES})
  gcc -dynamiclib  \
    -framework Foundation \
    -framework AVFoundation \
    -framework CoreMedia \
    -framework CoreVideo \
    -arch ${ARCH} -o "${OBJ_OUTPUT_DIR}/${ARCH}.dylib" "${OBJ_OUTPUT_DIR}.${ARCH}"/*.o
done
lipo -create -output "${LIB_OUTPUT_DIR}/${SO_BASE_NAME}.dylib" "${OBJ_OUTPUT_DIR}"/*.dylib
