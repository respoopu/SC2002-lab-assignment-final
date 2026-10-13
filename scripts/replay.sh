#!/usr/bin/env bash
# Builds the game, runs the self-check classes, and replays fixture transcripts.
#
#   scripts/replay.sh                              build, replay every fixture of the current stage
#   scripts/replay.sh fixtures/stage-1/victory.in  replay one fixture
#   scripts/replay.sh --update <fixture.in> ...    rewrite the .expected file(s) from the current output
#
# A fixture is <scenario>.in (one manager command per line; blank lines and
# lines starting with # are ignored) beside <scenario>.expected (the game's
# complete standard output; stderr is shown only on failure). Exit status is non-zero if any fixture fails.
set -euo pipefail
cd "$(dirname "$0")/.."

update=0
if [[ "${1:-}" == "--update" ]]; then
  update=1
  shift
  if [[ $# -eq 0 ]]; then
    echo "usage: scripts/replay.sh --update <fixture.in> [...]  (name the fixtures to rewrite)" >&2
    exit 2
  fi
fi

mkdir -p out
javac --release 17 -d out $(find src -name '*.java')

# Self-checks: every class in restaurantrush.check, run with assertions enabled.
fail=0
for check in $(find src/restaurantrush/check -name '*Check.java' 2>/dev/null | sort); do
  name="$(basename "$check" .java)"
  if java -ea -cp out "restaurantrush.check.$name" < /dev/null; then
    echo "PASS check $name"
  else
    echo "FAIL check $name"
    fail=1
  fi
done

# Default: only the current stage, the highest-numbered fixtures/stage-<n>.
# Earlier stages are verified at their tag (contract Section 5).
if [[ $# -eq 0 ]]; then
  current="$(find fixtures -mindepth 1 -maxdepth 1 -type d -name 'stage-*' | sort -t- -k2 -n | tail -n 1 || true)"
  if [[ -n "$current" ]]; then
    set -- $(find "$current" -name '*.in' | sort)
  fi
fi

for input in "$@"; do
  expected="${input%.in}.expected"
  actual="$(mktemp)"
  errors="$(mktemp)"
  status=0
  sed -e '/^[[:space:]]*#/d' -e '/^[[:space:]]*$/d' "$input" \
    | java -cp out restaurantrush.game.Main > "$actual" 2> "$errors" || status=$?

  if [[ $update -eq 1 ]]; then
    cp "$actual" "$expected"
    echo "UPDATED $expected (exit $status)"
  elif [[ $status -ne 0 ]]; then
    echo "FAIL $input: game exited with status $status"
    tail -n 20 "$actual"
    cat "$errors"
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
  rm -f "$actual" "$errors"
done

exit $fail
