# Restaurant Rush component contract

**Status:** Game rules, component responsibilities, the decisions in Section 6, and the shared Java API in Section 7 are agreed by the team. Workstream issues reference Section 7 instead of inventing types. Any PR that changes Section 7 updates it in the same PR.

The [assignment brief](../SC2002_Restaurant_Rush_Group_Assignment.docx.pdf) takes precedence over this document. This contract defines the boundaries between workstreams so that independently developed parts can be integrated. The behavioral decisions and the public Java surface in Section 7 are binding.

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
2. In Stage 3, accept one Host choice: seat a queued customer or wait. The Host acts before the Waiter under the team's agreed ordering.
3. Accept one Waiter choice: seat one queued customer, take one seated customer's order, serve one `READY` order, or wait. The Waiter acts before the Chef. An order made `READY` by the Chef later in this turn cannot be served yet.
4. Accept one Chef choice: advance one eligible order by one preparation unit or wait. An order taken by the Waiter in this turn is eligible.
5. Move customers served on an **earlier** turn through one full turn of eating to `READY_TO_PAY`. The Cashier then pays at most one customer who was already `READY_TO_PAY` before this turn, oldest first. A customer who becomes ready to pay now cannot pay until a later turn.
6. Deduct satisfaction from customers still waiting for a table or food and not served. Abandon those who reach 0, cancel any unserved order, and free their table if occupied. Served, eating, and ready-to-pay customers do not lose satisfaction.
7. Publish the turn summary: cumulative served count, unhappy departures, revenue, paid-customer average, and turns used. After turn 18, publish Victory or Defeat.

"Oldest first" means earliest turn becoming `READY_TO_PAY`, with arrival order breaking a tie. This is the team's agreed deterministic interpretation of the brief.

The snapshot must provide the turn number, revenue, paid-customer average, waiting queue, occupied tables, each arrived customer's satisfaction and status, each order's status, available staff tasks, and Stage 3 Happy Hour duration when relevant. The CLI displays events such as arrivals and food choices when they occur. The controller and domain determine what is legal; the CLI only parses choices and renders snapshots, events, and errors.

### Action boundary

The Java types are `Action` and `ActionResult` in Section 7.4; every staff action follows this behavior:

| Input | Output | Invalid-choice guarantee |
| --- | --- | --- |
| An action kind and stable target ID where needed; for example, `SEAT C2`, `ORDER C1`, `SERVE O1`, or `PREPARE O1` | An accepted/rejected result, a clear reason on rejection, events on success, and an updated read-only snapshot | Rejection changes no game state, does not consume that staff member's task, and allows another choice in the same turn. |

`WAIT` is a valid choice that ends that staff member's opportunity for the turn. The controller rejects actions in the wrong phase or a second accepted task from the same staff member. CLI parse errors follow the same retry behavior as invalid domain actions.

Every role is prompted every turn, even when `WAIT` is its only legal action, so a Stage 1 turn always consumes exactly two input lines (Waiter, then Chef). In Stage 3 the Host prompt precedes the Waiter prompt, and a `Manager> ` prompt for Happy Hour precedes the arrival on every turn until Happy Hour has been used. The CLI loop that produces these prompts is fixed in Section 7.

## 3. Component responsibilities and interfaces

These five workstreams match the intended team split. A workstream can own several classes; no one should duplicate another workstream's rules in their own component.

| Workstream | Owns and changes | Receives | Produces / guarantees |
| --- | --- | --- | --- |
| **Turn control and game state** | Turn number, phase, arrival schedule, storage of authoritative object references, cumulative metrics, result check, and ordering of staff calls | Game configuration and staff action requests | Read-only snapshots, ordered events, turn summaries, final result; exactly 18 turns and correct phase timing |
| **Customers and waiting queue** | Customer identity/status/satisfaction, queue insertion and removal rules, food preference, customer-specific waiting loss and personal discount rate | Arrival information, elapsed waiting turn, menu catalog, start-of-turn recovery request | Chosen meal, satisfaction changes, abandonment signal, personal discount rate; Stage 2 variation through customer methods rather than controller type checks |
| **Menu, orders, and pricing** | Menu definitions, order progress/status, preparation units, all cents arithmetic, locked bill, Stage 3 combo composition and Happy Hour price rule | Customer's chosen meal and personal discount rate, whether Happy Hour is active at order time, Chef work | New order and its displayed choice, one-unit progress events, final locked bill; no change to an existing order when a promotion later starts or ends |
| **Staff and tables** | Table occupancy and legal Waiter/Chef/Cashier operations; Stage 3 Host seating | Current phase, target IDs, customer/order/table state | One legal staff effect or a rejection reason; no double booking, premature serving, premature payment, or more than one task per role per turn |
| **CLI and reproducibility** | Text input/output, action parsing, the `fixtures/` directory, `scripts/replay.sh`, build/run instructions | Read-only snapshots, offered tasks, events, action results | Manager choice requests, readable status and errors, repeatable victory/defeat transcripts; no game rules or direct mutation of domain state |

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

