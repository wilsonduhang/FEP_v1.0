#!/usr/bin/env bash
#
# install-tlq-sdk.sh — Install TongLINK/Q SDK JARs into the local Maven repository (~/.m2).
#
# Usage:
#   ./scripts/install-tlq-sdk.sh --sdk-dir <path>
#   TLQ_SDK_DIR=<path> ./scripts/install-tlq-sdk.sh
#
# Behaviour:
#   * Installs `tlclient.jar`        as com.tongtech:tlclient:8.1.15.2-P3
#   * Installs `TLQRemoteApi.jar`    as com.tongtech:tlq-remote-api:8.1.15.2-P3
#   * Skips `tlq63j.jar` (legacy ThinAPI excluded by P1c decision §3.4)
#   * Refuses to run if Maven (mvn) is not on PATH.
#
# References:
#   docs/plans/2026-04-26-p1c-sdk-validation-and-decisions.md §3.2 (install strategy)
#   docs/plans/2026-04-26-p1c-sdk-validation-and-decisions.md §3.4 (tlq63j.jar exclusion)
#

set -euo pipefail

readonly SDK_VERSION="8.1.15.2-P3"
readonly SDK_GROUP_ID="com.tongtech"
readonly TLCLIENT_ARTIFACT_ID="tlclient"
readonly TLQ_REMOTE_API_ARTIFACT_ID="tlq-remote-api"
readonly TLCLIENT_JAR="tlclient.jar"
readonly TLQ_REMOTE_API_JAR="TLQRemoteApi.jar"
readonly LEGACY_THIN_API_JAR="tlq63j.jar"

print_usage() {
    cat <<'USAGE'
Usage:
  ./scripts/install-tlq-sdk.sh --sdk-dir <path>
  TLQ_SDK_DIR=<path> ./scripts/install-tlq-sdk.sh

Installs TongLINK/Q SDK JARs into the local Maven repository (~/.m2).
USAGE
}

log_info()    { printf '[INFO] %s\n' "$*"; }
log_warn()    { printf '[WARN] %s\n' "$*" >&2; }
log_error()   { printf '[ERROR] %s\n' "$*" >&2; }
log_success() { printf '[SUCCESS] %s\n' "$*"; }

# --- Parse CLI args ---
sdk_dir="${TLQ_SDK_DIR:-}"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --sdk-dir)
            if [[ $# -lt 2 ]]; then
                log_error "--sdk-dir requires a path argument"
                print_usage
                exit 2
            fi
            sdk_dir="$2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown argument: $1"
            print_usage
            exit 2
            ;;
    esac
done

if [[ -z "$sdk_dir" ]]; then
    log_error "SDK directory not provided. Set TLQ_SDK_DIR or pass --sdk-dir <path>."
    print_usage
    exit 2
fi

if [[ ! -d "$sdk_dir" ]]; then
    log_error "SDK directory does not exist: $sdk_dir"
    exit 1
fi

# Locate Maven binary: prefer system `mvn`, fall back to repo `./mvnw` wrapper.
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

if command -v mvn >/dev/null 2>&1; then
    mvn_cmd=("mvn")
elif [[ -x "$repo_root/mvnw" ]]; then
    mvn_cmd=("$repo_root/mvnw")
    log_info "Using Maven wrapper: $repo_root/mvnw"
else
    log_error "Neither 'mvn' on PATH nor './mvnw' wrapper found at $repo_root/mvnw."
    exit 1
fi

log_info "TLQ SDK directory: $sdk_dir"
log_info "Target Maven coordinates: $SDK_GROUP_ID:{$TLCLIENT_ARTIFACT_ID,$TLQ_REMOTE_API_ARTIFACT_ID}:$SDK_VERSION"

# --- Reject legacy ThinAPI jar (P1c decision §3.4) ---
if [[ -f "$sdk_dir/$LEGACY_THIN_API_JAR" ]]; then
    log_info "已排除老 ThinAPI ($LEGACY_THIN_API_JAR — P1c §3.4 决策不引入)"
fi

# --- Install tlclient.jar ---
tlclient_path="$sdk_dir/$TLCLIENT_JAR"
if [[ ! -f "$tlclient_path" ]]; then
    log_error "Required JAR not found: $tlclient_path"
    exit 1
fi

log_info "Installing $TLCLIENT_JAR -> $SDK_GROUP_ID:$TLCLIENT_ARTIFACT_ID:$SDK_VERSION"
"${mvn_cmd[@]}" install:install-file \
    -Dfile="$tlclient_path" \
    -DgroupId="$SDK_GROUP_ID" \
    -DartifactId="$TLCLIENT_ARTIFACT_ID" \
    -Dversion="$SDK_VERSION" \
    -Dpackaging=jar \
    -DgeneratePom=true \
    -B \
    --no-transfer-progress

# --- Install TLQRemoteApi.jar ---
tlq_remote_api_path="$sdk_dir/$TLQ_REMOTE_API_JAR"
if [[ ! -f "$tlq_remote_api_path" ]]; then
    log_error "Required JAR not found: $tlq_remote_api_path"
    exit 1
fi

log_info "Installing $TLQ_REMOTE_API_JAR -> $SDK_GROUP_ID:$TLQ_REMOTE_API_ARTIFACT_ID:$SDK_VERSION"
"${mvn_cmd[@]}" install:install-file \
    -Dfile="$tlq_remote_api_path" \
    -DgroupId="$SDK_GROUP_ID" \
    -DartifactId="$TLQ_REMOTE_API_ARTIFACT_ID" \
    -Dversion="$SDK_VERSION" \
    -Dpackaging=jar \
    -DgeneratePom=true \
    -B \
    --no-transfer-progress

log_success "TLQ SDK $SDK_VERSION 已安装至 ~/.m2"
