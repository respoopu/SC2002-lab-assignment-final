# Stage 3 UML and design evidence

<!-- Generated for issue #20; maintain against source changes before the stage gate. -->

These diagrams describe the merged Stage 3 implementation at **`d38231a68711e445eadd2c0bb795ff62d1638f04`** (PR #37). They support [issue #20](https://github.com/respoopu/SC2002-lab-assignment-final/issues/20), contract Sections 5 and 7.10-7.12, and assignment brief Section 5. There is no prescribed UML tool or image format in the brief, contract, or issue. This deliverable uses **PlantUML source plus SVG and PNG exports**; it does not introduce a repository-wide format rule.

The two class views form one model. The four sequence pages form one complete choice-sensitive scenario; include them together when using the diagrams in the report. SVG preserves text and line sharpness when zoomed or embedded in a PDF; PNG is supplied for tools that cannot import SVG. Use landscape space for the class views and preserve the sequence pages' aspect ratios rather than squeezing the complete set onto one page. This folder contains diagram evidence, not the full report or submission package.

## Diagrams

| View | Editable source | Scalable export | Raster export |
| --- | --- | --- | --- |
| Class model 1: customers, queue, menu, orders, tables and ownership | [class-domain.puml](class-domain.puml) | [SVG](exports/class-domain.svg) | [PNG](exports/class-domain.png) |
| Class model 2: entry point, setup, boundary, control, staff, snapshots and enums | [class-control.puml](class-control.puml) | [SVG](exports/class-control.svg) | [PNG](exports/class-control.png) |
| Sequence 1: full 18-turn victory and alternative defeat | [sequence-1-game.puml](sequence-1-game.puml) | [SVG](exports/sequence-1-game.svg) | [PNG](exports/sequence-1-game.png) |
| Sequence 2: turn-7 Happy Hour activation, arrival and Host seating | [sequence-2-arrival.puml](sequence-2-arrival.puml) | [SVG](exports/sequence-2-arrival.svg) | [PNG](exports/sequence-2-arrival.png) |
| Sequence 3: polymorphic combo choice, locked price and first Chef unit | [sequence-3-order.puml](sequence-3-order.puml) | [SVG](exports/sequence-3-order.svg) | [PNG](exports/sequence-3-order.png) |
| Sequence 4: recovery/preparation, serving, eating, payment and final result | [sequence-4-service.puml](sequence-4-service.puml) | [SVG](exports/sequence-4-service.svg) | [PNG](exports/sequence-4-service.png) |

Class members are selected key attributes and operations, not an exhaustive Java API listing. `+`, `#`, `~`, and `-` mean public, protected, package-private, and private. The constructor/operation names and types follow the implementation. Final values are marked read-only; a final reference to a mutable collection does not make its contents immutable. Records also have generated public component accessors even when only their private component fields are drawn. Supporting value records (`TurnStart`, `TurnEnd`, `TurnSummary`, `CustomerView`, `TableView`, `OrderView`, `GameEvent`) and routine accessors are omitted from the class boxes where they do not add design information; their role is identified in view 2 and the method signatures.

Solid directed associations represent retained object references; dashed dependencies represent calls, construction or expressly labelled ID lookup. `Order.customerId` and `Table.occupantId` are strings, not Customer object fields. An occupied table resolves to at most one customer and a customer has at most one order in reachable gameplay. GameState creates and owns its queue and tables (filled composition diamonds; two tables in the base configuration). Menu items are shared by the catalog, orders and combos: hollow diamonds on ComboMeal represent shared aggregation, not exclusive ownership. This is object composition in the design discussion without falsely claiming exclusive UML composition of the component items.

Sequence arrows show calls/inputs and dashed returns/outputs. `ref` frames point to expanded interactions on the other pages, not to invented Java methods. Guard/context labels summarize conditions; turn numbers in message annotations are context, not additional arguments. Object accessors and unrelated customers' repeated calls are compressed explicitly in notes. For example, the controller calls its injected arrival factory; the factory's lambda calls GameSetup, not the UI or a customer subtype switch inside the controller.

## Source and responsibility map

| Diagram evidence | Real implementation and responsibility |
| --- | --- |
| Boundary and numbered input | [ConsoleUI.run/render](../../src/restaurantrush/cli/ConsoleUI.java) collects choices, uses the same legal-action list for display and parsing, retries invalid input, and renders values. [CommandParser](../../src/restaurantrush/cli/CommandParser.java) parses role-specific text/numbers and unnumbered manager commands. Neither decides game rules. |
| Phase order, guards and results | [TurnController](../../src/restaurantrush/game/TurnController.java) `beginTurn(boolean)`, `submit`, `endTurn`, `snapshot`, `recoverWaitingCustomers`, `isVictory`; it alone advances the phases. Legacy construction omits Host and Happy Hour. |
| Activation and retained state | [GameState](../../src/restaurantrush/game/GameState.java) stores the activation turn and derives remaining duration. [Snapshot](../../src/restaurantrush/game/Snapshot.java) copies list values and carries availability/duration to the boundary. |
| Registration and arrival construction | [GameSetup](../../src/restaurantrush/game/GameSetup.java) `baseGame`, `stage3Menu`, `arrival`: wires staff, menu and injected factory. Customer type selection lives here. |
| One shared seating implementation | [Host.seat](../../src/restaurantrush/staff/Host.java) and [Waiter.seat](../../src/restaurantrush/staff/Waiter.java) delegate to package-private [Seating](../../src/restaurantrush/staff/Seating.java); it checks all preconditions before queue/table/customer effects. |
| Encapsulated, polymorphic customer behavior | [Customer](../../src/restaurantrush/customer/Customer.java) owns transitions, satisfaction and eligibility. [RegularCustomer](../../src/restaurantrush/customer/RegularCustomer.java), [VIPCustomer](../../src/restaurantrush/customer/VIPCustomer.java) and [CriticCustomer](../../src/restaurantrush/customer/CriticCustomer.java) supply choice/loss/discount through the same abstract operations. |
| Composition and price lock | [ComboMeal](../../src/restaurantrush/menu/ComboMeal.java) retains its two MenuItem components; [Order](../../src/restaurantrush/menu/Order.java) computes the final bill using [Money](../../src/restaurantrush/menu/Money.java) and owns preparation/state transitions. Waiter passes the order-time Happy Hour flag. |
| Strict preparation/service/payment delays | [Chef.prepare](../../src/restaurantrush/staff/Chef.java) advances one unit. [Waiter.serve](../../src/restaurantrush/staff/Waiter.java) checks `readyTurn < turn`. The controller's eating step sets readiness; [Cashier.collect](../../src/restaurantrush/staff/Cashier.java) requires `readyToPayTurn < turn`, pays oldest-first, records metrics and releases the table. |

These are concrete responsibility, encapsulation, polymorphism and composition examples. There is no separate HappyHour policy class, staff superclass, or menu-choice interface in the code, so the diagrams do not invent one. No unsupported claim that all SOLID principles are implemented is needed. The shared Seating helper avoids duplicate seating rules; the nullable Host field and retained legacy overload trade a separate controller hierarchy for a small optional phase in the existing coordinator.

## Reproducible scenario evidence

Use the committed [victory input](../../fixtures/stage-3/host-combo-happy-hour.in) and [transcript](../../fixtures/stage-3/host-combo-happy-hour.expected), and the [defeat input](../../fixtures/stage-3/defeat.in) and [transcript](../../fixtures/stage-3/defeat.expected). The code baseline is the commit above; `stage-3` will be created by #21 after its gate and is **not assumed to exist yet**. Earlier `stage-1` and `stage-2` tags remain their own runnable snapshots.

| Turn(s) | C3/O3 interaction in the winning scenario | State/evidence |
| --- | --- | --- |
| 1-6 | Other customers' usual flow; manager SKIP | C1 pays on 4 at 92; C2 reaches READY_TO_PAY before turn 7. |
| 7 start | HAPPY_HOUR; recover existing eligible customers before arrival; C3 arrives | C1 is PAID and C2 READY_TO_PAY, so neither changes. C3 arrives at 100, remaining duration 2. |
| 7 choices | Host seats C3 at T1; Waiter orders combo; Chef prepares first unit | C2 still occupies T2 until turn-7 payment. O3: 2430 -> 1944 cents, PREPARING 1/3; C3 ends at 92. |
| 8 | Recovery then second Chef unit | C3 recovers 8 to 100, loses 8 at end -> 92; remaining 1; O3 2/3. |
| 9 | Third Chef unit; no Happy Hour | O3 READY with readyTurn 9; remaining 0; C3 ends at 84. Waiter already acted before the Chef. |
| 10 | Host seats C4 at T2; Waiter serves O3 | C3 EATING, O3 SERVED; no same-turn eating completion or waiting loss. |
| 11 | End-of-turn eating step | C3 becomes READY_TO_PAY with readyToPayTurn 11; Cashier cannot collect it yet. |
| 12 | Automatic collection | O3 and C3 PAID; 1944 cents and satisfaction 84 recorded; T1 released. Cumulative revenue 4194, paidCount 3. |
| 13-18 | Remaining fixture choices complete the run | C4 pays on 15; C5 on 17. End turn 18 is VICTORY; C6 remains WAITING at 76. |

The winning run consumes **61 accepted commands** (7 manager, 54 staff). Payments are:

| Customer | Paid on turn | Bill (cents) | Satisfaction |
| --- | --- | --- | --- |
| C1 | 4 | 900 | 92 |
| C2 (VIP) | 7 | 1350 | 95 |
| C3 (combo during Happy Hour) | 12 | 1944 | 84 |
| C4 (Critic, Pasta) | 15 | 1800 | 64 |
| C5 | 17 | 1500 | 84 |

Totals: five paid, **7494 cents**, satisfaction sum **419**, average **83.8**, served five, left zero. Integer result checks all hold (`5 >= 4`, `7494 >= 4500`, `419 >= 60 * 5`). The second branch consumes **72 accepted commands**: SKIP followed by three WAITs every turn. Configuration, arrivals and meal-selection rules are identical, but manager choices yield no orders/payments, revenue zero, left two (C1 on 13, C4 on 18), and DEFEAT. C2/C3/C5/C6 remain waiting at 25/4/52/76. This is a comparison of outcomes; it does not claim that Happy Hour alone is the deciding factor.

## Regeneration and verification

Rendering requires Java and a local [PlantUML 1.2026.9 jar](https://github.com/plantuml/plantuml/releases/tag/v1.2026.9); it is not a game runtime dependency. The jar is not committed. Class views use PlantUML's built-in Smetana layout, so a separate Graphviz installation is not required. All rendering is local; do not paste repository source into a public rendering service.

The jar used for these exports has SHA-256:

```text
aa4a1d06f8911c4b842beff4828dd955be431692b631a312d04c0e9fab9581de
```

From the repository root in Git Bash (use your own jar location):

```bash
export PLANTUML_JAR="$HOME/tools/plantuml-1.2026.9.jar"
bash docs/diagrams/render.sh
bash scripts/replay.sh
git diff --check
```

Java must be on PATH. The render script checks the version, disables embedded source metadata, raises the image-size limit to prevent PNG clipping, and renders each source to both formats. `style.iuml` contains presentation settings only. Font metrics can vary across Java/platform installations; the exact font placement is not a game transcript guarantee. Keep corresponding source/SVG/PNG changes in the same logical commit when editing a view.

Verification for this issue includes rendering every source, inspecting all six PNGs and the SVG dimensions for clipping, matching the class members and sequence responsibilities against the linked methods, and running `scripts/replay.sh` on the baseline above: all five self-checks and both Stage 3 fixtures must pass. Check that the recorded C3 timings, five payments and alternative defeat values match the actual transcripts. Before #21 tags Stage 3, update these diagrams if a later fix changes the design or trace; use the final code and gate evidence rather than assuming this baseline cannot change.
