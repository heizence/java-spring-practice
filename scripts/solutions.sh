#!/usr/bin/env bash
# Maintainer tool for the `authoring` branch, where reference solutions live in a .solutions/ overlay
# that mirrors the repository layout (see CLAUDE.md).
#
#   scripts/solutions.sh apply [module-dir]   copy the overlay over the exercises (e.g. java/j05-collections)
#   scripts/solutions.sh reset [module-dir]   undo `apply`: restore every overlaid file from git and
#                                             delete the files that only exist in the overlay
#
# Commit your exercise files before `apply`, and run `reset` before committing again.
set -euo pipefail
cd "$(dirname "$0")/.."

action="${1:-}"
target="${2:-}"
overlay=".solutions${target:+/$target}"

if [[ ! -d .solutions ]]; then
  echo "No .solutions/ overlay here. This tool only works on the authoring branch." >&2
  exit 1
fi
if [[ ! -d "$overlay" ]]; then
  echo "No solutions in $overlay" >&2
  exit 1
fi

case "$action" in
  apply)
    mkdir -p "${target:-.}"
    cp -R "$overlay/." "${target:-.}/"
    echo "Applied $overlay. Run 'scripts/solutions.sh reset${target:+ $target}' before committing."
    ;;
  reset)
    while IFS= read -r -d '' file; do
      path="${file#.solutions/}"
      if git ls-files --error-unmatch -- "$path" > /dev/null 2>&1; then
        git checkout -- "$path"
      else
        rm -f -- "$path"
      fi
    done < <(find "$overlay" -type f -print0)
    # Remove directories that only held overlay files (for example verification mutants).
    find java spring -type d -empty -delete 2>/dev/null || true
    echo "Restored the exercise files${target:+ in $target}."
    ;;
  *)
    echo "usage: scripts/solutions.sh apply|reset [module-dir]" >&2
    exit 1
    ;;
esac