### Self-checks and fixtures

There is no unit test framework. Each workstream ships one self-check class, every reproducible run is a fixture, and `scripts/replay.sh` runs both. Self-checks, fixture replay, and the stage gate below are the project's only checks.

- **Self-check class:** one per workstream in package `restaurantrush.check`, named `CustomerCheck`, `MenuCheck`, `StaffCheck`, `GameCheck`, or `CliCheck`, with a `public static void main`. It builds its own objects through the public API in Section 7, uses Java `assert` statements, throws if assertions are not enabled, and prints one `OK <name>` line at the end. The replay script runs every class in that package with `java -ea` after compiling and before replaying fixtures. A collaborator from another workstream that is still a stub makes the check fail, which is why Section 7 sequences the issues in waves.

- **Location:** `fixtures/stage-<n>/<scenario>.in` with its transcript in `fixtures/stage-<n>/<scenario>.expected`. Stage 1 needs at least `victory` and `defeat`. Stage 2 needs at least `vip-critic`. Stage 3 needs at least `host-combo-happy-hour`. The pair of runs that differ only in manager choices uses the suffixes `-a` and `-b` on the same scenario name.
- **Input format:** one manager command per line, exactly as typed at the prompt, in the order the game asks for them. Blank lines and lines starting with `#` are comments; the script strips them before piping the file to the game's standard input.
- **Expected transcript:** the game's complete standard output for that input. Output must be byte-identical across runs and machines: no timestamps, no random values, no locale-dependent formatting, `\n` line endings (`.gitattributes` forces LF on these files).
- **End of input:** if standard input ends before turn 18 completes, the game prints a short message and exits with a non-zero status, so a short fixture fails instead of hanging.
- **Commands:** `scripts/replay.sh` builds the game, replays every fixture, prints `PASS` or `FAIL` per fixture, and exits non-zero on any failure. `scripts/replay.sh fixtures/stage-1/victory.in` replays one fixture. `scripts/replay.sh --update <fixture.in>` rewrites that expected transcript; use it only for an intended output change, and the PR reviewer reads the regenerated diff.
- **Before every PR:** run `scripts/replay.sh`. Every fixture from the current and earlier stages must pass. Add or update a fixture whenever the PR changes observable behavior.

### Stage gate

1. Every issue for the stage is closed by a merged PR.
2. From a fresh checkout, run `scripts/replay.sh`. Then one teammate plays the game manually against Sections 1 to 4 and the brief.
3. Each failure or deviation becomes a new work-item issue for that stage. Fix PRs merge and the replay is rerun.
4. When everything passes, tag that commit `stage-<n>`. Tags `stage-1`, `stage-2`, and `stage-3` each identify a specific tested commit.
5. PRs for the next stage may be opened at any time but are not merged until the `stage-<n>` tag exists. This keeps each tag free of later-stage code.

## 6. Agreed implementation decisions

The brief leaves these choices to the group. The team has accepted the following rules. Changing one requires an explicit contract update and a check of affected issues and tests.

