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
    # `reset` restores overlaid files from git and deletes the ones git doesn't know. Uncommitted work in the
    # target would be lost, so refuse to start from a dirty tree.
    check_paths=("${target:-java}")
    [[ -z "$target" && -d spring ]] && check_paths+=(spring)
    if [[ -n "$(git status --porcelain -- "${check_paths[@]}")" ]]; then
      echo "Uncommitted changes in ${check_paths[*]}. Commit them first: 'reset' would overwrite or delete them." >&2
      git status --short -- "${check_paths[@]}" | head -n 10 >&2
      exit 1
    fi
    mkdir -p "${target:-.}"
    cp -R "$overlay/." "${target:-.}/"
    echo "Applied $overlay. Run 'scripts/solutions.sh reset${target:+ $target}' before committing."
    ;;
  reset)
    tracked=()
    untracked=()
    while IFS= read -r -d '' file; do
      path="${file#.solutions/}"
      if git ls-files --error-unmatch -- "$path" > /dev/null 2>&1; then
        tracked+=("$path")
      else
        untracked+=("$path")
      fi
    done < <(find "$overlay" -type f -print0)
    # Restore all tracked files in one git call, retrying briefly: IDEs and other tools take git's index lock for
    # a moment when they refresh, and a reset must not stop halfway.
    if [[ ${#tracked[@]} -gt 0 ]]; then
      for attempt in 1 2 3 4 5 6 7 8 9 10; do
        if printf '%s\0' "${tracked[@]}" | git checkout --pathspec-from-file=- --pathspec-file-nul 2> /dev/null; then
          break
        fi
        [[ $attempt -eq 10 ]] && { echo "git checkout kept failing (index.lock?). Run reset again." >&2; exit 1; }
        sleep 1
      done
    fi
    for path in ${untracked[@]+"${untracked[@]}"}; do
      rm -f -- "$path"
    done
    # Remove directories that only held overlay files (for example verification mutants).
    find java spring -type d -empty -delete 2>/dev/null || true
    echo "Restored the exercise files${target:+ in $target}."
    ;;
  *)
    echo "usage: scripts/solutions.sh apply|reset [module-dir]" >&2
    exit 1
    ;;
esac
