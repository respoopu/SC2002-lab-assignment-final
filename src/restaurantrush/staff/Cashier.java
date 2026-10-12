package restaurantrush.staff;

import java.util.Optional;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;

/** Automatic: at most one payment per turn, oldest first. */
public final class Cashier {
    /** Pays the customer with the earliest readyToPayTurn before this turn, then earliest arrival. Effects: order.markPaid, customer.pay, state.tableOf(id).release, state.recordPayment, event. Empty when nobody qualifies. */
    public Optional<GameEvent> collect(GameState state) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }
}
