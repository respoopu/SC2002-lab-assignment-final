#!/usr/bin/env bash
# Builds the game, runs the self-check classes, and replays fixture transcripts.
#
#   scripts/replay.sh                              build, replay every fixture under fixtures/
#   scripts/replay.sh fixtures/stage-1/victory.in  replay one fixture
#   scripts/replay.sh --update <fixture.in> ...    rewrite the .expected file(s) from the current output
#
# A fixture is <scenario>.in (one manager command per line; blank lines and
# lines starting with # are ignored) beside <scenario>.expected (the game's
# complete standard output). Exit status is non-zero if any fixture fails.
set -euo pipefail
cd "$(dirname "$0")/.."

update=0
if [[ "${1:-}" == "--update" ]]; then
  update=1
  shift
fi

mkdir -p out
javac --release 17 -d out $(find src -name '*.java')

# Self-checks: every class in restaurantrush.check, run with assertions enabled.
fail=0
for check in $(find src/restaurantrush/check -name '*Check.java' 2>/dev/null | sort); do
  name="$(basename "$check" .java)"
  if java -ea -cp out "restaurantrush.check.$name"; then
    echo "PASS check $name"
  else
    echo "FAIL check $name"
    fail=1
  fi
done

if [[ $# -eq 0 ]]; then
  set -- $(find fixtures -name '*.in' | sort)
fi

for input in "$@"; do
  expected="${input%.in}.expected"
  actual="$(mktemp)"
  status=0
  sed -e '/^[[:space:]]*#/d' -e '/^[[:space:]]*$/d' "$input" \
    | java -cp out restaurantrush.game.Main > "$actual" 2>&1 || status=$?

  if [[ $update -eq 1 ]]; then
    cp "$actual" "$expected"
    echo "UPDATED $expected (exit $status)"
  elif [[ $status -ne 0 ]]; then
    echo "FAIL $input: game exited with status $status"
    tail -n 20 "$actual"
    fail=1
  elif [[ ! -f "$expected" ]]; then
    echo "FAIL $input: missing $expected (create it with --update)"
    fail=1
  elif diff -u "$expected" "$actual"; then
    echo "PASS $input"
  else
    echo "FAIL $input"
    fail=1
  fi
  rm -f "$actual"
done

exit $fail
