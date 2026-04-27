# syntax=docker/dockerfile:1.6
#
# FEP TLQ Runtime — Linux x86-64 base image with TongLINK/Q 8.1.15.2-P3 native libs
#
# Build context layout (caller must stage 6 .so under TLQ_SDK_LIB_DIR before build):
#   docker/tlq-runtime/
#   ├── Dockerfile                  ← this file
#   ├── README.md
#   ├── .dockerignore
#   └── tlq-sdk-lib/                ← caller-staged, NOT committed (see .gitignore)
#       ├── libgmssljni.so
#       ├── libjtlq_client.so
#       ├── libtl_public.so
#       ├── libtl_tcapi.so
#       ├── libtlqcrycomp.so
#       └── libtlqtcapi6.so
#
# Build:
#   cd docker/tlq-runtime/
#   cp -r "/path/to/TLQ_SDK_LIB" ./tlq-sdk-lib
#   docker build -t fep-tlq-runtime:8.1.15.2-P3 .
#
# Run (mount fat jar into container):
#   docker run --rm -v /path/to/fep-app.jar:/app.jar fep-tlq-runtime:8.1.15.2-P3 java -jar /app.jar
#
# SDK JAR is NOT bundled — Spring Boot fat jar is expected to provide it via mvn local repo.
# See README.md for glibc compatibility fallback (centos:7) if .so loading fails on jammy.
FROM eclipse-temurin:17-jre-jammy

LABEL org.opencontainers.image.title="FEP TLQ Runtime"
LABEL org.opencontainers.image.description="Linux x86-64 runtime image with TongLINK/Q 8.1.15.2-P3 native libraries"
LABEL org.opencontainers.image.version="8.1.15.2-P3"
LABEL org.opencontainers.image.vendor="FEP Team"
LABEL org.opencontainers.image.source="https://github.com/wilsonduhang/FEP_v1.0"

# TLQ SDK native lib directory (relative to build context). Override with --build-arg if needed.
ARG TLQ_SDK_LIB_DIR=tlq-sdk-lib

# Create lib directory before COPY for clarity (COPY would create it implicitly).
RUN mkdir -p /opt/tlq/lib

# Copy 6 .so explicitly (instead of glob) so missing libs fail the build immediately
# rather than silently producing a runtime UnsatisfiedLinkError.
COPY ${TLQ_SDK_LIB_DIR}/libgmssljni.so   /opt/tlq/lib/
COPY ${TLQ_SDK_LIB_DIR}/libjtlq_client.so /opt/tlq/lib/
COPY ${TLQ_SDK_LIB_DIR}/libtl_public.so   /opt/tlq/lib/
COPY ${TLQ_SDK_LIB_DIR}/libtl_tcapi.so    /opt/tlq/lib/
COPY ${TLQ_SDK_LIB_DIR}/libtlqcrycomp.so  /opt/tlq/lib/
COPY ${TLQ_SDK_LIB_DIR}/libtlqtcapi6.so   /opt/tlq/lib/

# Dual-protection for native lib resolution:
#  1. LD_LIBRARY_PATH lets the dynamic linker (ld.so) find dependent .so during dlopen()
#  2. -Djava.library.path overrides JVM's System.loadLibrary() search path
# Both are needed because TLQ JNI loads libjtlq_client.so, which dlopens 5 sibling libs.
ENV LD_LIBRARY_PATH=/opt/tlq/lib:${LD_LIBRARY_PATH:-}
ENV JAVA_TOOL_OPTIONS="-Djava.library.path=/opt/tlq/lib"

# Default workdir; user supplies their own jar via -v mount or downstream FROM.
WORKDIR /app

# Smoke check: image without an app prints java version (allows `docker run --rm fep-tlq-runtime:tag` sanity test).
CMD ["java", "-version"]
