package restaurantrush.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;
import restaurantrush.game.StaffRole;
import restaurantrush.menu.MenuItem;
import restaurantrush.menu.Money;
import restaurantrush.menu.Order;
import restaurantrush.menu.OrderStatus;

/** One task per turn: seat, take an order, or serve. Rejections change nothing. */
public final class Waiter {
    /** Contract Section 7.4: SEAT per queued customer (queue order) if a table is free, ORDER per seated customer without an order (arrival order), SERVE per READY order with readyTurn before this turn (order-ID order), then WAIT. */
    public List<Action> legalActions(GameState state) {
        List<Action> actions = new ArrayList<>();
        if (Seating.firstFreeTable(state).isPresent()) {
            for (Customer customer : state.queue().view()) {
                actions.add(new Action(StaffRole.WAITER, ActionKind.SEAT, customer.id()));
            }
        }
        for (Customer customer : state.customers()) {
            if (customer.status() == CustomerStatus.SEATED && customer.order().isEmpty()) {
                actions.add(new Action(StaffRole.WAITER, ActionKind.ORDER, customer.id()));
            }
        }
        for (Order order : state.orders()) {
            if (order.status() == OrderStatus.READY && order.readyTurn() < state.turn()) {
                actions.add(new Action(StaffRole.WAITER, ActionKind.SERVE, order.id()));
            }
        }
        actions.add(new Action(StaffRole.WAITER, ActionKind.WAIT, null));
        return List.copyOf(actions);
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
        Optional<Order> found = state.order(orderId);
        if (found.isEmpty()) {
            return ActionResult.rejected(orderId + " is not an order.");
        }
        Order order = found.get();
        if (order.status() != OrderStatus.READY) {
            return ActionResult.rejected(orderId + " is not READY; it is " + order.status() + ".");
        }
        if (order.readyTurn() >= state.turn()) {
            return ActionResult.rejected(orderId + " became READY this turn and can be served next turn.");
        }

        Customer customer = state.customer(order.customerId()).orElseThrow();
        customer.startEating(state.turn());
        order.markServed(state.turn());
        state.recordServed();
        String text = orderId + " served to " + customer.id();
        return ActionResult.accepted(text + ".", List.of(new GameEvent(state.turn(), text)));
    }
}
