# Restaurant Rush component contract

**Status:** Draft for team review. No implementation issues or stage tags should be based on this until the team agrees on the proposed decisions below.

The [assignment brief](../SC2002_Restaurant_Rush_Group_Assignment.docx.pdf) takes precedence over this document. This contract defines the boundaries between workstreams so that independently developed parts can be integrated. Names and method signatures are provisional; behavior and ownership are the important agreements.

## 1. Shared game rules

The required base setting is 18 turns, two tables, one Waiter task per turn, one Chef preparation unit per turn, and at most one automatic Cashier payment per turn. One customer arrives at the start of turns 1, 4, 7, 10, 13, and 16. The base menu is Salad ($9.00, one unit), Burger ($15.00, one unit), and Pasta ($18.00, two units). The game always completes turn 18. Victory requires at least four paid customers, revenue of at least 4,500 cents, and average satisfaction of paid customers of at least 60; otherwise it is Defeat.

All money is stored as integer cents. Satisfaction is an integer from 0 to 100. The average uses satisfaction **recorded at payment**, and shows `N/A` when no customer has paid. The result check uses the underlying sum and count, not a rounded display value. Revenue changes only on payment.

Each customer has a stable ID and can place at most one order. A table is occupied from seating until payment or abandonment. The waiting queue keeps arrival order for display, but the manager may choose any queued customer to seat. A customer's food choice follows a documented, reproducible rule; the manager cannot select the food.

### Shared states and transitions

| Object | States | Required transition rules |
| --- | --- | --- |
| Customer | `WAITING`, `SEATED`, `EATING`, `READY_TO_PAY`, `PAID`, `LEFT` | Arrival creates `WAITING` at 100 satisfaction. Seating changes to `SEATED`; serving starts `EATING`; payment changes to `PAID`. Reaching 0 satisfaction while waiting or seated changes to `LEFT`. |
| Order | `PLACED`, `PREPARING`, `READY`, `SERVED`, `PAID`, `CANCELLED` | Taking an order locks its bill. Chef work advances one unit and makes it `READY` when complete. Only a previously ready order can be served. Payment changes it to `PAID`; abandonment before service changes it to `CANCELLED`. |
| Table | Free or occupied by one customer ID | Seating requires a free table. Only payment or abandonment frees it. |

`NO_ORDER` may be displayed for a customer without an order; it is not an `Order` state. Record the turn on which an order was served and the turn on which its customer became ready to pay. These timestamps enforce the required delays without relying on CLI behavior.

## 2. Turn contract

The turn controller is the only component that advances phases. It applies actions in this order:

1. Start the turn, apply any Stage 3 Happy Hour activation and start-of-turn recovery, then add the scheduled arrival. Publish a read-only snapshot for the CLI to display before staff choices.
2. In Stage 3, accept one Host choice: seat a queued customer or wait. The Host acts before the Waiter under this draft's proposed ordering.
3. Accept one Waiter choice: seat one queued customer, take one seated customer's order, serve one `READY` order, or wait. The Waiter acts before the Chef. An order made `READY` by the Chef later in this turn cannot be served yet.
4. Accept one Chef choice: advance one eligible order by one preparation unit or wait. An order taken by the Waiter in this turn is eligible.
5. Move customers served on an **earlier** turn through one full turn of eating to `READY_TO_PAY`. The Cashier then pays at most one customer who was already `READY_TO_PAY` before this turn, oldest first. A customer who becomes ready to pay now cannot pay until a later turn.
6. Deduct satisfaction from customers still waiting for a table or food and not served. Abandon those who reach 0, cancel any unserved order, and free their table if occupied. Served, eating, and ready-to-pay customers do not lose satisfaction.
7. Publish the turn summary: cumulative served count, unhappy departures, revenue, paid-customer average, and turns used. After turn 18, publish Victory or Defeat.

"Oldest first" means earliest turn becoming `READY_TO_PAY`, with arrival order breaking a tie. This tie-break is a proposed deterministic interpretation of the brief.

The snapshot must provide the turn number, revenue, paid-customer average, waiting queue, occupied tables, each arrived customer's satisfaction and status, each order's status, available staff tasks, and Stage 3 Happy Hour duration when relevant. The CLI displays events such as arrivals and food choices when they occur. The controller and domain determine what is legal; the CLI only parses choices and renders snapshots, events, and errors.

