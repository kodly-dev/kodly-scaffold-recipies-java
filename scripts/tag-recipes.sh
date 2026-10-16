#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# Manifests declare release versions once. Published Git tags are the consumer catalog.
TAGS_OUTPUT=$(node --input-type=module <<'JS'
import fs from 'node:fs';
const paths = ['skeleton', ...fs.readdirSync('recipes', { withFileTypes: true })
  .filter(entry => entry.isDirectory()).map(entry => `recipes/${entry.name}`).sort()];
for (const directory of paths) {
  const { name, version } = JSON.parse(fs.readFileSync(`${directory}/manifest.json`, 'utf8'));
  const expected = directory === 'skeleton' ? 'skeleton' : directory.slice('recipes/'.length);
  if (name !== expected || !/^[a-zA-Z0-9][a-zA-Z0-9._-]*$/.test(name)
      || !/^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$/.test(version)) {
    throw new Error(`Invalid release name/version in ${directory}/manifest.json`);
  }
  console.log(`${directory}@${version}`);
}
JS
)

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Initializing git repository..."
  git init
  git add .
  git commit -m "Add base skeleton and test recipes"
fi

while IFS= read -r tag; do
  if git rev-parse "$tag" >/dev/null 2>&1; then
    echo "Tag already exists: $tag"
  else
    git tag "$tag"
    echo "Created tag: $tag"
  fi
done <<< "$TAGS_OUTPUT"

echo "Done. Tags:"
git tag -l 'recipes/*' 'skeleton@*'
