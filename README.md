# SC2002 Restaurant Rush

Repository: `https://github.com/respoopu/SC2002-lab-assignment-final`

A Java command-line restaurant management game for the SC2002 group assignment. The player is the manager and decides what the staff do each turn.

## Contents

- [Project status](#project-status)
- [Stage 1](#stage-1)
  - [Build and run](#build-and-run)
  - [Arrivals and meals](#arrivals-and-meals)
  - [Reproducible runs](#reproducible-runs)
  - [Testing](#testing)
- [Stage 2](#stage-2)
  - [Customer types](#customer-types)
  - [Reproducible runs](#reproducible-runs-1)
  - [Stage 1 to Stage 2](#stage-1-to-stage-2)
  - [Testing](#testing-1)
- [Stage 3](#stage-3)
  - [Build and run](#build-and-run-1)
  - [Host](#host)
  - [Combo meal](#combo-meal)
  - [Happy Hour](#happy-hour)
  - [Reproducible runs](#reproducible-runs-2)
  - [Stage 2 to Stage 3](#stage-2-to-stage-3)
  - [Testing](#testing-2)
- [Video timestamps](#video-timestamps)

## Project status

The game was built in three stages, and each one is tagged in GitHub:

| Tag | What it adds |
| --- | --- |
| `stage-1` | The complete base game with Regular customers, a victory and a defeat |
| `stage-2` | VIP and Critic customers |
| `stage-3` | The Host, the Salad + Pasta combo and Happy Hour |

Stage 3 is the final version, and the `stage-3` tag points at the final code. Near the end of Stage 3, the console output was redesigned to be easier to read. That update only changed how the output looks: the game rules, the order of events and the results stayed the same.

The UML class and sequence diagrams are in `docs/diagrams`. The full game rules and the team's design decisions are in `docs/component-contract.md`.

## Stage 1

### Build and run

You need JDK 17 or later. On Windows, run the commands from Git Bash.

```sh
javac --release 17 -d out $(find src -name '*.java')
java -cp out restaurantrush.game.Main
```

On `main` this plays Stage 3. To play an earlier stage, check out its tag first and build again, for example `git checkout stage-1`. Use `git checkout main` to return to Stage 3. The rest of this section describes the game at the `stage-1` tag.

The game always lasts 18 turns. Each turn shows any new arrival, the restaurant status and the numbered actions available. It then asks the Waiter for one command at `Waiter> ` and the Chef for one command at `Chef> `. Type either the command or its number. Commands are not case-sensitive.

| Prompt | Commands | Numbers |
| --- | --- | --- |
| `Waiter> ` | `SEAT C<n>`, `ORDER C<n>`, `SERVE O<n>`, `WAIT` | `1` and up for the listed actions, `0` for `WAIT` |
| `Chef> ` | `PREPARE O<n>`, `WAIT` | `1` and up for the listed actions, `0` for `WAIT` |

For example, on turn 1 the Waiter sees `[1] SEAT C1` and `[0] WAIT`, and typing `1` seats C1. A number only refers to the list shown just above the prompt.

If a command is invalid or not allowed, the game explains why and asks again, and the staff member does not lose their turn. The Cashier works automatically and takes at most one payment per turn. If the input ends before the game finishes, the game prints `Input ended before the game finished.` and exits with status 2.

### Arrivals and meals

One Regular customer arrives on turns 1, 4, 7, 10, 13 and 16, named `C1` to `C6` in order. Each customer picks their own meal based on their arrival number, going Salad, Burger, Pasta and repeating. The manager does not choose the food.

| Customer | Arrives on turn | Meal |
| --- | --- | --- |
| C1 | 1 | Salad ($9.00, 1 unit) |
| C2 | 4 | Burger ($15.00, 1 unit) |
| C3 | 7 | Pasta ($18.00, 2 units) |
| C4 | 10 | Salad |
| C5 | 13 | Burger |
| C6 | 16 | Pasta |

Orders are numbered `O1`, `O2` and so on in the order they are taken. The two tables are `T1` and `T2`.

### Reproducible runs

```sh
scripts/replay.sh                                   # build, run the self-checks and replay the current stage's runs
git checkout stage-1 && scripts/replay.sh           # replay all the Stage 1 runs
scripts/replay.sh fixtures/stage-1/victory.in       # replay one run (at the stage-1 tag)
```

The script prints `PASS` or `FAIL` for each check and each run. Each run in `fixtures/stage-1/` has a `.in` file with the manager's commands and a comment for every turn, and a `.expected` file with the game's full output. Use the script rather than piping the `.in` file in directly, because the script removes the comments first.

| Run | What it shows | Result |
| --- | --- | --- |
| `victory` | A full game that uses both tables. C3 follows the brief's Pasta example: seated on turn 7, ordered on turn 8, READY on turn 9, served on turn 10, ready to pay on turn 11, and paid $18.00 at satisfaction 76 on turn 12. | C1 to C5 pay, $66.00 revenue, average 79.2, VICTORY |
| `defeat` | The Waiter and the Chef wait every turn. Nobody pays, and C1 and C2 leave unhappy at the end of turns 13 and 16. | Nobody pays, DEFEAT |
| `prioritization-a` | The same choices as `victory`: while one customer eats or waits to pay, the Waiter seats the next customer at the free table. | VICTORY |
| `prioritization-b` | The same arrivals and the same choices up to turn 3. From turn 4 the manager serves only one customer at a time, so T2 is never used and the queue builds up. It also shows two rejected commands being retried. | C1 to C3 pay, $42.00 revenue, average 65.3, DEFEAT |

The last two runs show that the manager's choices alone change the result: leaving T2 empty makes customers wait longer, so fewer are served within 18 turns.

### Testing

Before tagging, the team ran `scripts/replay.sh` on a fresh copy of the repository, and two teammates each played a full game by hand. The play-throughs checked the turn display, retrying invalid commands, the READY to SERVED to payment steps, seating when both tables are full, customers leaving, and the final result. No problems were found with the game rules. A few unclear error messages were then improved, and the end-of-turn display now shows how many turns have been used.

## Stage 2

### Customer types

Stage 2 keeps the same setting and the same win and lose rules as Stage 1. The player wins after turn 18 with at least four paying customers, at least $45.00 revenue and an average satisfaction of at least 60. Otherwise the player loses.

Customers lose satisfaction at the end of each turn while they are waiting for a table or sitting without their food. Satisfaction never goes below zero, and a customer who reaches zero leaves. Customers who have been served lose nothing.

| Customer | Satisfaction lost per waiting turn | Bill | Meal |
| --- | --- | --- | --- |
| Regular | 8 | Full price | Salad, Burger, Pasta by arrival number, repeating |
| VIP | 5 | 10% off (Salad $8.10, Burger $13.50, Pasta $16.20) | Same as Regular |
| Critic | 12 | Full price | Always Pasta |

Two of the Regular customers are replaced by a VIP and a Critic. There are still six arrivals on the same turns.

| Customer | Arrives on turn | Type | Meal |
| --- | --- | --- | --- |
| C1 | 1 | Regular | Salad |
| C2 | 4 | VIP | Burger |
| C3 | 7 | Regular | Pasta |
| C4 | 10 | Critic | Pasta |
| C5 | 13 | Regular | Burger |
| C6 | 16 | Regular | Pasta |

All three types go through the same seat, order, prepare, serve, eat and pay steps. Each type provides its own satisfaction loss, discount, meal choice and name through its own methods, so the turn controller, staff and CLI never check what type a customer is. `GameSetup.arrival` is the only place that decides which type of customer to create.

### Reproducible runs

```sh
git checkout stage-2 && scripts/replay.sh                                 # replay both Stage 2 runs
git checkout stage-2 && scripts/replay.sh fixtures/stage-2/vip-critic.in  # replay one run
```

| Run | What it shows | Result |
| --- | --- | --- |
| `vip-critic` | A VIP and a Critic both go through to payment. VIP C2 pays $13.50 for a Burger and loses 5 per waiting turn. Critic C4 orders Pasta even though a Regular C4 would order Salad, and loses 12 per waiting turn. | Five pay, $73.50 revenue, average 75.6, VICTORY |
| `defeat` | Both staff wait every turn. Nobody pays. C1 leaves at the end of turn 13, and Critic C4 leaves at the end of turn 18. | Nobody pays, DEFEAT |

The payments in `vip-critic` are:

| Customer | Payment turn | Bill | Satisfaction at payment |
| --- | --- | --- | --- |
| C1 | 5 | $9.00 | 84 |
| C2 | 8 | $13.50 | 90 |
| C3 | 12 | $18.00 | 76 |
| C4 | 16 | $18.00 | 52 |
| C5 | 18 | $15.00 | 76 |

C6 arrives on turn 16 and cannot pay before the game ends, so at most five customers can pay.

### Stage 1 to Stage 2

Compare: `https://github.com/respoopu/SC2002-lab-assignment-final/compare/stage-1...stage-2`, or run `git diff --name-status stage-1 stage-2`. No files were deleted.

| File | Status | Reason |
| --- | --- | --- |
| `src/restaurantrush/customer/VIPCustomer.java` | Added | The VIP: loses 5 per turn, gets 10% off, and chooses meals like a Regular. |
| `src/restaurantrush/customer/CriticCustomer.java` | Added | The Critic: loses 12 per turn, pays full price, and always orders Pasta. |
| `src/restaurantrush/game/GameSetup.java` | Modified | A small setup change: the second customer is now a VIP and the fourth a Critic. |
| `src/restaurantrush/staff/Waiter.java` | Modified | A bug fix found while testing Stage 2. `ORDER` on a customer who is already eating or ready to pay now says they have already ordered, instead of saying they are not seated. |
| `src/restaurantrush/check/CustomerCheck.java` | Modified | Check the VIP and Critic rules and pricing. |
| `src/restaurantrush/check/GameCheck.java` | Modified | Check the Stage 2 arrivals and both full Stage 2 games, and keep the Stage 1 checks. |
| `src/restaurantrush/check/StaffCheck.java` | Modified | Check the corrected `ORDER` error messages. |
| `fixtures/stage-2/vip-critic.in`, `.expected` | Added | The Stage 2 victory run. |
| `fixtures/stage-2/defeat.in`, `.expected` | Added | The Stage 2 defeat run. |
| `README.md`, `fixtures/README.md` | Modified | Describe the Stage 2 rules, runs and changes. |
| `docs/component-contract.md` | Modified | Record the `Waiter` fix and how stage tags are handled. |

Every other Stage 1 class stayed the same, including `Customer`, `RegularCustomer`, `TurnController`, `GameState`, the menu classes and the CLI. The Stage 1 games are still checked by `GameCheck` with Regular customers only.

### Testing

The team ran `scripts/replay.sh` on a fresh copy of the repository for both Stage 2 and Stage 1. A teammate then played a full Stage 2 game by hand. It ended in VICTORY with five customers paying, $73.50 revenue and an average of 75.6. The VIP and Critic behaved as expected, and invalid commands were handled correctly. One wrong error message for `ORDER` was found and fixed (see the `Waiter.java` row above).

## Stage 3

### Build and run

Build and run the same way as in Stage 1. On `main`, the game plays Stage 3. The setting, arrival turns, VIP and Critic rules, service steps, and win and lose rules are the same as in Stage 2.

The console groups each turn into clear sections. Before each turn, until Happy Hour has been used, the game asks the manager whether to start Happy Hour. The turn banner shows the turn number and whether Happy Hour is on. Then come any new events such as arrivals (`NEWS`), the `WAITING QUEUE`, `TABLES`, `CUSTOMERS` and `ORDERS`. The Host, the Waiter and the Chef are then asked in that order, each with a numbered list of what they can do.

| Prompt | Commands | Numbers |
| --- | --- | --- |
| `Manager> ` | `HAPPY_HOUR` to start Happy Hour, `SKIP` to not start it | None, type the word |
| `Host> ` | `SEAT C<n>`, `WAIT` | `1` and up for the listed actions, `0` for `WAIT` |
| `Waiter> ` | `SEAT C<n>`, `ORDER C<n>`, `SERVE O<n>`, `WAIT` | As in Stage 1 |
| `Chef> ` | `PREPARE O<n>`, `WAIT` | As in Stage 1 |

Any other answer at `Manager> ` prints `Invalid manager command. Use HAPPY_HOUR or SKIP.` and asks again.

### Host

The Host goes first each turn and can seat one waiting customer at the first free table, or wait. The Waiter can still seat customers too, so two customers can be seated in one turn when both tables are free. Because the Host goes first, the Waiter can take the order of a customer the Host just seated. The Host and the Waiter share the same seating code (`Seating`), so seating at a full table is rejected the same way for both.

### Combo meal

The Stage 3 menu adds the `Salad + Pasta combo`. A combo is made of two existing menu items. It takes as many preparation units as both items together, and costs 90% of their combined price, rounded to the nearest cent. The Salad + Pasta combo costs ($9.00 + $18.00) x 0.9 = $24.30 and takes 3 units, so the Chef needs three turns to finish it.

Regular customers whose arrival number is divisible by 3 (C3 and C6) order the combo. Every other customer orders the same meal as in Stage 2.

| Customer | Arrives on turn | Type | Meal |
| --- | --- | --- | --- |
| C1 | 1 | Regular | Salad |
| C2 | 4 | VIP | Burger |
| C3 | 7 | Regular | Salad + Pasta combo |
| C4 | 10 | Critic | Pasta |
| C5 | 13 | Regular | Burger |
| C6 | 16 | Regular | Salad + Pasta combo |

### Happy Hour

- **When:** the manager can start Happy Hour once per game, before any turn.
- **How long:** it lasts for the turn it starts on and the next turn. If started on turn 18, it only lasts that turn because the game ends.
- **Recovery:** at the start of each Happy Hour turn, every customer still waiting for a table or for their food gains 15 satisfaction, up to a maximum of 100.
- **Discount:** orders taken during Happy Hour get a further 20% off, after any VIP discount, rounded to the nearest cent at each step. A VIP Burger costs $15.00 x 0.9 x 0.8 = $10.80, and a Regular customer's combo costs $24.30 x 0.8 = $19.44.
- **Fixed prices:** the price is set when the Waiter takes the order. An order taken before Happy Hour keeps its normal price, even if it is paid during Happy Hour.

Each rule belongs to the class that holds the data. `GameState` records when Happy Hour started and how many turns are left, `TurnController` starts it and asks each customer to recover, `Customer.recover` decides who can recover, and `Order` sets the discounted price. The CLI only asks the question and shows the result.

### Reproducible runs

```sh
scripts/replay.sh                                            # replay both Stage 3 runs
scripts/replay.sh fixtures/stage-3/host-combo-happy-hour.in  # replay one run
```

| Run | What it shows | Result |
| --- | --- | --- |
| `host-combo-happy-hour` | The Host does all the seating, so the Waiter is free to take orders and serve food. Happy Hour starts on turn 7. C3 orders the combo during Happy Hour for $19.44 instead of $24.30 and recovers satisfaction on turn 8, and the Chef takes three turns to make the combo. VIP C2 ordered before Happy Hour, so their Burger stays at $13.50 even though they pay during Happy Hour. | Five pay, $74.94 revenue, average 83.8, VICTORY |
| `defeat` | The manager skips Happy Hour every turn and all staff wait. Nobody pays. C1 leaves at the end of turn 13, and Critic C4 at the end of turn 18. | Nobody pays, DEFEAT |

The payments in `host-combo-happy-hour` are:

| Customer | Payment turn | Bill | Satisfaction at payment |
| --- | --- | --- | --- |
| C1 | 4 | $9.00 | 92 |
| C2 | 7 | $13.50 | 95 |
| C3 | 12 | $19.44 | 84 |
| C4 | 15 | $18.00 | 64 |
| C5 | 17 | $15.00 | 84 |

Both runs use the same setting, arrivals and meals, so only the manager's choices change the result. The sequence diagrams follow C3's path through the `host-combo-happy-hour` run.

### Stage 2 to Stage 3

Compare: `https://github.com/respoopu/SC2002-lab-assignment-final/compare/stage-2...stage-3`, or run `git diff --name-status stage-2 stage-3`.

| File | Status | Reason |
| --- | --- | --- |
| `src/restaurantrush/staff/Host.java` | Added | The Host: lists who can be seated and seats them using the shared `Seating` code. |
| `src/restaurantrush/menu/ComboMeal.java` | Added | A menu item made of two existing items, with their units added together and 90% of their combined price. |
| `src/restaurantrush/game/Phase.java`, `StaffRole.java` | Modified | Add the Host's turn, before the Waiter's. |
| `src/restaurantrush/game/GameState.java` | Modified | Store when Happy Hour started and report how many turns are left. |
| `src/restaurantrush/game/Snapshot.java` | Modified | Pass the Happy Hour status to the CLI. |
| `src/restaurantrush/game/TurnController.java` | Modified | Add the Host's turn, and start Happy Hour and apply recovery at the start of a turn. |
| `src/restaurantrush/game/GameSetup.java` | Modified | A small setup change: add the combo to the menu and the Host to the game. |
| `src/restaurantrush/customer/Customer.java` | Modified | Add `recover`, because satisfaction belongs to the customer. |
| `src/restaurantrush/customer/RegularCustomer.java` | Modified | Choose the combo when the arrival number is divisible by 3 and the combo is on the menu. |
| `src/restaurantrush/menu/Order.java` | Modified | Apply the Happy Hour discount when the order is taken. |
| `src/restaurantrush/staff/Waiter.java` | Modified | Tell the order whether Happy Hour is on, and name the customer and order in its messages. |
| `src/restaurantrush/staff/Cashier.java`, `Chef.java`, `Seating.java` | Modified | Console display update: name the customer, order and table in their messages. |
| `src/restaurantrush/cli/CommandParser.java` | Modified | Read the `Manager> ` answer, and only allow `SEAT` and `WAIT` for the Host. |
| `src/restaurantrush/cli/ConsoleUI.java` | Modified | Add the `Manager> ` and `Host> ` prompts, ask each role in turn, and lay out the console display. |
| `src/restaurantrush/check/CliCheck.java`, `CustomerCheck.java`, `GameCheck.java`, `MenuCheck.java`, `StaffCheck.java` | Modified | Check the Host, the combo, Happy Hour, recovery and both full Stage 3 games, and keep the Stage 1 and 2 checks. |
| `fixtures/stage-3/host-combo-happy-hour.in`, `.expected` | Added | The Stage 3 victory run. |
| `fixtures/stage-3/defeat.in`, `.expected` | Added | The Stage 3 defeat run. |
| `docs/diagrams/*.puml`, `style.iuml` | Added | The UML class and sequence diagram sources. |
| `docs/diagrams/exports/*.png`, `*.svg` | Added | The diagrams as images for the report. |
| `docs/diagrams/render.sh` | Added | A script that redraws all the diagram images. |
| `README.md`, `fixtures/README.md`, `docs/component-contract.md` | Modified | Describe the Stage 3 rules, runs, changes and console display. |
| `.github/PULL_REQUEST_TEMPLATE.md`, `.github/ISSUE_TEMPLATE/work-item.yml` | Modified | Tidy the template wording before submission. |
| `SC2002_Restaurant_Rush_Group_Assignment.docx.pdf` | Deleted | The assignment brief, removed from the repository before submission. |

**Why earlier classes changed.** Each change is the smallest one that gives the new feature to the class that owns the data. No Stage 1 or Stage 2 game rule was rewritten. Most changes add a new constructor or method next to the old one, so the old Stage 1 and 2 flow without the Host or Happy Hour still works. Two methods were reorganised without changing their output: `ConsoleUI.run` now asks whichever role the controller names instead of always asking the Waiter and then the Chef, and `TurnController` passes each action to the right staff member.

**Earlier stages still work.** `GameCheck` still runs the full Stage 1 and Stage 2 games and passes. Each earlier stage can also be replayed at its own tag with `git checkout stage-1 && scripts/replay.sh` or `git checkout stage-2 && scripts/replay.sh`.

**Unchanged classes.** `Menu`, `MenuItem`, `Money`, `OrderStatus`, `VIPCustomer`, `CriticCustomer`, `CustomerStatus`, `WaitingQueue`, `Table`, `Main`, `GameConfig`, `Action`, `ActionKind`, `ActionResult`, `CustomerView`, `GameEvent`, `GameResult`, `OrderView`, `TableView`, `TurnStart`, `TurnEnd` and `TurnSummary`.

### Testing

The team ran `scripts/replay.sh` on a fresh copy of the repository for Stage 3, and again at the `stage-2` and `stage-1` tags. All the checks and runs passed. A teammate also played a full 18-turn Stage 3 game by hand, and the team checked that the diagrams match the final code. After the console display update, all the checks and both Stage 3 runs passed again.

## Video timestamps

The timestamps below are for `SC2002_Demo_Vid_Final.mp4`.

| # | Segment | Presenter | Time |
| --- | --- | --- | --- |
| 1 | The game launching from the submitted project | Rayden | 0:00 |
| 2 | Meaningful manager choices, including a decision where staff are constrained | Rayden | 1:05 |
| 3 | A dish going READY → SERVED → payment | Isaac | 2:38 |
| 4 | A VIP or Critic behaviour | Advay | 4:52 |
| 5 | A Stage 3 feature | Zachary | 6:40 |
| 6 | One victory and one defeat | Zachary | 8:03 |
| 7 | The stage tag comparisons and the diagrams | Jethro | 9:02 |