### Action boundary

The precise Java types can be agreed when the first code skeleton is made, but every staff action follows this behavior:

| Input | Output | Invalid-choice guarantee |
| --- | --- | --- |
| An action kind and stable target ID where needed; for example, `SEAT C2`, `ORDER C1`, `SERVE O1`, or `PREPARE O1` | An accepted/rejected result, a clear reason on rejection, events on success, and an updated read-only snapshot | Rejection changes no game state, does not consume that staff member's task, and allows another choice in the same turn. |

`WAIT` is a valid choice that ends that staff member's opportunity for the turn. The controller rejects actions in the wrong phase or a second accepted task from the same staff member. CLI parse errors follow the same retry behavior as invalid domain actions.

## 3. Component responsibilities and interfaces

These five workstreams match the intended team split. A workstream can own several classes; no one should duplicate another workstream's rules in their own component.

| Workstream | Owns and changes | Receives | Produces / guarantees |
| --- | --- | --- | --- |
| **Turn control and game state** | Turn number, phase, arrival schedule, storage of authoritative object references, cumulative metrics, result check, and ordering of staff calls | Game configuration and staff action requests | Read-only snapshots, ordered events, turn summaries, final result; exactly 18 turns and correct phase timing |
| **Customers and waiting queue** | Customer identity/status/satisfaction, queue insertion and removal rules, food preference, customer-specific waiting loss and personal discount rate | Arrival information, elapsed waiting turn, menu catalog, start-of-turn recovery request | Chosen meal, satisfaction changes, abandonment signal, personal discount rate; Stage 2 variation through customer methods rather than controller type checks |
| **Menu, orders, and pricing** | Menu definitions, order progress/status, preparation units, all cents arithmetic, locked bill, Stage 3 combo composition and Happy Hour price rule | Customer's chosen meal and personal discount rate, whether Happy Hour is active at order time, Chef work | New order and its displayed choice, one-unit progress events, final locked bill; no change to an existing order when a promotion later starts or ends |
| **Staff and tables** | Table occupancy and legal Waiter/Chef/Cashier operations; Stage 3 Host seating | Current phase, target IDs, customer/order/table state | One legal staff effect or a rejection reason; no double booking, premature serving, premature payment, or more than one task per role per turn |
| **CLI and reproducibility** | Text input/output, action parsing, deterministic run fixtures, build/run instructions | Read-only snapshots, offered tasks, events, action results | Manager choice requests, readable status and errors, repeatable victory/defeat transcripts; no game rules or direct mutation of domain state |

The game state stores the shared objects; the relevant domain or staff component owns the rules for changing each object. The turn controller coordinates them; it should not contain `if`/`switch` chains on customer subtype. Customer objects supply their own waiting loss, food preference, and personal discount rate. The pricing component performs all rounding and discount arithmetic. An order owns its preparation progress and locked bill. A table owns its occupant. The CLI does not import concrete customer subtypes or calculate prices, satisfaction, or legal actions.

The controller exposes read-only views or defensive copies. No caller receives a mutable internal queue, table list, customer list, or order list. Stable IDs, rather than list positions, identify action targets so that paying or leaving does not silently change what a later input means.

## 4. Stage-specific extension contract

### Stage 1: complete base game

All six arrivals are Regular customers. Each loses 8 satisfaction per waiting or seated, unserved turn and pays full price. Stage 1 contains all core turn and outcome rules, plus reproducible victory and defeat notes. Its tagged commit must compile and run without any Stage 2 or 3 feature classes or menu policies.

### Stage 2: customer behavior

Add `VIPCustomer` and `CriticCustomer` as customer variants using the same seating, ordering, preparation, serving, eating, and payment flow. VIP loses 5 satisfaction per eligible turn and receives 10% off the menu price. Critic loses 12, pays full price, and has a distinct documented preference or service reaction. Keep six arrivals and include at least one VIP and one Critic in a reproducible base-setting run. The controller asks a customer object for its behavior; it does not inspect the customer's class to decide these rules.

### Stage 3: new capacity and policies

