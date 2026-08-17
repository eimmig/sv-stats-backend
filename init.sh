#!/usr/bin/env bash
# Verification for stats-service (Java 25 + Spring Boot 4.x).
set -euo pipefail

if ! command -v java >/dev/null 2>&1; then
  echo "MISS Java not found on PATH (need Java 25)"
  exit 1
fi
echo "OK   $(java --version 2>&1 | head -n1)"

if [ -f "pom.xml" ]; then
  echo "OK   Maven project detected (pom.xml)"
  if ! command -v mvn >/dev/null 2>&1; then
    echo "MISS mvn not found on PATH"
    exit 1
  fi
  mvn -q -DskipTests=false verify
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
  echo "FAIL Gradle project detected, but docs/CONVENTIONS.md decided Maven for all Java"
  echo "     services (to avoid each service using a different build tool). Migrate to"
  echo "     Maven, or get docs/CONVENTIONS.md updated first if this needs to change."
  exit 1
else
  echo "----  No pom.xml or build.gradle(.kts) yet — feat-001 not started."
  echo "     See feature_list.json for the next step."
  exit 1
fi

echo "stats-service verification passed."
