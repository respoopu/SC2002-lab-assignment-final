package restaurantrush.staff;

import java.util.List;
import restaurantrush.game.Action;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameState;

/** One task per turn: seat, take an order, or serve. Rejections change nothing. */
public final class Waiter {
    /** Contract Section 7.4: SEAT per queued customer (queue order) if a table is free, ORDER per seated customer without an order (arrival order), SERVE per READY order with readyTurn before this turn (order-ID order), then WAIT. */
    public List<Action> legalActions(GameState state) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }

    /** Checks: customer in queue, a table free. Effects: queue.remove, lowest-numbered free table seat, customer.seat, event. */
    public ActionResult seat(GameState state, String customerId) {
        return Seating.seat(state, customerId);
    }

    /** Checks: customer SEATED with no order. Effects: chooseMeal, new Order with state.nextOrderId(), state.addOrder, customer.attachOrder, event naming the food. */
    public ActionResult takeOrder(GameState state, String customerId) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }

    /** Checks: order READY and readyTurn before this turn. Effects: customer.startEating, order.markServed, state.recordServed, event. */
    public ActionResult serve(GameState state, String orderId) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }
}