| Decision | Agreed rule | Reason / effect |
| --- | --- | --- |
| Java/build baseline | Java 17 language level, standard library only, no test framework. Sources live under `src/restaurantrush`, entry point `restaurantrush.game.Main`. Build: `javac --release 17 -d out $(find src -name '*.java')`. Run: `java -cp out restaurantrush.game.Main` | Keeps a fresh offline build straightforward and consistent across contributors, whatever JDK is installed. |
| Verification | Self-checks and fixture replay with `scripts/replay.sh` before every PR, then the stage gate in Section 5; no JUnit | The brief's required reproducible runs double as the regression check, and the build stays plain `javac`. |
| Arrivals, types, and meals | The arrival table below. Arrival index is 1-based. Regular and VIP customers cycle Salad, Burger, Pasta by arrival index; the Critic always chooses Pasta; in Stage 3 a Regular customer whose index is divisible by 3 chooses the Salad + Pasta combo | One table replaces three prose rules and removes the 0-based versus 1-based ambiguity. |
| IDs | Plain strings: customers `C1`..`C6` in arrival order, orders `O1`.. in the order taken, tables `T1` and `T2`; input is uppercased before parsing | Stable targets for actions and fixtures without wrapper types. |
| Prompting | Every role is prompted every turn even when only `WAIT` is legal; Stage 3 adds the `Manager> ` Happy Hour prompt before the arrival on each turn until used, answered `HAPPY_HOUR` or `SKIP`; remaining duration shows 2 on the activation turn and 1 on the next | Fixture line counts are predictable and the sequence diagram has a fixed shape. |
| Display | Money as dollars with two decimals; satisfaction as an integer; paid-customer average with one decimal in `Locale.ROOT`, or `N/A` | Transcripts are byte-identical across machines. |
| Branches and PRs | Branch `<issue-number>-<short-slug>`, one PR per issue, PR body is the filled template, commits authored by the teammate only with no agent co-author trailers | Every PR links to its issue and the contributor list shows the team. |
| Skeleton depth | The API skeleton fully implements enums, records, `MenuItem`, `Menu`, `Money`, `Table`, and `GameState`; rule-bearing classes are stubs that throw `UnsupportedOperationException` naming the owning workstream | Wave 1 self-checks run against real collaborators instead of stubs. |
| Host/Waiter order | Host acts before Waiter, so the Waiter may take the order of a customer seated by the Host that turn | Makes their separate capacities useful and gives a deterministic turn sequence. |
| Happy Hour at turn start | Activate and recover before the scheduled arrival, then display the actionable snapshot | Customers arriving that turn already have 100 satisfaction; the order does not change their recovery. |
| Percentage rounding | Round half up to the nearest cent after combo pricing, after personal discount, and after Happy Hour discount | Maintains integer cents and a consistent order of operations. |
| Cashier tie-break | Earliest `READY_TO_PAY` turn, then earliest arrival | Makes "oldest first" deterministic if two customers qualify together. |

### Arrival table

| Turn | ID | Stage 1 type | Stage 2 and 3 type | Meal Stage 1 | Meal Stage 2 | Meal Stage 3 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | C1 | Regular | Regular | Salad | Salad | Salad |
| 4 | C2 | Regular | VIP | Burger | Burger | Burger |
| 7 | C3 | Regular | Regular | Pasta | Pasta | Salad + Pasta combo |
| 10 | C4 | Regular | Critic | Salad | Pasta | Pasta |
| 13 | C5 | Regular | Regular | Burger | Burger | Burger |
| 16 | C6 | Regular | Regular | Pasta | Pasta | Salad + Pasta combo |

## 7. Shared Java API

This section is the complete cross-workstream surface. An issue references these types and signatures; it does not invent competing ones. Private helpers, fields, and algorithms inside a package belong to that package's owner and are described in the issue, not here. A PR that changes anything in this section updates this section in the same PR and names the affected workstreams.

### 7.1 Packages and owners

| Package under `src/restaurantrush` | Owner | Stage 1 types |
| --- | --- | --- |
| `game` | Turn control and game state | `Main`, `GameSetup`, `GameConfig`, `GameState`, `TurnController`, `Phase`, `Action`, `ActionKind`, `StaffRole`, `ActionResult`, `GameEvent`, `Snapshot`, `CustomerView`, `TableView`, `OrderView`, `TurnStart`, `TurnEnd`, `TurnSummary`, `GameResult` |
| `customer` | Customers and waiting queue | `Customer`, `RegularCustomer`, `CustomerStatus`, `WaitingQueue` |
| `menu` | Menu, orders, and pricing | `MenuItem`, `Menu`, `Order`, `OrderStatus`, `Money` |
| `staff` | Staff and tables | `Table`, `Waiter`, `Chef`, `Cashier` |
| `cli` | CLI and reproducibility | `ConsoleUI`, `CommandParser`; also `fixtures/` and `scripts/replay.sh` |
| `check` | One class per workstream | `CustomerCheck`, `MenuCheck`, `StaffCheck`, `GameCheck`, `CliCheck` |

