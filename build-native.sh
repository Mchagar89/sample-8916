#!/usr/bin/env bash
# build-native.sh – Build OpenRedukti + JNI wrapper locally (macOS / Linux)
#
# Prerequisites (macOS):  brew install cmake openblas lapack protobuf
# Prerequisites (Ubuntu):
#   sudo apt-get install cmake build-essential libopenblas-dev liblapack-dev \
#                        libprotobuf-dev protobuf-compiler
#
# Usage:
#   ./build-native.sh              # full build
#   ./build-native.sh --jni-only   # rebuild JNI wrapper only (OpenRedukti already built)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_ROOT="${SCRIPT_DIR}/.native-build"
OPENREDUKTI_SRC="${BUILD_ROOT}/OpenRedukti"
OPENREDUKTI_BUILD="${BUILD_ROOT}/openredukti-build"
JNI_BUILD="${BUILD_ROOT}/jni-build"
JNI_INSTALL="${SCRIPT_DIR}/src/main/resources/native/$(uname -s | tr '[:upper:]' '[:lower:]')"

JNI_ONLY=false
for arg in "$@"; do [[ "$arg" == "--jni-only" ]] && JNI_ONLY=true; done

# ---- Step 1: Clone and build OpenRedukti ----
if [[ "$JNI_ONLY" == false ]]; then
    echo ">>> Cloning OpenRedukti..."
    mkdir -p "${BUILD_ROOT}"
    if [[ ! -d "${OPENREDUKTI_SRC}" ]]; then
        git clone --depth 1 https://github.com/redukti/OpenRedukti.git "${OPENREDUKTI_SRC}"
    fi

    echo ">>> Building OpenRedukti..."
    cmake -S "${OPENREDUKTI_SRC}" -B "${OPENREDUKTI_BUILD}" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_CXX_STANDARD=17 \
        -DGRPC_SERVER=OFF \
        -DUSE_OPENBLAS=ON
    cmake --build "${OPENREDUKTI_BUILD}" --parallel "$(nproc 2>/dev/null || sysctl -n hw.logicalcpu)"
    echo ">>> OpenRedukti built at: ${OPENREDUKTI_BUILD}"
fi

# ---- Step 2: Build JNI wrapper ----
echo ">>> Building JNI wrapper..."
JAVA_HOME="${JAVA_HOME:-$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | awk '{print $3}')}"

cmake -S "${SCRIPT_DIR}/jni" -B "${JNI_BUILD}" \
    -DCMAKE_BUILD_TYPE=Release \
    -DOPENREDUKTI_SRC_DIR="${OPENREDUKTI_SRC}" \
    -DOPENREDUKTI_BUILD_DIR="${OPENREDUKTI_BUILD}" \
    -DJAVA_HOME="${JAVA_HOME}"
cmake --build "${JNI_BUILD}" --parallel "$(nproc 2>/dev/null || sysctl -n hw.logicalcpu)"

# ---- Step 3: Copy .so / .dylib to resources ----
mkdir -p "${JNI_INSTALL}"
if [[ "$(uname -s)" == "Darwin" ]]; then
    cp "${JNI_BUILD}/libopenredukti_jni.dylib" "${JNI_INSTALL}/"
    cp "${OPENREDUKTI_BUILD}/libopenredukti.dylib" "${JNI_INSTALL}/"
else
    cp "${JNI_BUILD}/libopenredukti_jni.so"  "${JNI_INSTALL}/"
    cp "${OPENREDUKTI_BUILD}/libopenredukti.so" "${JNI_INSTALL}/"
fi

echo ""
echo ">>> Native libraries installed to: ${JNI_INSTALL}"
echo ">>> Run Spring Boot with: mvn spring-boot:run -Djava.library.path=${JNI_INSTALL}"
