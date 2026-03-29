#!/usr/bin/env bash
set -euo pipefail

if ! command -v mvn &>/dev/null; then
  echo "Maven is not installed. Install Apache Maven (https://maven.apache.org/download.cgi) and retry."
  exit 1
fi

mvn clean spring-boot:run
