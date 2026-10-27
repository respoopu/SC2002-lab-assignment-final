package restaurantrush.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.menu.Order;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Table;
import restaurantrush.staff.Waiter;

/** The only component that changes turn and phase. See contract Sections 2 and 7.5. */
public final class TurnController {
    /** Satisfaction each waiting or seated, unserved customer recovers at the start of an active Happy Hour turn. */
    private static final int HAPPY_HOUR_RECOVERY = 15;

    private final GameState state;
    private final Host host;            // null in the Stage 1 and 2 flow, which has no Host phase and no Happy Hour
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private final IntFunction<Customer> arrivals;

    /** The Stage 1 and 2 flow: Waiter then Chef, with Happy Hour disabled. arrivals maps a 1-based arrival index to a new Customer. */
    public TurnController(GameState state, Waiter waiter, Chef chef, Cashier cashier, IntFunction<Customer> arrivals) {
        this(state, waiter, chef, cashier, arrivals, null);
    }

    /** The Stage 3 flow: Host, Waiter, then Chef, with Happy Hour available once. host must not be null. */
    public TurnController(GameState state, Host host, Waiter waiter, Chef chef, Cashier cashier,
                          IntFunction<Customer> arrivals) {
        this(state, waiter, chef, cashier, arrivals, Objects.requireNonNull(host, "host"));
    }

    private TurnController(GameState state, Waiter waiter, Chef chef, Cashier cashier, IntFunction<Customer> arrivals,
                           Host hostOrNull) {
        this.state = state;
        this.host = hostOrNull;
        this.waiter = waiter;
        this.chef = chef;
        this.cashier = cashier;
        this.arrivals = arrivals;
    }

    /** Begins the next turn without activating Happy Hour. */
    public TurnStart beginTurn() {
        return beginTurn(false);
    }

    /**
     * TURN_START only. Effects in contract Sections 7.5 and 7.10: next turn, Happy Hour activation if requested,
     * recovery while Happy Hour is active, scheduled arrival into customers and queue, then phase HOST (WAITER without
     * a Host). A wrong phase, a repeated activation, or activation without a Host throws before any change.
     */
    public TurnStart beginTurn(boolean activateHappyHour) {
        requirePhase("beginTurn", Phase.TURN_START);
        if (activateHappyHour && host == null) {
            throw new IllegalStateException("Happy Hour needs the Stage 3 game with a Host");
        }
        if (activateHappyHour && state.happyHourUsed()) {
            throw new IllegalStateException("Happy Hour has already been used");
        }

        state.setTurn(state.turn() + 1);
        List<GameEvent> events = new ArrayList<>();
        if (activateHappyHour) {
            state.activateHappyHour();
            events.add(new GameEvent(state.turn(), "Happy Hour activated"));
        }
        if (state.happyHourActive()) {
            recoverWaitingCustomers(events);
        }
        int index = state.config().arrivalTurns().indexOf(state.turn()) + 1;
        if (index > 0) {
            Customer customer = arrivals.apply(index);
            state.addCustomer(customer);
            state.queue().add(customer);
            events.add(new GameEvent(state.turn(), customer.id() + " (" + customer.typeName() + ") arrives"));
        }
        state.setPhase(host == null ? Phase.WAITER : Phase.HOST);
        return new TurnStart(events, snapshot());
    }

    /** HOST, WAITER, or CHEF only; the action's role must match the phase. An accepted action or WAIT advances the phase; a rejection changes nothing. */
    public ActionResult submit(Action action) {
        Optional<StaffRole> role = currentRole();
        if (role.isEmpty()) {
            return ActionResult.rejected("No staff action is expected during " + state.phase() + ".");
        }
        if (action.role() != role.get()) {
            return ActionResult.rejected("It is the " + role.get() + "'s turn, not the " + action.role() + "'s.");
        }

        ActionResult result = dispatch(action);
        if (result.accepted()) {
            state.setPhase(switch (state.phase()) {
                case HOST -> Phase.WAITER;
                case WAITER -> Phase.CHEF;
                default -> Phase.TURN_END;
            });
        }
        return result;
    }

    /** TURN_END only. Effects in contract Section 7.5: eating, payment, satisfaction loss, abandonment, summary; result and GAME_OVER after the last turn, else TURN_START. */
    public TurnEnd endTurn() {
        requirePhase("endTurn", Phase.TURN_END);
        int turn = state.turn();
        List<GameEvent> events = new ArrayList<>();

        for (Customer customer : state.customers()) {
            if (customer.status() == CustomerStatus.EATING && customer.servedTurn() < turn) {
                customer.becomeReadyToPay(turn);
                events.add(new GameEvent(turn, customer.id() + " is ready to pay"));
            }
        }
        cashier.collect(state).ifPresent(events::add);
        for (Customer customer : state.customers()) {
            if (customer.isWaitingForTableOrFood() && customer.loseSatisfaction() == 0) {
                events.add(abandon(customer));
            }
        }

        TurnSummary summary = new TurnSummary(turn, state.servedCount(), state.leftCount(),
                state.revenueCents(), state.paidCount(), state.satisfactionSum());
        Optional<GameResult> result = Optional.empty();
        if (turn == state.config().turns()) {
            result = Optional.of(isVictory() ? GameResult.VICTORY : GameResult.DEFEAT);
            state.setPhase(Phase.GAME_OVER);
        } else {
            state.setPhase(Phase.TURN_START);
        }
        return new TurnEnd(events, summary, result);
    }

