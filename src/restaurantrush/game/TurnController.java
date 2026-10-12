package restaurantrush.game;

import java.util.Optional;
import java.util.function.IntFunction;
import restaurantrush.customer.Customer;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;

/** The only component that changes turn and phase. See contract Sections 2 and 7.5. */
public final class TurnController {
    /** arrivals maps a 1-based arrival index to a new Customer. */
    public TurnController(GameState state, Waiter waiter, Chef chef, Cashier cashier, IntFunction<Customer> arrivals) {
    }

    /** TURN_START only: turn + 1, clear tasks, scheduled arrival, phase WAITER. */
    public TurnStart beginTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    /** WAITER or CHEF only; role must match the phase. An accepted action or WAIT marks the role done and advances the phase. */
    public ActionResult submit(Action action) {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    /** EATING_AND_PAYMENT only: eating, payment, satisfaction loss, abandonment, summary; result after the last turn. */
    public TurnEnd endTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    public Snapshot snapshot() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    public Phase phase() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    public Optional<StaffRole> currentRole() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    public boolean isOver() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }
}
