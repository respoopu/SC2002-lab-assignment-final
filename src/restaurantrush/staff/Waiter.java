package restaurantrush.staff;

import java.util.List;
import restaurantrush.game.Action;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameState;

/** One task per turn: seat, take an order, or serve. Rejections change nothing. */
public final class Waiter {
    /** SEAT per queued customer if a table is free, ORDER per seated customer without an order, SERVE per READY order with readyTurn before this turn, plus WAIT. */
    public List<Action> legalActions(GameState state) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }

    public ActionResult seat(GameState state, String customerId) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }

    /** Asks the customer to choose, creates the Order with state.nextOrderId(), attaches it, and emits the food-choice event. */
    public ActionResult takeOrder(GameState state, String customerId) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }

    public ActionResult serve(GameState state, String orderId) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }
}
