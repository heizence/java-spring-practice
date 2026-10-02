#!/usr/bin/env bash
# Maintainer checks (see CLAUDE.md, "Verifying").
#
#   scripts/verify.sh solved   [module] [--rerun]   every test passes and every mutant is caught (solutions applied)
#   scripts/verify.sh unsolved [module] [--rerun]   every exercise starts out failing (the exercises as learners get them)
#
# `module` is a short name such as j05. Without it, every module is checked.
# Gradle reuses earlier test results when nothing changed. `--rerun` forces the tests to run again, which is what
# you want when repeating a check to hunt flaky (timing-dependent) tests.
set -uo pipefail
cd "$(dirname "$0")/.."

mode="${1:-}"
shift || true
module=""
rerun=()
for arg in "$@"; do
  case "$arg" in
    --rerun) rerun=(-Prerun) ;;
    *) module="$arg" ;;
  esac
done

case "$mode" in
  solved|unsolved) ;;
  *) echo "usage: scripts/verify.sh solved|unsolved [module] [--rerun]" >&2; exit 1 ;;
esac

if [[ -n "$module" ]]; then
  dir="$(find java spring -mindepth 1 -maxdepth 1 -type d -name "${module}-*" 2>/dev/null | sort | head -n 1)"
  [[ -n "$dir" ]] || { echo "No module matches '$module'" >&2; exit 1; }
  project=":${dir//\//:}"
  tasks=("$project:test" "$project:mutationTest")
else
  tasks=(test mutationTest)
fi

mkdir -p build
./gradlew "${tasks[@]}" --continue --quiet -Pscore ${rerun[@]+"${rerun[@]}"} > build/verify.log 2>&1
status=$?
if [[ $status -ne 0 ]]; then
  echo "Gradle failed (compile error?). The end of build/verify.log:" >&2
  tail -n 40 build/verify.log >&2
  exit $status
fi

./gradlew --quiet :testkit:scoreReport -Ponly="$module" -Pmode="verify-$mode"