`Main` and `GameSetup` are the composition root. They are the only files a later stage edits to register new customer types, the Host, the combo, or Happy Hour. Stage 2 adds `VIPCustomer` and `CriticCustomer` to `customer`; Stage 3 adds `Host` to `staff`, `ComboMeal` to `menu`, and the Happy Hour state to `game`, each with the recorded enum constants and overloads named below.

Dependencies between packages: `game` calls `staff`, `customer`, and `menu`; `staff` reads `GameState` and returns `ActionResult` and `GameEvent`, so `game` and `staff` depend on each other by design; `customer` depends on `menu` for `Menu` and `Order`; `cli` depends only on `game` records and enums and never imports a concrete customer class.

### 7.2 Enums and IDs

IDs are plain strings. Customers are `C1` to `C6` in arrival order, orders `O1` onward in the order taken, tables `T1` and `T2`. Lookups are by ID, never by list position.

| Enum | Package | Stage 1 constants | Later additions |
| --- | --- | --- | --- |
| `CustomerStatus` | `customer` | `WAITING`, `SEATED`, `EATING`, `READY_TO_PAY`, `PAID`, `LEFT` | none |
| `OrderStatus` | `menu` | `PLACED` (no units done), `PREPARING` (some units done), `READY`, `SERVED`, `PAID`, `CANCELLED` | none |
| `Phase` | `game` | `TURN_START`, `WAITER`, `CHEF`, `EATING_AND_PAYMENT`, `WAITING_AND_ABANDONMENT`, `TURN_END`, `GAME_OVER` | Stage 3 inserts `HOST` before `WAITER` |
| `ActionKind` | `game` | `SEAT`, `ORDER`, `SERVE`, `PREPARE`, `WAIT` | Stage 3 adds `HAPPY_HOUR` |
| `StaffRole` | `game` | `WAITER`, `CHEF`, `CASHIER` | Stage 3 adds `HOST` |
| `GameResult` | `game` | `VICTORY`, `DEFEAT` | none |

### 7.3 Domain types

A domain object throws `IllegalStateException` on an illegal transition, because reaching one is a programming error. Staff check legality first and return a rejection, so manager mistakes never throw.

```java
package restaurantrush.customer;

public abstract class Customer {
    protected Customer(String id, int arrivalTurn, int arrivalIndex);   // arrivalIndex is 1-based
    public String id();  public int arrivalTurn();  public int arrivalIndex();
    public String typeName();                                            // "Regular"; subclasses override
    public CustomerStatus status();  public int satisfaction();
    public Optional<Order> order();
    public int servedTurn();  public int readyToPayTurn();                // 0 until set

    public abstract int waitingLossPerTurn();                            // Regular 8
    public abstract int discountPercent();                               // Regular 0
    public abstract MenuItem chooseMeal(Menu menu);                      // Regular: menu.cycleChoice(arrivalIndex())

    public boolean isWaitingForTableOrFood();                            // status is WAITING or SEATED
    public void seat();                                                  // WAITING -> SEATED
    public void attachOrder(Order order);                                // SEATED with no order
    public void startEating(int turn);                                   // SEATED with READY order -> EATING, sets servedTurn
    public void becomeReadyToPay(int turn);                              // EATING -> READY_TO_PAY, sets readyToPayTurn
    public void pay();                                                   // READY_TO_PAY -> PAID
    public void leave();                                                 // WAITING or SEATED -> LEFT
    public int loseSatisfaction();                                       // subtracts waitingLossPerTurn, floor 0, returns new value
}

public final class RegularCustomer extends Customer {
    public RegularCustomer(String id, int arrivalTurn, int arrivalIndex);
}

public final class WaitingQueue {
    public void add(Customer customer);
    public Optional<Customer> find(String id);
    public Customer remove(String id);                                   // IllegalArgumentException if absent
    public List<Customer> view();                                        // unmodifiable, arrival order
    public boolean isEmpty();
}
```

