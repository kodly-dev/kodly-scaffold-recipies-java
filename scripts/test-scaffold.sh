#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RECIPES_REPO="${KODLY_RECIPES_REPO_URL:-file://${REPO_ROOT}}"
WORKSPACE="${1:-/tmp/kodly-scaffold-test}"

echo "==> Preparing test workspace at ${WORKSPACE}"
rm -rf "$WORKSPACE"
mkdir -p "$(dirname "$WORKSPACE")"

echo "==> Initializing skeleton from ${RECIPES_REPO}"
export KODLY_RECIPES_REPO_URL="$RECIPES_REPO"

kodly scaffold init --group com.test.app --name test-app --path "$WORKSPACE"

echo "==> Applying recipes"
cd "$WORKSPACE"

kodly scaffold redis-cache@1.0.1
kodly scaffold sql@1.0.0

echo "==> Listing cached recipes"
kodly scaffold list

echo "==> Compiling project"
./gradlew compileJava

echo ""
echo "Success. Test workspace: ${WORKSPACE}"
echo "Note: smoke test applies sql (JPA+Flyway). For Mongo+Mongock use: kodly scaffold mongodb@1.0.0"
