#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

usage() {
  echo "Usage: $0 <recipe-ref> [recipe-ref...]"
  echo
  echo "Delete local and remote git tags for retired recipes."
  echo "Refs may be name@version or recipes/name@version."
  echo
  echo "Example:"
  echo "  $0 actuator@1.0.0 validation@1.0.0"
  echo "  $0 recipes/actuator@1.0.0"
  exit 1
}

if [[ $# -lt 1 ]]; then
  usage
fi

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Not a git repository: $REPO_ROOT" >&2
  exit 1
fi

normalize_tag() {
  local ref="$1"
  if [[ "$ref" == recipes/* ]] || [[ "$ref" == skeleton@* ]]; then
    echo "$ref"
  else
    echo "recipes/${ref}"
  fi
}

for ref in "$@"; do
  tag="$(normalize_tag "$ref")"

  if git rev-parse "$tag" >/dev/null 2>&1; then
    git tag -d "$tag"
    echo "Deleted local tag: $tag"
  else
    echo "Local tag not found (ok): $tag"
  fi

  if git push origin ":refs/tags/${tag}"; then
    echo "Deleted remote tag: $tag"
  else
    echo "Failed to delete remote tag: $tag" >&2
    exit 1
  fi
done

echo "Done."