    public Snapshot snapshot() {
        List<TableView> tables = state.tables().stream().map(t -> new TableView(t.id(), t.occupantId())).toList();
        List<OrderView> orders = state.orders().stream().map(o -> new OrderView(o.id(), o.customerId(),
                o.item().name(), o.status(), o.unitsDone(), o.unitsRequired(), o.billCents())).toList();
        return new Snapshot(state.turn(), state.phase(), state.revenueCents(), state.paidCount(), state.satisfactionSum(),
                views(state.queue().view()), tables, views(state.customers()), orders, currentRole(), legalActions(),
                isHappyHourAvailable(), host == null ? 0 : state.happyHourRemaining());
    }

    public Phase phase() {
        return state.phase();
    }

    public Optional<StaffRole> currentRole() {
        return switch (state.phase()) {
            case HOST -> Optional.of(StaffRole.HOST);
            case WAITER -> Optional.of(StaffRole.WAITER);
            case CHEF -> Optional.of(StaffRole.CHEF);
            default -> Optional.empty();
        };
    }

    public boolean isOver() {
        return state.phase() == Phase.GAME_OVER;
    }

    /** Hands the action to the staff member who owns it; staff decide legality. */
    private ActionResult dispatch(Action action) {
        if (action.kind() == ActionKind.WAIT) {
            return ActionResult.accepted(switch (action.role()) {
                case HOST -> "Host waits.";
                case WAITER -> "Waiter waits.";
                case CHEF -> "Chef waits.";
            }, List.of());
        }
        String target = action.targetId();
        return switch (action.role()) {
            case HOST -> action.kind() == ActionKind.SEAT ? host.seat(state, target) : cannot(action);
            case WAITER -> switch (action.kind()) {
                case SEAT -> waiter.seat(state, target);
                case ORDER -> waiter.takeOrder(state, target);
                case SERVE -> waiter.serve(state, target);
                default -> cannot(action);
            };
            case CHEF -> action.kind() == ActionKind.PREPARE ? chef.prepare(state, target) : cannot(action);
        };
    }

    private static ActionResult cannot(Action action) {
        return ActionResult.rejected("The " + action.role() + " cannot " + action.kind() + ".");
    }

    /** Arrival order; the customer decides eligibility, so only WAITING and SEATED customers change. No event at the cap. */
    private void recoverWaitingCustomers(List<GameEvent> events) {
        for (Customer customer : state.customers()) {
            int before = customer.satisfaction();
            int after = customer.recover(HAPPY_HOUR_RECOVERY);
            if (after > before) {
                events.add(new GameEvent(state.turn(),
                        customer.id() + " recovers " + (after - before) + " satisfaction (" + after + ")"));
            }
        }
    }

    /** Stage 3 only, before Happy Hour is used, between turns. */
    private boolean isHappyHourAvailable() {
        return host != null && !state.happyHourUsed() && state.phase() == Phase.TURN_START;
    }

    /** Effects in contract Section 7.5 order: leave the queue or free the table, cancel the unserved order, leave, record. */
    private GameEvent abandon(Customer customer) {
        if (customer.status() == CustomerStatus.WAITING) {
            state.queue().remove(customer.id());
        }
        state.tableOf(customer.id()).ifPresent(Table::release);
        // A WAITING or SEATED customer's order is never served, so any attached order is cancellable.
        customer.order().ifPresent(Order::cancel);
        customer.leave();
        state.recordLeft();
        return new GameEvent(state.turn(), customer.id() + " left unhappy");
    }

    private boolean isVictory() {
        return state.paidCount() >= 4 && state.revenueCents() >= 4500
                && state.satisfactionSum() >= 60 * state.paidCount();
    }

    private List<Action> legalActions() {
        return switch (state.phase()) {
            case HOST -> host.legalActions(state);
            case WAITER -> waiter.legalActions(state);
            case CHEF -> chef.legalActions(state);
            default -> List.of();
        };
    }

    private static List<CustomerView> views(List<Customer> customers) {
        return customers.stream().map(c -> new CustomerView(c.id(), c.typeName(), c.status(), c.satisfaction(),
                c.order().map(Order::id))).toList();
    }

    private void requirePhase(String call, Phase required) {
        if (state.phase() != required) {
            throw new IllegalStateException(call + " needs phase " + required + " but the phase is " + state.phase());
        }
    }
}
