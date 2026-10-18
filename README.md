# SC2002 Restaurant Rush

Repository: https://github.com/respoopu/SC2002-lab-assignment-final

Java command-line restaurant management game for the SC2002 group assignment.

The [assignment brief](SC2002_Restaurant_Rush_Group_Assignment.docx.pdf) is the source of truth for the game rules and submission requirements.

The [component contract](docs/component-contract.md) records the team's agreed rules, responsibilities, turn timing, implementation decisions, and the shared Java API that every workstream issue builds against.

## Project status

Stage 1, the complete base game with Regular customers, is playable from the command line and has reproducible victory and defeat runs. Stage 2 and Stage 3 work is tracked in the GitHub issues.

## Planned stages

| Tag | Required milestone |
| --- | --- |
| `stage-1` | Complete base game with Regular customers and both outcomes |
| `stage-2` | Add VIP and Critic customer behavior |
| `stage-3` | Add Host, ComboMeal, and Happy Hour |

Each tag will identify a tested, runnable commit. The Stage 1 section below has the build, run, and replay commands; the contract has the full rules (Sections 5 and 6). Stage change summaries and video timestamps will be added as the corresponding work is completed.

## Stage 1

### Build and run

You need JDK 17 or later. On Windows, run the commands from Git Bash.

```sh
javac --release 17 -d out $(find src -name '*.java')
java -cp out restaurantrush.game.Main
```

The game always plays 18 turns. Each turn prints any arrival, the status display, and the legal actions, then asks for one Waiter command at `Waiter> ` and one Chef command at `Chef> `. Commands are case-insensitive:

| Prompt | Commands |
| --- | --- |
| `Waiter> ` | `SEAT C<n>`, `ORDER C<n>`, `SERVE O<n>`, `WAIT` |
| `Chef> ` | `PREPARE O<n>`, `WAIT` |

An invalid or illegal command prints the reason and asks again without using up that staff member's task. The Cashier is automatic and collects at most one payment per turn. If input ends before the game finishes, the game prints `Input ended before the game finished.` and exits with status 2.

### Arrivals and meal choice

One Regular customer arrives at the start of turns 1, 4, 7, 10, 13, and 16, with IDs `C1` to `C6` in arrival order. Customers choose their own meal by their 1-based arrival index, cycling Salad, Burger, Pasta (contract Section 6). The manager cannot choose the food.

| Customer | Arrives on turn | Meal |
| --- | --- | --- |
| C1 | 1 | Salad ($9.00, 1 unit) |
| C2 | 4 | Burger ($15.00, 1 unit) |
| C3 | 7 | Pasta ($18.00, 2 units) |
| C4 | 10 | Salad |
| C5 | 13 | Burger |
| C6 | 16 | Pasta |

Orders are numbered `O1`, `O2`, ... in the order they are taken, and the tables are `T1` and `T2`.

### Replay the reproducible runs

```sh
scripts/replay.sh                              # build, run every self-check, replay every fixture of the current stage
scripts/replay.sh fixtures/stage-1/victory.in  # replay one fixture
```

The script prints `PASS` or `FAIL` for each self-check class and each fixture, and exits non-zero on any failure. It strips `#` comments and blank lines from the `.in` file before piping it to the game, so use the script rather than `java ... < victory.in`. After an intended output change, `scripts/replay.sh --update <fixture.in>` rewrites that transcript; review the diff before committing it. To check Stage 1 after later stages have been added, run `git checkout stage-1 && scripts/replay.sh`.

Each fixture under `fixtures/stage-1/` is a `.in` file of manager commands with comments for every turn, beside the `.expected` transcript of the game's complete output:

| Fixture | What it demonstrates | Result |
| --- | --- | --- |
| `victory` | A full game that uses both tables. It reproduces the brief's Pasta example with C3: seated on turn 7, ordered on turn 8, READY on turn 9, served on turn 10, ready to pay on turn 11, and paid $18.00 at satisfaction 76 on turn 12. | C1 to C5 pay, revenue $66.00, average 79.2, VICTORY |
| `defeat` | The Waiter and the Chef wait on every turn. Nobody pays, so the average shows `N/A` throughout, and C1 and C2 leave unhappy at the end of turns 13 and 16. | 0 paid, 2 unhappy departures, DEFEAT |
| `prioritization-a` | First run of the pair: the victory choices. While one customer eats or waits to pay, the Waiter seats the next arrival (C2 to C5) at the free table. C6 is left in the queue because it cannot pay by turn 18. | VICTORY, as in `victory` |
| `prioritization-b` | Second run of the pair: the same arrivals and meals, and the same choices up to turn 3. From turn 4 the manager serves one customer at a time, so T2 is never used and the queue backs up. It also shows two rejected commands, each followed by a retry at the same prompt: `SERVE O3` before the Pasta is ready, and `PAY C3`, which is not a command. | C1 to C3 pay, revenue $42.00, average 65.3, DEFEAT |

The pair shows that manager choices alone change the outcome: run B leaves T2 empty, so C2 to C5 wait longer for a table and fewer customers are served within 18 turns. These transcripts are the Stage 1 reproducible runs for the report and the video.

### Manual play-through for the stage gate

After `scripts/replay.sh` passes on a fresh clone, one teammate plays a full game by hand and checks it against contract Sections 1 to 4 and the brief. The play-through covers the start-of-turn display, invalid commands and illegal targets being retried without using the task, the READY to SERVED to payment delays, a full table blocking seating, abandonment, and the final result. For the Stage 1 gate, two teammates each played a full game on 2026-10-18 and found no deviations; [#25](https://github.com/respoopu/SC2002-lab-assignment-final/pull/25) records the details.

## Team workflow

Track a coherent piece of work in each GitHub issue using the work-item form. Work on a branch, run `scripts/replay.sh`, open a pull request using the repository template and link it to the issue, then review and merge it. When every issue for a stage is merged, run the stage gate in the contract (replay plus a manual play-through), open fix issues for anything outstanding, and tag the passing commit. Next-stage PRs are not merged until that tag exists. Specify the shared Java API in the contract before assigning dependent implementation work.
