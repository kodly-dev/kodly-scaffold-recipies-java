#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

TAGS=(
  "skeleton@1.0.1"
  "recipes/redis-cache@1.0.1"
  "recipes/mongodb@1.0.0"
  "recipes/sql@1.0.0"
)

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Initializing git repository..."
  git init
  git add .
  git commit -m "Add base skeleton and test recipes"
fi

for tag in "${TAGS[@]}"; do
  if git rev-parse "$tag" >/dev/null 2>&1; then
    echo "Tag already exists: $tag"
  else
    git tag "$tag"
    echo "Created tag: $tag"
  fi
done

echo "Done. Tags:"
git tag -l 'recipes/*' 'skeleton@*'
