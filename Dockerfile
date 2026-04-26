# =============================================================================
# Stage 1 – Build OpenRedukti C++ library
# =============================================================================
FROM ubuntu:22.04 AS cpp-builder

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential cmake git \
        libprotobuf-dev protobuf-compiler \
        libopenblas-dev liblapack-dev \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt
RUN git clone --depth 1 https://github.com/redukti/OpenRedukti.git

# OpenRedukti CMakeLists uses a local set(CMAKE_CXX_STANDARD 14) that shadows
# the -D flag, so we patch it directly to C++17 (required for std::size).
RUN sed -i 's/set(CMAKE_CXX_STANDARD 14)/set(CMAKE_CXX_STANDARD 17)/' \
        /opt/OpenRedukti/CMakeLists.txt

RUN cmake -S /opt/OpenRedukti -B /opt/openredukti-build \
        -DCMAKE_BUILD_TYPE=Release \
        -DGRPC_SERVER=OFF \
        -DUSE_OPENBLAS=ON \
    && cmake --build /opt/openredukti-build --parallel "$(nproc)"

# =============================================================================
# Stage 2 – Build JNI wrapper (.so)
# =============================================================================
FROM cpp-builder AS jni-builder

# JDK is needed only for JNI headers at compile time
RUN apt-get update && apt-get install -y --no-install-recommends \
        openjdk-21-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

COPY jni /opt/jni-src

RUN cmake -S /opt/jni-src -B /opt/jni-build \
        -DCMAKE_BUILD_TYPE=Release \
        -DOPENREDUKTI_SRC_DIR=/opt/OpenRedukti \
        -DOPENREDUKTI_BUILD_DIR=/opt/openredukti-build \
        -DJAVA_HOME=${JAVA_HOME} \
    && cmake --build /opt/jni-build --parallel "$(nproc)"

# =============================================================================
# Stage 3 – Build Spring Boot JAR
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS java-builder

WORKDIR /workspace
COPY pom.xml .
RUN mvn -f pom.xml dependency:go-offline -q

COPY src ./src
RUN mvn -f pom.xml package -DskipTests -q

# =============================================================================
# Stage 4 – Runtime image
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

# Native runtime deps (OpenBLAS, Protobuf, LAPACK)
RUN apt-get update && apt-get install -y --no-install-recommends \
        libopenblas0-pthread \
        liblapack3 \
        libprotobuf23 \
    && rm -rf /var/lib/apt/lists/*

# Copy native libraries
COPY --from=cpp-builder /opt/openredukti-build/libopenredukti.so /usr/local/lib/
COPY --from=jni-builder  /opt/jni-build/libopenredukti_jni.so    /usr/local/lib/
RUN ldconfig

# Copy application
WORKDIR /app
COPY --from=java-builder /workspace/target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.library.path=/usr/local/lib", "-jar", "app.jar"]