The Host may seat at most one queued customer each turn in addition to the Waiter's task; the Waiter retains the ability to seat. A `ComboMeal` contains two existing menu items. Its preparation units are their sum and its base price is 90% of their combined prices, rounded to the nearest cent. A documented customer rule chooses at least one combo in a reproducible run.

Happy Hour can be activated once, at the start of a turn. It is active for that turn and the next, with remaining duration displayed. At the start of each active turn, customers still waiting for a table or food recover 15 satisfaction, capped at 100; departed customers do not recover. Orders placed while active receive a further 20% discount. Apply any VIP discount first, then the Happy Hour discount, and lock the result when the order is taken. Existing orders keep their original bill.

Stage extensions should change registration/setup and the few components that genuinely own new behavior. Every change to an existing Stage 1 or 2 class must be recorded with a reason for the README stage comparison.

## 5. Integration and acceptance checks

Before merging a PR, its owner and reviewer should check the relevant behavior against this contract and the brief. In particular:

- The brief's Pasta example must be reproducible: seat on turn 1; order and prepare unit 1 on turn 2; finish preparation on turn 3; serve on turn 4; become ready to pay on turn 5; pay on turn 6; record satisfaction 76 and revenue $18.00.
- Invalid input or an illegal target explains the problem and allows another choice without consuming the staff action.
- A customer served on turn `t` cannot become ready to pay on turn `t`; one becoming ready to pay on turn `t` cannot pay on turn `t`.
- Revenue never includes an unpaid or abandoned order. The customer arriving on turn 16 cannot pay by turn 18 in the base flow.
- Stage 1 has full 18-turn victory and defeat runs. Stage 2 has a reproducible run with VIP and Critic. Stage 3 shows Host, ComboMeal, Happy Hour recovery and pricing, and a complete result.
- At least one pair of runs uses the same setting and arrival/meal rules but different manager choices to demonstrate how prioritization changes the outcome.
- The class diagram matches the final code, and the sequence diagram traces real method responsibilities and timing. README stage comparisons identify added and changed files, reasons, and commit or compare links.

No stage tag is created until that stage builds offline, runs from a fresh checkout using the documented command, and passes its reproducible scenarios. Tags `stage-1`, `stage-2`, and `stage-3` each identify a specific tested commit.

## 6. Proposed decisions for team review

These choices are **not specified fully by the brief**. Resolve them before creating dependent issues; record any changes here so PRs use the same behavior.

| Decision | Proposed default | Reason / effect |
| --- | --- | --- |
| Java/build baseline | Java 17, standard library only, with documented `javac`/`java` commands | Keeps a fresh offline build straightforward; confirm the course's supported Java version. |
| Food choice | Cycle Salad, Burger, Pasta by arrival index for Regular and VIP customers; Critic always chooses Pasta | Deterministic, demonstrates the Critic's distinct preference, and needs no manager food choice. |
| Customer types at arrivals in Stage 2/3 | Regular, VIP, Regular, Critic, Regular, Regular at turns 1, 4, 7, 10, 13, 16 | Ensures both new types appear while preserving six arrivals. |
| Stage 3 combo choice | A Regular customer with arrival index divisible by 3 chooses Salad + Pasta when the combo is available; otherwise use the base food cycle | Produces a visible, reproducible combo without adding a manager food choice. |
| Host/Waiter order | Host acts before Waiter, so the Waiter may take the order of a customer seated by the Host that turn | Makes their separate capacities useful and gives a deterministic turn sequence. |
| Happy Hour at turn start | Activate and recover before the scheduled arrival, then display the actionable snapshot | Customers arriving that turn already have 100 satisfaction; the order does not change their recovery. |
| Percentage rounding | Round half up to the nearest cent after combo pricing, after personal discount, and after Happy Hour discount | Maintains integer cents and a consistent order of operations. |
| Cashier tie-break | Earliest `READY_TO_PAY` turn, then earliest arrival | Makes "oldest first" deterministic if two customers qualify together. |

Once the team agrees on these decisions, turn each workstream into small Stage 1, Stage 2, Stage 3, and evidence issues with one primary owner and one reviewer. Any PR that needs to change a shared contract should update this document and explain the impact on other workstreams.