```java
package restaurantrush.menu;

public class MenuItem {
    public MenuItem(String name, int priceCents, int preparationUnits);
    public String name();  public int priceCents();  public int preparationUnits();
}

public final class Menu {
    public static Menu base();                                           // Salad 900/1, Burger 1500/1, Pasta 1800/2
    public List<MenuItem> items();                                       // unmodifiable
    public MenuItem cycleChoice(int arrivalIndex);                       // 1 Salad, 2 Burger, 3 Pasta, 4 Salad, ...
}

public final class Order {
    public Order(String id, String customerId, MenuItem item, int customerDiscountPercent, int placedTurn);
                                                                         // billCents = Money.applyDiscount(item.priceCents(), customerDiscountPercent), locked
    public String id();  public String customerId();  public MenuItem item();  public int billCents();
    public OrderStatus status();  public int unitsDone();  public int unitsRequired();
    public int placedTurn();  public int readyTurn();  public int servedTurn();   // 0 until set
    public boolean isPreparable();                                       // PLACED or PREPARING
    public void advanceOneUnit(int turn);                                // -> PREPARING, or READY when complete (sets readyTurn)
    public void markServed(int turn);                                    // READY -> SERVED
    public void markPaid();                                              // SERVED -> PAID
    public void cancel();                                                // PLACED, PREPARING, or READY -> CANCELLED
}

public final class Money {
    public static int applyDiscount(int cents, int percentOff);         // round half up to the cent
    public static String format(int cents);                              // "$18.00"
}
```

Stage 3 adds `ComboMeal extends MenuItem` and an `Order` constructor overload with a `boolean happyHourActive` parameter that applies the second discount after the customer discount.

### 7.4 Staff, tables, and the action boundary

Staff objects hold no state except their role. The controller resolves IDs to objects, enforces phase, and enforces one task per role per turn. Staff enforce domain legality and perform exactly one effect, or return a rejection with a reason and no state change.

```java
package restaurantrush.game;

public record Action(StaffRole role, ActionKind kind, String targetId) {}       // targetId null for WAIT
public record ActionResult(boolean accepted, String message, List<GameEvent> events) {
    public static ActionResult accepted(String message, List<GameEvent> events);
    public static ActionResult rejected(String reason);
}
public record GameEvent(int turn, String text) {}
```

```java
package restaurantrush.staff;

public final class Table {
    public Table(String id);
    public String id();  public boolean isFree();  public Optional<String> occupantId();
    public void seat(String customerId);                                 // free -> occupied
    public void release();                                               // occupied -> free
}

public final class Waiter {
    public List<Action> legalActions(GameState state);                   // SEAT per queued customer if a table is free, ORDER per seated customer without an order,
                                                                         // SERVE per READY order with readyTurn < state.turn(), plus WAIT
    public ActionResult seat(GameState state, String customerId);
    public ActionResult takeOrder(GameState state, String customerId);   // chooses the meal, creates the Order with state.nextOrderId(), attaches it, event names the food
    public ActionResult serve(GameState state, String orderId);
}

public final class Chef {
    public List<Action> legalActions(GameState state);                   // PREPARE per preparable order, plus WAIT
    public ActionResult prepare(GameState state, String orderId);
}

public final class Cashier {
    public Optional<GameEvent> collect(GameState state);                 // pays the oldest customer with readyToPayTurn < state.turn(): order PAID, customer PAID,
                                                                         // table released, state.recordPayment; tie-break by arrival; empty when nobody qualifies
}
```

Stage 3 adds `Host` with `legalActions` and `seat` of the same shape.

### 7.5 Controller, state, and snapshot

