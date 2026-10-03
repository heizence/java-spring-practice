#!/usr/bin/env bash
# Maintainer tool: publishes the `authoring` branch to the two branches learners use.
#
#   main       = authoring without the .solutions/ overlay and .authoring/ (the exercises)
#   solutions  = authoring with the overlay applied           (the reference solutions)
#
# Run it from a clean `authoring` checkout after both verify modes pass. It commits on main and
# solutions, then returns to authoring. Learners merge `main` into their own branch to get updates.
set -euo pipefail
cd "$(dirname "$0")/.."

message="${1:?usage: scripts/release.sh \"<commit message>\"}"

[[ "$(git rev-parse --abbrev-ref HEAD)" == "authoring" ]] || { echo "Run this on the authoring branch." >&2; exit 1; }
[[ -z "$(git status --porcelain)" ]] || { echo "The working tree isn't clean." >&2; exit 1; }

# Run a git command, retrying briefly if another tool holds git's index lock for a moment.
g() {
  local attempt
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if git "$@"; then
      return 0
    fi
    [[ -e .git/index.lock && $attempt -lt 10 ]] || return 1
    sleep 1
  done
}

publish() {
  local branch="$1" with_solutions="$2"
  if git show-ref --verify --quiet "refs/heads/$branch"; then
    g checkout -q "$branch"
  else
    g checkout -q --orphan "$branch"
  fi
  g rm -rqf --ignore-unmatch . > /dev/null
  g checkout authoring -- .
  if [[ "$with_solutions" == "yes" ]]; then
    cp -R .solutions/. .
  fi
  # Maintainer-only folders never reach the learner branches.
  g rm -rq --cached --ignore-unmatch .solutions .authoring > /dev/null
  rm -rf .solutions .authoring
  g add -A
  if git diff --cached --quiet; then
    echo "$branch: nothing changed"
  else
    g commit -q -m "$message"
    echo "$branch: committed"
  fi
}

publish main no
publish solutions yes
g checkout -q authoring
echo "Done. Learners: git checkout my-work && git merge main"
