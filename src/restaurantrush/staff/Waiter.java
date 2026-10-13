package restaurantrush.staff;

import java.util.List;
import java.util.Optional;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.game.Action;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;
import restaurantrush.menu.MenuItem;
import restaurantrush.menu.Money;
import restaurantrush.menu.Order;

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
        Optional<Customer> found = state.customer(customerId);
        if (found.isEmpty()) {
            return ActionResult.rejected(customerId + " is not a customer.");
        }
        Customer customer = found.get();
        if (customer.status() != CustomerStatus.SEATED) {
            return ActionResult.rejected(customerId + " is not seated.");
        }
        if (customer.order().isPresent()) {
            return ActionResult.rejected(customerId + " has already ordered.");
        }

        MenuItem item = customer.chooseMeal(state.menu());
        Order order = new Order(state.nextOrderId(), customerId, item, customer.discountPercent(), state.turn());
        state.addOrder(order);
        customer.attachOrder(order);
        String text = customerId + " orders " + item.name() + " (" + Money.format(order.billCents()) + ") as " + order.id();
        return ActionResult.accepted(text + ".", List.of(new GameEvent(state.turn(), text)));
    }

    /** Checks: order READY and readyTurn before this turn. Effects: customer.startEating, order.markServed, state.recordServed, event. */
    public ActionResult serve(GameState state, String orderId) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }
}
