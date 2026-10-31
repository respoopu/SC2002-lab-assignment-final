# SC2002 Restaurant Rush

Repository: https://github.com/respoopu/SC2002-lab-assignment-final

Java command-line restaurant management game for the SC2002 group assignment.

The [assignment brief](SC2002_Restaurant_Rush_Group_Assignment.docx.pdf) is the source of truth for the game rules and submission requirements.

The [component contract](docs/component-contract.md) records the team's agreed rules, responsibilities, turn timing, implementation decisions, and the shared Java API that every workstream issue builds against.

## Project status

Stage 3 is the final playable stage. It adds the Host, the Salad + Pasta combo, and Happy Hour to the Stage 2 game and has reproducible victory and defeat runs. Stage 1 and Stage 2 are preserved at the `stage-1` and `stage-2` tags. The final UML class and sequence diagrams are in [`docs/diagrams`](docs/diagrams). The `stage-3` tag is created after the [Stage 3 gate](#stage-3-gate) passes. The report, video, declarations, and submission package are separate from the stage tags.

## Stages

| Tag | Required milestone |
| --- | --- |
| `stage-1` | Complete base game with Regular customers and both outcomes |
| `stage-2` | Add VIP and Critic customer behavior |
| `stage-3` | Add Host, ComboMeal, and Happy Hour |

Each tag identifies a tested, runnable commit. `stage-1` and `stage-2` are fixed now that later-stage work has merged, and `stage-3` is never moved once it is pushed. The Stage 1 section below has the build, run, and replay commands; the contract has the full rules (Sections 5 and 6). The [Stage 1 to Stage 2](#stage-1-to-stage-2) and [Stage 2 to Stage 3](#stage-2-to-stage-3) comparisons list every changed path. Video timestamps are added at submission.

## Stage 1

The arrival rules and results in this section describe the `stage-1` tag. The build and command instructions also apply to Stages 2 and 3; see [Stage 2](#stage-2) for its customer types and runs, and [Stage 3](#stage-3) for the extra `Manager> ` and `Host> ` prompts.

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

The rules and results in this section describe the `stage-2` tag. Use the [Stage 1 build, run, and command instructions](#build-and-run), unchanged. After `git checkout stage-2` and a rebuild, `java -cp out restaurantrush.game.Main` plays Stage 2; from `main` it plays Stage 3. To return after checking an earlier tag, use `git checkout -` and rebuild. The game runs offline with JDK 17 or later.

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
git checkout stage-2 && scripts/replay.sh                                # build, run all five self-checks, replay both Stage 2 fixtures
git checkout stage-2 && scripts/replay.sh fixtures/stage-2/vip-critic.in # replay one Stage 2 fixture
```

| Fixture | What it demonstrates | Result |
| --- | --- | --- |
| `vip-critic` | The brief Section 4.2 base-setting run includes a VIP and a Critic, both through payment. C2 orders Burger at $13.50 and loses 5 per seated, unserved turn: 95 then 90 after turns 4 and 5. C4 chooses Pasta where a Regular C4 would choose Salad, losing 12 per waiting or seated, unserved turn: 88, 76, 64, 52 after turns 10 to 13. C3 reproduces the Pasta example: seated on 7, ordered on 8, READY on 9, served on 10, ready to pay on 11, paid on 12 at satisfaction 76. | Five paid, revenue $73.50, average 75.6, VICTORY |
| `defeat` | Both staff always `WAIT`. Nobody pays and the average is `N/A` throughout. C1 leaves at the end of turn 13; Critic C4 leaves at the end of turn 18 after nine waiting turns (10 to 18). With the same input at Stage 1, C1 and C2 left on turns 13 and 16; Stage 2 VIP C2 is still waiting at satisfaction 25. | Zero paid, two unhappy departures, DEFEAT |

The `vip-critic` payments are:

| Customer | Payment turn | Bill | Satisfaction at payment |
| --- | --- | --- | --- |
| C1 | 5 | $9.00 | 84 |
| C2 | 8 | $13.50 | 90 |
| C3 | 12 | $18.00 | 76 |
| C4 | 16 | $18.00 | 52 |
| C5 | 18 | $15.00 | 76 |

C6 is still waiting at satisfaction 76 when the game ends: the turn 16 arrival cannot pay by turn 18, so at most five customers can pay. The `vip-critic` run also shows VIP pricing at order time and supplies the customer-behaviour video segment; its timestamp is added at submission. The final sequence diagrams trace the Stage 3 run instead (see [Stage 3](#stage-3)).

### Stage 1 to Stage 2

The table lists the complete transition from `stage-1` to `stage-2`, including these documentation changes, and matches `git diff --name-status stage-1 stage-2` row for row. There are no deleted paths.

| Path | Status | Reason | PR |
| --- | --- | --- | --- |
| `README.md` | Modified | Describe Stage 2 rules, runs, stage comparison, and gate status; correct the earlier-stage replay example. | [#32](https://github.com/respoopu/SC2002-lab-assignment-final/pull/32), [#34](https://github.com/respoopu/SC2002-lab-assignment-final/pull/34) |
| `docs/component-contract.md` | Modified | Allow a stage tag to move to a gate-fix merge commit before any next-stage PR merges (Section 5, Stage gate step 6), and record the `Waiter` gate fix in the Section 7.8 Stage 2 row. | [#34](https://github.com/respoopu/SC2002-lab-assignment-final/pull/34) |
| `fixtures/README.md` | Modified | Point readers to both stage sections for scenario details. | [#32](https://github.com/respoopu/SC2002-lab-assignment-final/pull/32) |
| `fixtures/stage-2/defeat.expected` | Added | Record the all-WAIT Stage 2 transcript, including the Critic's turn 18 departure. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `fixtures/stage-2/defeat.in` | Added | Supply the 36 WAIT commands for Stage 2 Defeat. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `fixtures/stage-2/vip-critic.expected` | Added | Record both new customer behaviours through payment and the $73.50 Victory. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `fixtures/stage-2/vip-critic.in` | Added | Supply the 36-command Stage 2 victory trace with turn notes. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `src/restaurantrush/check/CustomerCheck.java` | Modified | Check VIP and Critic behaviour, pricing, inherited lifecycle, and polymorphic dispatch. | [#30](https://github.com/respoopu/SC2002-lab-assignment-final/pull/30) |
| `src/restaurantrush/check/GameCheck.java` | Modified | Check the Stage 2 arrivals and both full-game traces while retaining the Stage 1 traces. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `src/restaurantrush/check/StaffCheck.java` | Modified | Check the `ORDER` rejection reasons, including customers who are eating or ready to pay. | [#34](https://github.com/respoopu/SC2002-lab-assignment-final/pull/34) |
| `src/restaurantrush/customer/CriticCustomer.java` | Added | Supply loss 12, full price, Pasta preference, and the Critic label through Customer methods. | [#30](https://github.com/respoopu/SC2002-lab-assignment-final/pull/30) |
| `src/restaurantrush/customer/VIPCustomer.java` | Added | Supply loss 5, 10% discount, cyclic meal choice, and the VIP label through Customer methods. | [#30](https://github.com/respoopu/SC2002-lab-assignment-final/pull/30) |
| `src/restaurantrush/game/GameSetup.java` | Modified | The registration change expected by brief Section 4.4: `arrival` builds a VIP for index 2 and a Critic for index 4. | [#31](https://github.com/respoopu/SC2002-lab-assignment-final/pull/31) |
| `src/restaurantrush/staff/Waiter.java` | Modified | A Stage 2 gate fix for brief Section 3.4: `takeOrder` checks for an existing order before the SEATED status, so `ORDER` on a customer who is eating or ready to pay says they have already ordered instead of that they are not seated. | [#34](https://github.com/respoopu/SC2002-lab-assignment-final/pull/34) |

[Compare Stage 1 to Stage 2](https://github.com/respoopu/SC2002-lab-assignment-final/compare/stage-1...stage-2).

The unchanged Stage 1 classes include `Customer`, `RegularCustomer`, `WaitingQueue`, `Chef`, `Cashier`, `Table`, `Seating`, `TurnController`, `GameState`, all menu classes, `ConsoleUI`, `CommandParser`, and `Main`. Earlier behaviour remains covered by `GameCheck`'s Stage 1 traces on all-Regular arrivals; `git checkout stage-1 && scripts/replay.sh` replays the unchanged Stage 1 transcripts at their tag.

### Stage 2 gate

On 2026-10-19, `scripts/replay.sh` passed from a fresh clone of merged `main` at `063a13ff69f46665c524ba3980b8c026960f73b4`: all five self-checks (`CliCheck`, `CustomerCheck`, `GameCheck`, `MenuCheck`, `StaffCheck`) and exactly `fixtures/stage-2/defeat.in` and `fixtures/stage-2/vip-critic.in` passed, exit 0. The run used JDK 21 with the script's `--release 17` compilation target.

In the same clone, `git checkout stage-1 && scripts/replay.sh` also passed at `a40cdfd`: all five self-checks and all four Stage 1 fixtures, exit 0. The Stage 1 fixtures remain unchanged, and the Stage 2 tree contains no Host, ComboMeal, HappyHour, or Stage 3 fixture paths.

For the manual play-through, cknisaac played a full Stage 2 game by hand on 2026-10-19 at `063a13f`. It ran all 18 turns and ended in VICTORY: five paid, revenue $73.50, average 75.6, and no unhappy departures. C2 appeared as VIP, ordered Burger at $13.50, and lost 5 per waiting turn; C4 appeared as Critic, ordered Pasta at $18.00, and lost 12 per waiting turn. Preparation, serving, eating, and payment happened in order, and invalid commands, unfinished orders, and seating with both tables full each gave a clear error and re-prompted without using the staff task. cknisaac reported no deviations.

The README PR [#32](https://github.com/respoopu/SC2002-lab-assignment-final/pull/32) merged as `085fa38`, which also closed [#15](https://github.com/respoopu/SC2002-lab-assignment-final/issues/15) through the branch link. The fresh-clone replay passed there and at `stage-1`, and `stage-2` was first tagged on that commit. A final check of the tag against the brief and #15 then found two deviations, recorded in [#33](https://github.com/respoopu/SC2002-lab-assignment-final/issues/33): `ORDER` on a customer who was eating or ready to pay said `C<n> is not seated.` (brief 3.4), and the earlier version of this paragraph still described #15 as open. [#34](https://github.com/respoopu/SC2002-lab-assignment-final/pull/34) fixed both and added contract Section 5, Stage gate step 6, which allows a stage tag to move before any next-stage PR merges. `stage-2` was then moved to the #34 merge commit after the fresh-clone replay passed there and at `stage-1`, and #15 records both tag placements. The `stage-1` tag is unchanged, so Stage 1 at its tag still prints `C<n> is not seated.` in that case.

## Stage 3

### Build and run

Build and run exactly as in [Stage 1](#build-and-run); from `main`, `java -cp out restaurantrush.game.Main` plays Stage 3. The base setting, the six arrival turns, the VIP and Critic rules, the service flow, and the win and lose criteria are all unchanged from Stage 2.

A Stage 3 turn has up to four prompts. Before each turn, until Happy Hour has been used, the game prints `Happy Hour available: HAPPY_HOUR or SKIP` and asks at `Manager> `. The turn then starts, the start-of-turn display adds `Happy Hour remaining: <n>` below the average, and the Host, the Waiter, and the Chef are asked in that order:

| Prompt | Commands | Numbers |
| --- | --- | --- |
| `Manager> ` | `HAPPY_HOUR` activates Happy Hour for this turn and the next; `SKIP` does not | None; type the word |
| `Host> ` | `SEAT C<n>`, `WAIT` | `1` to the last number shown, `0` = `WAIT` |
| `Waiter> ` | `SEAT C<n>`, `ORDER C<n>`, `SERVE O<n>`, `WAIT` | As in Stage 1 |
| `Chef> ` | `PREPARE O<n>`, `WAIT` | As in Stage 1 |

Answers are case-insensitive. Any other answer at `Manager> ` prints `Invalid manager command. Use HAPPY_HOUR or SKIP.` and asks again without starting the turn. Invalid commands at the staff prompts are retried without using the task, as in Stage 1, and the end-of-input message and exit status 2 are unchanged.

### Host

The Host acts first each turn and seats at most one queued customer at the lowest-numbered free table, or waits. Its legal actions are `SEAT` for each queued customer, in queue order, while a table is free, then `WAIT`. The Waiter keeps the ability to seat, so the Host and the Waiter can seat two customers in one turn when both tables are free. Because the Host acts before the Waiter (contract Section 6), the Waiter can take the order of a customer the Host seated that turn. The Host and the Waiter share the same seating rule through the package-private `Seating` helper, so seating with both tables full is rejected the same way for both.

### Combo meal and meal choice

The Stage 3 menu adds the `Salad + Pasta combo` to Salad, Burger, and Pasta. A combo is built from two existing menu items: its preparation units are their sum and its price is 90% of their combined price, rounded half up to the cent. The Salad + Pasta combo is ($9.00 + $18.00) x 0.9 = $24.30 and takes 3 units, so the Chef needs three turns to make it READY.

A Regular customer whose 1-based arrival index is divisible by 3 (C3 and C6) chooses the combo; other Regular customers keep the Salad, Burger, Pasta cycle. VIP and Critic choices are unchanged: the VIP follows the cycle and the Critic always orders Pasta.

| Customer | Arrives on turn | Type | Stage 3 meal |
| --- | --- | --- | --- |
| C1 | 1 | Regular | Salad |
| C2 | 4 | VIP | Burger |
| C3 | 7 | Regular | Salad + Pasta combo |
| C4 | 10 | Critic | Pasta |
| C5 | 13 | Regular | Burger |
| C6 | 16 | Regular | Salad + Pasta combo |

### Happy Hour

- **When:** the manager may activate Happy Hour once per game, at the `Manager> ` prompt before any turn from 1 to 18. After it is used, the prompt no longer appears.
- **Duration:** it is active on the activation turn and the next. The display shows `Happy Hour remaining: 2` on the activation turn, `1` on the next turn, and `0` on every other Stage 3 turn. Activated on turn 18, it lasts only that turn because the game ends, although the display still shows `2`.
- **Recovery:** at the start of each active turn, after activation and before that turn's arrival, every customer still waiting for a table or for food (`WAITING` or `SEATED`, including while their order is being prepared) recovers 15 satisfaction, capped at 100. The game prints `Happy Hour activated` on the activation turn and `C<n> recovers <amount> satisfaction (<new value>)` for each customer whose satisfaction rises; a customer already at 100 gets no line. Eating, ready-to-pay, paid, and departed customers do not recover, and the customer arriving that turn starts at 100.
- **Discount order:** an order taken while Happy Hour is active gets a further 20% off. The bill starts from the menu price (for the combo, the 90% combo price), applies the customer's own discount (10% for a VIP), then the Happy Hour discount, rounding half up to the cent after each step. A VIP Burger is $15.00 x 0.9 x 0.8 = $10.80, the brief's example; a Regular customer's combo is $24.30 x 0.8 = $19.44.
- **Locked bills:** the bill is fixed when the Waiter takes the order. An order taken before activation keeps its price even if it is served or paid during Happy Hour, and an order taken after Happy Hour ends pays the normal price.

These rules are owned by the classes that hold the state: `GameState` records the activation turn and reports the remaining duration, `TurnController` activates and asks each customer to `recover`, `Customer.recover` decides eligibility, and the `Order` constructor locks the discounted bill. The CLI only asks the question and displays the result.

### Replay the reproducible runs

```sh
scripts/replay.sh                                           # build, run all five self-checks, replay both Stage 3 fixtures
scripts/replay.sh fixtures/stage-3/host-combo-happy-hour.in # replay one Stage 3 fixture
git checkout stage-2 && scripts/replay.sh                   # replay Stage 2 at its tag
git checkout stage-1 && scripts/replay.sh                   # replay Stage 1 at its tag
git checkout main                                           # return to Stage 3
```

`scripts/replay.sh` replays only the highest-numbered fixture directory, `fixtures/stage-3`, because Stage 3 adds the `Manager> ` and `Host> ` prompts to every transcript. The Stage 1 and Stage 2 fixtures stay unchanged in the tree and pass at their own tags. Each Stage 3 `.in` file has one line per prompt in order, with a comment for every turn.

| Fixture | What it demonstrates | Result |
| --- | --- | --- |
| `host-combo-happy-hour` | The Host does all the seating, which leaves the Waiter free to take the orders of C1 to C3 on the turn they are seated and to serve O3 and O4 on the turns C4 and C5 are seated. Happy Hour is activated on turn 7 (remaining 2 on turn 7, 1 on turn 8, 0 from turn 9). C3 orders the Salad + Pasta combo during Happy Hour for $19.44 instead of $24.30, recovers 8 satisfaction to 100 at the start of turn 8, and the combo takes three Chef turns (7, 8, 9). Bills are locked at order time: VIP C2's Burger, ordered on turn 4, is $13.50 although C2 pays during Happy Hour on turn 7, and Critic C4's Pasta, ordered on turn 11 after Happy Hour ends, is $18.00. | Five paid, revenue $74.94, average 83.8, VICTORY |
| `defeat` | `SKIP` at every `Manager> ` prompt and `WAIT` for the Host, the Waiter, and the Chef, so Happy Hour is offered before all 18 turns and never used. Nobody pays and the average is `N/A` throughout. C1 leaves at the end of turn 13 and Critic C4 at the end of turn 18; C2, C3, C5, and C6 end waiting at 25, 4, 52, and 76. | Zero paid, two unhappy departures, DEFEAT |

The `host-combo-happy-hour` payments are:

| Customer | Payment turn | Bill | Satisfaction at payment |
| --- | --- | --- | --- |
| C1 | 4 | $9.00 | 92 |
| C2 | 7 | $13.50 | 95 |
| C3 | 12 | $19.44 | 84 |
| C4 | 15 | $18.00 | 64 |
| C5 | 17 | $15.00 | 84 |

The satisfaction sum is 419, so the average is 83.8. C6 arrives on turn 16 and is still waiting at 76 when the game ends. The two fixtures use the same setting, arrivals, and meal rules, so they are also the Stage 3 pair in which manager choices alone change the result. The C3 path in `host-combo-happy-hour` is the scenario of the [sequence diagrams](docs/diagrams).

### Stage 2 to Stage 3

The table lists the complete transition from `stage-2` to `stage-3`, including this README, and matches `git diff --name-status stage-2 stage-3` row for row. There are no deleted paths. Before the tag exists, compare with `git diff --name-status stage-2 HEAD`.

| Path | Status | Reason | PR |
| --- | --- | --- | --- |
| `README.md` | Modified | Describe the Stage 3 rules, runs, comparison, and gate, and point the Stage 2 run and replay instructions at the `stage-2` tag. | [#39](https://github.com/respoopu/SC2002-lab-assignment-final/pull/39) |
| `docs/component-contract.md` | Modified | Specify the Stage 3 shared Java API and issue waves (Sections 7.10 to 7.12), and let the README/stage-gate PR use `Refs` so its issue stays open until the tag is pushed (Section 5, Stage gate step 1). | [#24](https://github.com/respoopu/SC2002-lab-assignment-final/pull/24) |
| `docs/diagrams/class-control.puml` | Added | Class diagram source: CLI boundary, `TurnController`, and staff, with associations and multiplicities. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/class-domain.puml` | Added | Class diagram source: customers, menu items, `ComboMeal`, and orders, with inheritance and composition. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/class-state.puml` | Added | Class diagram source: `GameState` ownership, snapshot and event values, and the enums. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/class-control.png` | Added | Rendered image of `class-control.puml` for the report. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/class-control.svg` | Added | Rendered vector image of `class-control.puml`. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/class-domain.png` | Added | Rendered image of `class-domain.puml` for the report. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/class-domain.svg` | Added | Rendered vector image of `class-domain.puml`. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/class-state.png` | Added | Rendered image of `class-state.puml` for the report. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/class-state.svg` | Added | Rendered vector image of `class-state.puml`. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-1-game.png` | Added | Rendered image of `sequence-1-game.puml` for the report. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-1-game.svg` | Added | Rendered vector image of `sequence-1-game.puml`. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-2-arrival.png` | Added | Rendered image of `sequence-2-arrival.puml` for the report. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-2-arrival.svg` | Added | Rendered vector image of `sequence-2-arrival.puml`. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-3-order.png` | Added | Rendered image of `sequence-3-order.puml` for the report. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-3-order.svg` | Added | Rendered vector image of `sequence-3-order.puml`. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-4-service.png` | Added | Rendered image of `sequence-4-service.puml` for the report. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/exports/sequence-4-service.svg` | Added | Rendered vector image of `sequence-4-service.puml`. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/render.sh` | Added | Regenerate every export locally with a pinned PlantUML version, keeping the previous exports if a render fails. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/sequence-1-game.puml` | Added | Sequence diagram source: the 18-turn loop of manager and staff choices and the Victory or Defeat result check. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/sequence-2-arrival.puml` | Added | Sequence diagram source: turn 7, with Happy Hour activation, C3's arrival, and the Host seating C3. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/sequence-3-order.puml` | Added | Sequence diagram source: turn 7, with C3's combo choice, the Happy Hour price locked at order time, and the first preparation unit. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/sequence-4-service.puml` | Added | Sequence diagram source: C3's recovery on turn 8, the combo becoming READY, then serving, eating, and payment on turn 12. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `docs/diagrams/style.iuml` | Added | Shared diagram styling, with comments indexing the views, the methods, and the fixture they trace. | [#38](https://github.com/respoopu/SC2002-lab-assignment-final/pull/38) |
| `fixtures/README.md` | Modified | Describe the Stage 3 input shape (the `Manager> ` and `Host> ` lines) and the two Stage 3 fixtures, and link the README's Stage 3 section. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37), [#39](https://github.com/respoopu/SC2002-lab-assignment-final/pull/39) |
| `fixtures/stage-3/defeat.expected` | Added | Record the all-`SKIP`, all-`WAIT` Stage 3 transcript. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `fixtures/stage-3/defeat.in` | Added | Supply the 18 `SKIP` and 54 `WAIT` commands for Stage 3 Defeat. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `fixtures/stage-3/host-combo-happy-hour.expected` | Added | Record the Host, combo, and Happy Hour victory transcript. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `fixtures/stage-3/host-combo-happy-hour.in` | Added | Supply the 61-command Stage 3 victory trace with turn notes. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/check/CliCheck.java` | Modified | Check `Host> ` and `Manager> ` parsing, the remaining-duration line, a Stage 3 run, and end of input at the new prompts; run the two-role checks on an explicit Stage 1/2 controller and assert they show no Stage 3 text. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/check/CustomerCheck.java` | Modified | Check recovery, its cap and ineligible statuses, the combo choice, and that VIP and Critic ignore the combo. | [#36](https://github.com/respoopu/SC2002-lab-assignment-final/pull/36) |
| `src/restaurantrush/check/GameCheck.java` | Modified | Check the Stage 3 setup, the Host phase, Host and Waiter seating in one turn, Happy Hour activation, pricing, recovery and turn-18 rules, and both full Stage 3 traces, while keeping the Stage 1 and Stage 2 traces. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/check/MenuCheck.java` | Modified | Check combo price, units and validation, and the Happy Hour bill, including the $10.80 VIP Burger. | [#35](https://github.com/respoopu/SC2002-lab-assignment-final/pull/35) |
| `src/restaurantrush/check/StaffCheck.java` | Modified | Check Host seating, rejections, and legal actions, and Host and Waiter seating different customers. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/cli/CommandParser.java` | Modified | Parse the `Manager> ` answer (`parseHappyHour`) and allow only `SEAT` and `WAIT` for the Host. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/cli/ConsoleUI.java` | Modified | Ask `Manager> ` while Happy Hour is available, follow the controller's current role instead of a fixed Waiter-then-Chef loop, add the `Host> ` prompt, and show the remaining duration only on Stage 3 turns. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/customer/Customer.java` | Modified | Add `recover`, because satisfaction is customer-owned state and the customer decides who is still waiting for a table or food. | [#36](https://github.com/respoopu/SC2002-lab-assignment-final/pull/36) |
| `src/restaurantrush/customer/RegularCustomer.java` | Modified | The documented combo rule: indices divisible by 3 choose the combo when the menu offers it, so the Stage 1 and 2 menu gives the same choices as before. | [#36](https://github.com/respoopu/SC2002-lab-assignment-final/pull/36) |
| `src/restaurantrush/game/GameSetup.java` | Modified | The registration change expected by brief Section 4.4: `stage3Menu` adds the combo, and `baseGame` builds the game with the Host and that menu. `arrival` is unchanged. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/game/GameState.java` | Modified | Store the Happy Hour activation turn and report whether it is used, active, and how many turns remain. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/game/Phase.java` | Modified | Add the `HOST` phase before `WAITER`. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/game/Snapshot.java` | Modified | Carry Happy Hour availability and remaining duration to the CLI; the old constructor remains and fills in `false` and `0`. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/game/StaffRole.java` | Modified | Add the `HOST` role. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/game/TurnController.java` | Modified | Add the Host phase, Happy Hour activation and recovery at turn start, and a constructor taking a `Host`; the old constructor keeps the Stage 1/2 flow. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/menu/ComboMeal.java` | Added | A `MenuItem` made of two existing items, with summed units and 90% of their combined price. | [#35](https://github.com/respoopu/SC2002-lab-assignment-final/pull/35) |
| `src/restaurantrush/menu/Order.java` | Modified | A constructor overload that applies the 20% Happy Hour discount after the personal discount and locks the bill; the old constructor delegates with Happy Hour off. | [#35](https://github.com/respoopu/SC2002-lab-assignment-final/pull/35) |
| `src/restaurantrush/staff/Host.java` | Added | The Host: legal `SEAT` actions while a table is free, and seating through the shared `Seating` helper. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |
| `src/restaurantrush/staff/Waiter.java` | Modified | Pass whether Happy Hour is active when taking an order, so the bill is locked at the right price. | [#37](https://github.com/respoopu/SC2002-lab-assignment-final/pull/37) |

[Compare Stage 2 to Stage 3](https://github.com/respoopu/SC2002-lab-assignment-final/compare/stage-2...stage-3). The link resolves once the `stage-3` tag is pushed.

**Why earlier classes changed (brief Section 4.4).** Every Stage 1 or 2 game class that changed is planned in contract Sections 7.8 and 7.10, and the self-check additions in Section 7.12. Each change is the smallest one that gives the new behaviour to the class that owns the state. `Phase` and `StaffRole` gain the Host, `GameState` and `Snapshot` gain the Happy Hour fields, and `TurnController` gains the Host phase and the turn-start activation and recovery. `Customer` and `RegularCustomer` gain recovery and the combo choice, and `Order` and `Waiter` lock the Happy Hour price at order time. `GameSetup` registers the Host and the combo, and `CommandParser` and `ConsoleUI` add the two prompts. No Stage 1 or 2 game rule was rewritten. Two methods were restructured without changing their output: `ConsoleUI.run` now asks whichever role the controller names instead of looping over a fixed Waiter and Chef list, and `TurnController` hands each action to its role's staff member with the same rejection messages. Every other change adds an overload or a branch beside the old behaviour: the old `TurnController`, `Order`, and `Snapshot` constructors and `beginTurn()` still work and give the Stage 1/2 flow with no Host and no Happy Hour.

**Earlier behaviour still works.** `GameCheck` runs the Stage 1 and Stage 2 full-game traces on controllers built with the old constructor and `Menu.base()`, and passes. #37 also replayed all six Stage 1 and Stage 2 fixture inputs on such controllers and got output byte-identical to the committed transcripts. `git checkout stage-1 && scripts/replay.sh` and `git checkout stage-2 && scripts/replay.sh` replay each stage's own transcripts at its tag.

**Unchanged classes.** `Menu`, `MenuItem`, `Money`, `OrderStatus`, `VIPCustomer`, `CriticCustomer`, `CustomerStatus`, `WaitingQueue`, `Chef`, `Cashier`, `Table`, `Seating`, `Main`, `GameConfig`, `Action`, `ActionKind`, `ActionResult`, `CustomerView`, `GameEvent`, `GameResult`, `OrderView`, `TableView`, `TurnStart`, `TurnEnd`, and `TurnSummary` are unchanged. `Menu.base()` still has only the three base items, and `GameSetup.arrival` still follows the Stage 2 arrival table.

**Material deviations.** None from the brief or from contract Sections 4, 6, 7.8, and 7.10. A line-by-line check of the Stage 3 game against brief Sections 3, 4.2, and 4.3, made on 2026-10-31 before this README PR opened, found no violations; the README PR lists the output-wording notes it raised. The shared `Seating` helper that 7.8 asks the Host to use already existed for the Waiter, so it did not change.

### Stage 3 gate

Before this README merged, `scripts/replay.sh` passed from a fresh clone of `main` at `bc0b691` (the #38 merge): all five self-checks and exactly `fixtures/stage-3/defeat.in` and `fixtures/stage-3/host-combo-happy-hour.in`, exit 0. In the same clone, `git checkout stage-2 && scripts/replay.sh` passed with both Stage 2 fixtures, and `git checkout stage-1 && scripts/replay.sh` passed with all four Stage 1 fixtures.

The post-merge gate is recorded on [#21](https://github.com/respoopu/SC2002-lab-assignment-final/issues/21): the fresh-clone replay on the merge commit and at both earlier tags, a teammate's manual 18-turn play-through against the brief with its date and any deviations, a check that the diagrams match the final source, and the `stage-3` tag. The team tags the tested commit `stage-3`, pushes the tag, and checks that `git rev-parse stage-3` is that commit and that the comparison link above resolves. Any deviation becomes a fix issue; its PR updates the comparison above, and the gate is repeated before tagging. The `stage-1` and `stage-2` tags do not move, and `stage-3` is not moved once pushed. #21 is closed by hand after this evidence and the pushed tag are recorded.

## Team workflow

Track a coherent piece of work in each GitHub issue using the work-item form. Work on a branch, run `scripts/replay.sh`, open a pull request using the repository template and link it to the issue, then review and merge it. When every issue for a stage is merged, run the stage gate in the contract (replay plus a manual play-through), open fix issues for anything outstanding, and tag the passing commit. Next-stage PRs are not merged until that tag exists. Specify the shared Java API in the contract before assigning dependent implementation work.
