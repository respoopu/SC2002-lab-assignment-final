package restaurantrush.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.menu.Order;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Table;
import restaurantrush.staff.Waiter;

/** The only component that changes turn and phase. See contract Sections 2 and 7.5. */
public final class TurnController {
    private final GameState state;
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private final IntFunction<Customer> arrivals;

    /** arrivals maps a 1-based arrival index to a new Customer. */
    public TurnController(GameState state, Waiter waiter, Chef chef, Cashier cashier, IntFunction<Customer> arrivals) {
        this.state = state;
        this.waiter = waiter;
        this.chef = chef;
        this.cashier = cashier;
        this.arrivals = arrivals;
    }

    /** TURN_START only. Effects in contract Section 7.5: next turn, scheduled arrival into customers and queue, phase WAITER. */
    public TurnStart beginTurn() {
        requirePhase("beginTurn", Phase.TURN_START);
        state.setTurn(state.turn() + 1);
        List<GameEvent> events = new ArrayList<>();
        int index = state.config().arrivalTurns().indexOf(state.turn()) + 1;
        if (index > 0) {
            Customer customer = arrivals.apply(index);
            state.addCustomer(customer);
            state.queue().add(customer);
            events.add(new GameEvent(state.turn(), customer.id() + " (" + customer.typeName() + ") arrives"));
        }
        state.setPhase(Phase.WAITER);
        return new TurnStart(events, snapshot());
    }

    /** WAITER or CHEF only; the action's role must match the phase. An accepted action or WAIT advances the phase; a rejection changes nothing. */
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
            state.setPhase(state.phase() == Phase.WAITER ? Phase.CHEF : Phase.TURN_END);
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
                views(state.queue().view()), tables, views(state.customers()), orders, currentRole(), legalActions());
    }

    public Phase phase() {
        return state.phase();
    }

    public Optional<StaffRole> currentRole() {
        return switch (state.phase()) {
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
            return ActionResult.accepted(action.role() == StaffRole.WAITER ? "Waiter waits." : "Chef waits.", List.of());
        }
        if ((action.kind() == ActionKind.PREPARE) != (action.role() == StaffRole.CHEF)) {
            return ActionResult.rejected("The " + action.role() + " cannot " + action.kind() + ".");
        }
        return switch (action.kind()) {
            case SEAT -> waiter.seat(state, action.targetId());
            case ORDER -> waiter.takeOrder(state, action.targetId());
            case SERVE -> waiter.serve(state, action.targetId());
            default -> chef.prepare(state, action.targetId());
        };
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
