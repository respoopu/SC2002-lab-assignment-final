Fixture transcripts replayed by `scripts/replay.sh`. Format and required scenarios are defined in `docs/component-contract.md`, Section 5.

Each scenario is a pair of files in `fixtures/stage-<n>/`:

- `<scenario>.in` holds one manager command per line, in the order the game asks for them. A Stage 1 turn reads two lines, the Waiter's command and then the Chef's, plus one extra line for each command that is rejected or cannot be parsed. Blank lines and lines starting with `#` are comments; the script strips them before piping the file to the game.
- `<scenario>.expected` is the game's complete standard output for that input, with LF line endings. Prompt lines such as `Waiter> ` end with a space, so do not let an editor strip trailing whitespace from these files.

By default `scripts/replay.sh` replays only the highest-numbered `stage-<n>` directory. Earlier stages stay unchanged as evidence and are replayed at their tag. To add or change a scenario, edit the `.in` file, run `scripts/replay.sh --update fixtures/stage-<n>/<scenario>.in`, and review the regenerated `.expected` diff line by line.

The Stage 1 scenarios and what each demonstrates are listed in the main `README.md`, Stage 1 section.
