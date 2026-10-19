# SC2002 Restaurant Rush

Repository: https://github.com/respoopu/SC2002-lab-assignment-final

Java command-line restaurant management game for the SC2002 group assignment.

The [assignment brief](SC2002_Restaurant_Rush_Group_Assignment.docx.pdf) is the source of truth for the game rules and submission requirements.

The [component contract](docs/component-contract.md) records the team's agreed rules, responsibilities, turn timing, implementation decisions, and the shared Java API that every workstream issue builds against.

## Project status

Stage 2 is the current playable stage. It adds VIP and Critic customers to the complete base game and has reproducible victory and defeat runs. Stage 1 is preserved at the `stage-1` tag. Stage 3 work is tracked in the GitHub issues.

## Planned stages

| Tag | Required milestone |
| --- | --- |
| `stage-1` | Complete base game with Regular customers and both outcomes |
| `stage-2` | Add VIP and Critic customer behavior |
| `stage-3` | Add Host, ComboMeal, and Happy Hour |

Each tag will identify a tested, runnable commit. The Stage 1 section below has the build, run, and replay commands; the contract has the full rules (Sections 5 and 6). The Stage 1 to Stage 2 comparison is below. The Stage 3 comparison and video timestamps will be added at submission.

## Stage 1

The arrival rules and results in this section describe the `stage-1` tag. The build and command instructions also apply to Stage 2; see [Stage 2](#stage-2) for its customer types and runs.

### Build and run

You need JDK 17 or later. On Windows, run the commands from Git Bash.

```sh
javac --release 17 -d out $(find src -name '*.java')
java -cp out restaurantrush.game.Main
```

The game always plays 18 turns. Each turn prints any arrival, the status display, and the numbered legal actions, then asks for one Waiter command at `Waiter> ` and one Chef command at `Chef> `. Type a command, which is case-insensitive, or the number shown beside a legal action:

| Prompt | Commands | Numbers |
| --- | --- | --- |
| `Waiter> ` | `SEAT C<n>`, `ORDER C<n>`, `SERVE O<n>`, `WAIT` | `1` to the last number shown, `0` = `WAIT` |
| `Chef> ` | `PREPARE O<n>`, `WAIT` | `1` to the last number shown, `0` = `WAIT` |

For example, on turn 1 the Waiter sees `Legal actions: [1] SEAT C1, [0] WAIT`, and typing `1` seats C1. A number refers only to the list printed just above the prompt; a number that is not in it prints `That number is not in the list. Choose a number from the list, or type a command.` and asks again. The fixture `.in` files use text commands so they stay readable.

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
git checkout stage-1 && scripts/replay.sh fixtures/stage-1/victory.in  # replay one Stage 1 fixture at its tag
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

After `scripts/replay.sh` passes on a fresh clone, one teammate plays a full game by hand and checks it against contract Sections 1 to 4 and the brief. The play-through covers the start-of-turn display, invalid commands and illegal targets being retried without using the task, the READY to SERVED to payment delays, a full table blocking seating, abandonment, and the final result. For the Stage 1 gate, two teammates each played a full game on 2026-10-18 and found no deviations; [#25](https://github.com/respoopu/SC2002-lab-assignment-final/pull/25) records the details. After the numbered choices ([#27](https://github.com/respoopu/SC2002-lab-assignment-final/pull/27)), respoopu reran the gate the same day at `533490c`: an 18-turn checklist game typing numbers, text, and invalid input at both prompts, ending in VICTORY with $57.00 and average 78.0, and every line matched the expected output. A line-by-line check against the brief then found output gaps, recorded in [#29](https://github.com/respoopu/SC2002-lab-assignment-final/issues/29) and fixed in [#28](https://github.com/respoopu/SC2002-lab-assignment-final/pull/28): an out-of-range number and an empty line now say why they failed (brief 3.4), and the end-of-turn display shows turns used (brief 3.5). Replaying the same inputs on #28 gives the same game with only those lines changed; #28 lists the inputs.

## Stage 2

### Build and run

Use the [Stage 1 build, run, and command instructions](#build-and-run), unchanged. From this checkout, `java -cp out restaurantrush.game.Main` plays Stage 2. To return here after checking Stage 1, use `git checkout -` and rebuild. The game runs offline with JDK 17 or later.

The base setting remains 18 turns, two tables, one Waiter task, one Chef preparation unit, and at most one automatic Cashier payment per turn. Salad costs $9.00 and takes one preparation unit, Burger $15.00 and one unit, and Pasta $18.00 and two units. The Stage 1 win and lose criteria are unchanged: at least four paid customers, revenue of at least $45.00, and average satisfaction at payment of at least 60 gives Victory after turn 18; otherwise Defeat.

### Customer types and arrivals

Satisfaction loss applies at turn end while waiting for a table or seated without being served, including turns spent waiting for preparation. Satisfaction never falls below zero; reaching zero causes abandonment. Served, eating, and ready-to-pay customers lose nothing.

| Customer | Satisfaction loss per waiting turn | Bill | Special behaviour | Meal |
| --- | --- | --- | --- | --- |
| Regular | 8 | Full price | None | Salad, Burger, Pasta by 1-based arrival index, repeating |
| VIP | 5 | 10% off: Salad $8.10, Burger $13.50, Pasta $16.20 | None | Same cycle as Regular |
| Critic | 12 | Full price | A Critic always orders Pasta, whatever their arrival position (in the base setting C4 would otherwise get Salad), and loses 12 satisfaction per waiting turn, the fastest of the three types. | Always Pasta |

Two Regular arrivals were replaced by a VIP and a Critic; there are still six arrivals at the same turns.

| Customer | Arrives on turn | Type | Meal |
| --- | --- | --- | --- |
| C1 | 1 | Regular | Salad |
| C2 | 4 | VIP | Burger |
| C3 | 7 | Regular | Pasta |
| C4 | 10 | Critic | Pasta |
| C5 | 13 | Regular | Burger |
| C6 | 16 | Regular | Pasta |

All three types use the same seat, order, prepare, serve, eat, and pay flow, with each type supplying `waitingLossPerTurn`, `discountPercent`, `chooseMeal`, and `typeName` through its own methods. The turn controller, staff, and CLI perform no customer type checks: `GameSetup.arrival`, the composition root, is the only production code that selects the concrete customer class.

### Replay the reproducible runs

```sh
scripts/replay.sh                              # build, run all five self-checks, replay both Stage 2 fixtures
scripts/replay.sh fixtures/stage-2/vip-critic.in # replay one Stage 2 fixture
```

| Fixture | What it demonstrates | Result |
| --- | --- | --- |
| `vip-critic` | The brief Section 4.2 base-setting run includes a VIP and a Critic, both through payment. C2 orders Burger at $13.50 and loses 5 per seated, unserved turn: 95 then 90 after turns 4 and 5. C4 chooses Pasta where a Regular C4 would choose Salad, losing 12 per waiting or seated, unserved turn: 88, 76, 64, 52 after turns 10 to 13. C3 reproduces the Pasta example: seated on 7, ordered and prepared on 8, READY on 9, served on 10, ready to pay on 11, paid on 12 at satisfaction 76. | Five paid, revenue $73.50, average 75.6, VICTORY |
| `defeat` | Both staff always `WAIT`. Nobody pays and the average is `N/A` throughout. C1 leaves at the end of turn 13; Critic C4 leaves at the end of turn 18 after nine waiting turns (10 to 18). With the same input at Stage 1, C1 and C2 left on turns 13 and 16; Stage 2 VIP C2 is still waiting at satisfaction 25. | Zero paid, two unhappy departures, DEFEAT |

The `vip-critic` payments are:

| Customer | Payment turn | Bill | Satisfaction at payment |
| --- | --- | --- | --- |
| C1 | 5 | $9.00 | 84 |
| C2 | 8 | $13.50 | 90 |
| C3 | 12 | $18.00 | 76 |
| C4 | 16 | $18.00 | 52 |
| C5 | 18 | $15.00 | 76 |

C6 is still waiting at satisfaction 76 when the game ends: the turn 16 arrival cannot pay by turn 18, so at most five customers can pay. The `vip-critic` run also supplies the Stage 2 sequence-diagram scenario, including VIP pricing at order time, and the customer-behaviour video segment; its timestamp is added at submission.

### Stage 1 to Stage 2

The table lists the complete transition from `stage-1` to this Stage 2 branch, including these documentation changes. There are no deleted paths. After the README PR merges and the tag is created, `git diff --name-status stage-1 stage-2` must match it row for row.

| Path | Status | Reason | PR |
| --- | --- | --- | --- |
| `README.md` | Modified | Describe Stage 2 rules, runs, stage comparison, and gate status; correct the earlier-stage replay example. | [#32](https://github.com/respoopu/SC2002-lab-assignment-final/pull/32) |
| `fixtures/README.md` | Modified | Point readers to both stage sections for scenario details. | [#32](https://github.com/respoopu/SC2002-lab-assignment-final/pull/32) |
| `fixtures/stage-2/defeat.expected` | Added | Record the all-WAIT Stage 2 transcript, including the Critic's turn 18 departure. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `fixtures/stage-2/defeat.in` | Added | Supply the 36 WAIT commands for Stage 2 Defeat. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `fixtures/stage-2/vip-critic.expected` | Added | Record both new customer behaviours through payment and the $73.50 Victory. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `fixtures/stage-2/vip-critic.in` | Added | Supply the 36-command Stage 2 victory trace with turn notes. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `src/restaurantrush/check/CustomerCheck.java` | Modified | Check VIP and Critic behaviour, pricing, inherited lifecycle, and polymorphic dispatch. | [#30](https://github.com/respoopu/SC2002-lab-assignment-final/pull/30) |
| `src/restaurantrush/check/GameCheck.java` | Modified | Check the Stage 2 arrivals and both full-game traces while retaining the Stage 1 traces. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `src/restaurantrush/customer/CriticCustomer.java` | Added | Supply loss 12, full price, Pasta preference, and the Critic label through Customer methods. | [#30](https://github.com/respoopu/SC2002-lab-assignment-final/pull/30) |
| `src/restaurantrush/customer/VIPCustomer.java` | Added | Supply loss 5, 10% discount, cyclic meal choice, and the VIP label through Customer methods. | [#30](https://github.com/respoopu/SC2002-lab-assignment-final/pull/30) |
| `src/restaurantrush/game/GameSetup.java` | Modified | The registration change expected by brief Section 4.4: `arrival` builds a VIP for index 2 and a Critic for index 4. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |

[Compare Stage 1 to Stage 2](https://github.com/respoopu/SC2002-lab-assignment-final/compare/stage-1...stage-2). This link resolves once the `stage-2` tag is pushed.

The unchanged Stage 1 classes are `Customer`, `RegularCustomer`, `WaitingQueue`, `Waiter`, `Chef`, `Cashier`, `Table`, `Seating`, `TurnController`, `GameState`, all menu classes, `ConsoleUI`, `CommandParser`, and `Main`. Earlier behaviour remains covered by `GameCheck`'s Stage 1 traces on all-Regular arrivals; `git checkout stage-1 && scripts/replay.sh` replays the unchanged Stage 1 transcripts at their tag.

### Stage 2 gate

On 2026-10-19, `scripts/replay.sh` passed from a fresh clone of merged `main` at `063a13ff69f46665c524ba3980b8c026960f73b4`: all five self-checks (`CliCheck`, `CustomerCheck`, `GameCheck`, `MenuCheck`, `StaffCheck`) and exactly `fixtures/stage-2/defeat.in` and `fixtures/stage-2/vip-critic.in` passed, exit 0. The run used JDK 21 with the script's `--release 17` compilation target.

In the same clone, `git checkout stage-1 && scripts/replay.sh` also passed at `a40cdfd`: all five self-checks and all four Stage 1 fixtures, exit 0. The Stage 1 fixtures remain unchanged, and the Stage 2 tree contains no Host, ComboMeal, HappyHour, or Stage 3 fixture paths.

The play-through report is to be supplied separately in the README PR discussion. Before approval, a teammate must confirm the Stage 2 manual gate against brief Sections 2.1, 2.2, 3.1 to 3.5 and 4.2, including the player, date, tested commit, final result, and any deviations with issue links. The Stage 2 gate remains open until that confirmation and the post-merge checks are complete.

The README PR uses `Refs #15`. After an independent reviewer finds nothing outstanding and the PR merges, rerun replay from a fresh clone at its merge commit and at `stage-1`. Create the annotated `stage-2` tag on the merge commit, push only that tag, confirm the comparison matches the table and no Stage 3 files exist, then record the tag commit and replay result on issue #15 and close it. Until those steps are complete, the issue remains open.

## Team workflow

Track a coherent piece of work in each GitHub issue using the work-item form. Work on a branch, run `scripts/replay.sh`, open a pull request using the repository template and link it to the issue, then review and merge it. The Stage 2 gate issue [#15](https://github.com/respoopu/SC2002-lab-assignment-final/issues/15) uses `Refs #15` and stays open until its post-merge tag checks pass. When every issue for a stage is merged, run the stage gate in the contract (replay plus a manual play-through), open fix issues for anything outstanding, and tag the passing commit. Next-stage PRs are not merged until that tag exists. Specify the shared Java API in the contract before assigning dependent implementation work.
