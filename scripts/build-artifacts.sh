#!/bin/bash
set -e
# Usage: scripts/build-artifacts.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"
./gradlew clean build packageLambda --stacktrace
if [ $? -ne 0 ]; then
    echo "[ERROR] Gradle build/package failed"
    exit 1
fi
echo "[INFO] Lambda artifact created at build/distributions/lambda-deployment.zip"
