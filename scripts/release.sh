#!/usr/bin/env bash
# Maintainer tool: publishes the `authoring` branch to the two branches learners use.
#
#   main       = authoring without the .solutions/ overlay   (the exercises)
#   solutions  = authoring with the overlay applied           (the reference solutions)
#
# Run it from a clean `authoring` checkout after both verify modes pass. It commits on main and
# solutions, then returns to authoring. Learners merge `main` into their own branch to get updates.
set -euo pipefail
cd "$(dirname "$0")/.."

message="${1:?usage: scripts/release.sh \"<commit message>\"}"

[[ "$(git rev-parse --abbrev-ref HEAD)" == "authoring" ]] || { echo "Run this on the authoring branch." >&2; exit 1; }
[[ -z "$(git status --porcelain)" ]] || { echo "The working tree isn't clean." >&2; exit 1; }

publish() {
  local branch="$1" with_solutions="$2"
  if git show-ref --verify --quiet "refs/heads/$branch"; then
    git checkout -q "$branch"
  else
    git checkout -q --orphan "$branch"
  fi
  git rm -rqf --ignore-unmatch . > /dev/null
  git checkout authoring -- .
  if [[ "$with_solutions" == "yes" ]]; then
    cp -R .solutions/. .
  fi
  git rm -rq --cached .solutions > /dev/null
  rm -rf .solutions
  git add -A
  if git diff --cached --quiet; then
    echo "$branch: nothing changed"
  else
    git commit -q -m "$message"
    echo "$branch: committed"
  fi
}

publish main no
publish solutions yes
git checkout -q authoring
echo "Done. Learners: git checkout my-work && git merge main"