```java
package restaurantrush.game;

public record GameConfig(int turns, int tableCount, List<Integer> arrivalTurns) {
    public static GameConfig base();                                     // 18, 2, [1, 4, 7, 10, 13, 16]
}

public final class GameState {                                           // shared store; no rules
    public GameState(GameConfig config, Menu menu);
    public GameConfig config();  public int turn();  public Phase phase();  public Menu menu();  public WaitingQueue queue();
    public List<Customer> customers();  public List<Order> orders();  public List<Table> tables();   // unmodifiable, creation order
    public Optional<Customer> customer(String id);  public Optional<Order> order(String id);  public Optional<Table> table(String id);
    public String nextOrderId();                                         // "O1", "O2", ...
    public void addCustomer(Customer customer);  public void addOrder(Order order);
    public boolean taskUsed(StaffRole role);  public void markTaskUsed(StaffRole role);  public void clearTasks();
    public void setTurn(int turn);  public void setPhase(Phase phase);   // controller only
    public void recordServed();  public void recordLeft();  public void recordPayment(int cents, int satisfaction);
    public int servedCount();  public int leftCount();  public int paidCount();  public int revenueCents();  public int satisfactionSum();
}

public final class TurnController {                                      // the only component that changes turn and phase
    public TurnController(GameState state, Waiter waiter, Chef chef, Cashier cashier, IntFunction<Customer> arrivals);
                                                                         // arrivals maps a 1-based arrival index to a new Customer
    public TurnStart beginTurn();            // TURN_START only: turn + 1, clear tasks, scheduled arrival, phase WAITER; events + snapshot
    public ActionResult submit(Action action);  // WAITER or CHEF only, role must match phase; accepted action or WAIT marks the role done and advances the phase
    public TurnEnd endTurn();                // EATING_AND_PAYMENT only: eating, payment, satisfaction loss, abandonment, summary; result after the last turn
    public Snapshot snapshot();  public Phase phase();  public Optional<StaffRole> currentRole();  public boolean isOver();
}

public record TurnStart(List<GameEvent> events, Snapshot snapshot) {}
public record TurnEnd(List<GameEvent> events, TurnSummary summary, Optional<GameResult> result) {}
public record TurnSummary(int turn, int servedCount, int leftCount, int revenueCents, int paidCount, int satisfactionSum) {}

public record Snapshot(int turn, Phase phase, int revenueCents, int paidCount, int satisfactionSum,
                       List<CustomerView> queue, List<TableView> tables, List<CustomerView> customers,
                       List<OrderView> orders, Optional<StaffRole> currentRole, List<Action> legalActions) {}
public record CustomerView(String id, String typeName, CustomerStatus status, int satisfaction, Optional<String> orderId) {}
public record TableView(String id, Optional<String> occupantId) {}
public record OrderView(String id, String customerId, String itemName, OrderStatus status, int unitsDone, int unitsRequired, int billCents) {}

public final class GameSetup {                                           // composition root
    public static TurnController baseGame();
    public static Customer arrival(int arrivalIndex, int turn);          // Stage 1: always RegularCustomer; Stage 2 follows the arrival table
}

public final class Main { public static void main(String[] args); }      // GameSetup.baseGame(), then ConsoleUI.run
```

The result check is integer arithmetic: `paidCount >= 4`, `revenueCents >= 4500`, and `satisfactionSum >= 60 * paidCount`. The snapshot lists every arrived customer, including those who paid or left, in arrival order. `legalActions` is empty outside a staff phase. Stage 3 adds `happyHourRemaining` to `Snapshot` and the Happy Hour state to `GameState`.

### 7.6 CLI loop and commands

```java
package restaurantrush.cli;

public final class CommandParser {
    public static Optional<Action> parse(String line, StaffRole role);  // empty on error; see errorFor
    public static String errorFor(String line, StaffRole role);          // the message printed when parse is empty
}

public final class ConsoleUI {
    public ConsoleUI(Scanner in, PrintStream out);
    public void run(TurnController controller);                          // the loop below; returns when the game is over
}
```

The loop, fixed so every fixture has the same shape:

1. Call `beginTurn`. Print its events, then the start-of-turn display from the snapshot.
2. For each staff phase in order: print that role's legal actions, print the prompt `Waiter> ` or `Chef> `, read one line, parse it, and `submit`. A parse error or a rejected action prints the message and re-prompts. An accepted action or `WAIT` prints its events and moves on.
3. Call `endTurn`. Print its events, the turn summary, and on the last turn the result.

Commands are case-insensitive: `SEAT C2`, `ORDER C1`, `SERVE O1`, `PREPARE O1`, `WAIT`. If standard input ends before the game finishes, print `Input ended before the game finished.` and exit with status 2. Money prints as `$18.00`, satisfaction as an integer, the paid-customer average with one decimal in `Locale.ROOT` or `N/A`. Stage 3 adds the `Host> ` prompt before `Waiter> ` and the `Manager> ` prompt described in Section 6.

### 7.7 Stage 1 issue waves

| Wave | Issues | Depends on |
| --- | --- | --- |
| 0 | API skeleton (#1) | none |
| 1 | Customers and queue; Menu, orders, and pricing; CLI parser and display | #1 merged |
| 2 | Staff and tables; Turn controller and setup | wave 1 merged, because their self-checks need real customers and orders |
| 3 | Stage 1 fixtures and integration: `victory`, `defeat`, and the `-a`/`-b` pair | wave 2 merged; doubles as the stage gate run-through |

A wave 2 owner can draft against the skeleton while wave 1 is in review, but its PR is not opened until its dependencies have merged and its self-check passes.
