cmake_minimum_required(VERSION 3.15)
project(openredukti_jni VERSION 1.0)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# ---- JDK / JNI ----
find_package(JNI REQUIRED)

# ---- OpenRedukti ----
# Set via -DOPENREDUKTI_SRC_DIR=/path/to/OpenRedukti when invoking cmake
set(OPENREDUKTI_SRC_DIR "" CACHE PATH "OpenRedukti source root")
set(OPENREDUKTI_BUILD_DIR "" CACHE PATH "OpenRedukti cmake build directory")

if(NOT OPENREDUKTI_SRC_DIR OR NOT OPENREDUKTI_BUILD_DIR)
    message(FATAL_ERROR
        "Set -DOPENREDUKTI_SRC_DIR and -DOPENREDUKTI_BUILD_DIR to the "
        "OpenRedukti source root and its cmake build directory respectively.")
endif()

# Headers: public C++ API + generated protobuf C++ headers
set(OPENREDUKTI_INCLUDE_DIRS
    "${OPENREDUKTI_SRC_DIR}/include"
    "${OPENREDUKTI_SRC_DIR}/cpp"
    "${OPENREDUKTI_BUILD_DIR}/proto"      # generated *.pb.h files live here
)

find_library(OPENREDUKTI_LIBRARY
    NAMES openredukti
    PATHS "${OPENREDUKTI_BUILD_DIR}"
    NO_DEFAULT_PATH
    REQUIRED
)

# ---- Protobuf (needed to link generated pb.cc code) ----
find_package(Protobuf REQUIRED)

# ---- nlohmann/json (single-header, no install needed) ----
include(FetchContent)
FetchContent_Declare(
    nlohmann_json
    URL https://github.com/nlohmann/json/releases/download/v3.11.3/json.hpp
    DOWNLOAD_NO_EXTRACT TRUE
    DOWNLOAD_DIR "${CMAKE_BINARY_DIR}/nlohmann"
)
FetchContent_MakeAvailable(nlohmann_json)

# ---- Shared library ----
add_library(openredukti_jni SHARED
    src/openredukti_jni.cpp
)

target_include_directories(openredukti_jni PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${OPENREDUKTI_INCLUDE_DIRS}
    ${Protobuf_INCLUDE_DIRS}
    "${CMAKE_BINARY_DIR}/nlohmann"
)

target_link_libraries(openredukti_jni PRIVATE
    ${OPENREDUKTI_LIBRARY}
    ${Protobuf_LIBRARIES}
    ${JNI_LIBRARIES}
)

# Strip release builds
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_custom_command(TARGET openredukti_jni POST_BUILD
        COMMAND ${CMAKE_STRIP} $<TARGET_FILE:openredukti_jni>)
endif()

install(TARGETS openredukti_jni DESTINATION lib)
