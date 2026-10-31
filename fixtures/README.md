Fixture transcripts replayed by `scripts/replay.sh`. Format and required scenarios are defined in `docs/component-contract.md`, Section 5.

Each scenario is a pair of files in `fixtures/stage-<n>/`:

- `<scenario>.in` holds one manager command per line, in the order the game asks for them. A Stage 1 or Stage 2 turn reads two lines, the Waiter's command and then the Chef's. A Stage 3 turn reads three, the Host's, the Waiter's and the Chef's, after a `HAPPY_HOUR` or `SKIP` line at the `Manager> ` prompt on every turn until Happy Hour has been used. Each command that is rejected or cannot be parsed adds one extra line. Blank lines and lines starting with `#` are comments; the script strips them before piping the file to the game.
- `<scenario>.expected` is the game's complete standard output for that input, with LF line endings. Prompt lines such as `Waiter> ` end with a space, so do not let an editor strip trailing whitespace from these files.

By default `scripts/replay.sh` replays only the highest-numbered `stage-<n>` directory. Earlier stages stay unchanged as evidence and are replayed at their tag. To add or change a scenario, edit the `.in` file, run `scripts/replay.sh --update fixtures/stage-<n>/<scenario>.in`, and review the regenerated `.expected` diff line by line.

The scenarios and what each demonstrates are listed in the main README's [Stage 1](../README.md#stage-1), [Stage 2](../README.md#stage-2), and [Stage 3](../README.md#stage-3) sections. The Stage 2 and Stage 3 sections also list each payment in the winning run. Stage 3 has two:

| Fixture | What it demonstrates | Result |
| --- | --- | --- |
| `stage-3/host-combo-happy-hour` | The Host does all the seating, which leaves the Waiter free to take C1's to C3's orders on the turn they are seated and to serve O3 and O4 on the turns C4 and C5 are seated. Happy Hour starts on turn 7 (2 turns remaining, 1 on turn 8, 0 from turn 9). C3 orders the Salad + Pasta combo ($24.30, 3 units) during Happy Hour for $19.44 and recovers 8 satisfaction to 100 at the start of turn 8. Orders taken outside Happy Hour are not discounted by it: VIP C2's Burger (turn 4) is $13.50 and Critic C4's Pasta (turn 11) is $18.00. | Five paid, revenue $74.94, average 83.8, VICTORY |
| `stage-3/defeat` | `SKIP` at every `Manager> ` prompt and `WAIT` for the Host, Waiter and Chef, so Happy Hour is offered before all 18 turns and never used. C1 leaves at the end of turn 13 and Critic C4 at the end of turn 18. | Zero paid, two unhappy departures, average N/A, DEFEAT |
