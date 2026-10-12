package restaurantrush.staff;

import java.util.Optional;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;

/** Automatic: at most one payment per turn, oldest first. */
public final class Cashier {
    /** Pays the oldest customer with readyToPayTurn before this turn (tie-break by arrival): order PAID, customer PAID, table released, state.recordPayment. Empty when nobody qualifies. */
    public Optional<GameEvent> collect(GameState state) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }
}
