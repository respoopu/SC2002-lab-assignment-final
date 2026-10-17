package restaurantrush.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;
import restaurantrush.game.StaffRole;
import restaurantrush.menu.Order;
import restaurantrush.menu.OrderStatus;

/** One preparation unit per turn. */
public final class Chef {
    /** PREPARE per preparable order in order-ID order, then WAIT. */
    public List<Action> legalActions(GameState state) {
        List<Action> actions = new ArrayList<>();
        for (Order order : state.orders()) {
            if (order.isPreparable()) {
                actions.add(new Action(StaffRole.CHEF, ActionKind.PREPARE, order.id()));
            }
        }
        actions.add(new Action(StaffRole.CHEF, ActionKind.WAIT, null));
        return List.copyOf(actions);
    }

    /** Checks: order preparable. Effects: order.advanceOneUnit, event. */
    public ActionResult prepare(GameState state, String orderId) {
        Optional<Order> found = state.order(orderId);
        if (found.isEmpty()) {
            return ActionResult.rejected(orderId + " is not an order.");
        }
        Order order = found.get();
        if (!order.isPreparable()) {
            return ActionResult.rejected(orderId + " cannot be prepared; it is " + order.status() + ".");
        }

        order.advanceOneUnit(state.turn());
        String text = order.status() == OrderStatus.READY
                ? orderId + " is READY"
                : orderId + " unit " + order.unitsDone() + " of " + order.unitsRequired() + " done";
        return ActionResult.accepted(text + ".", List.of(new GameEvent(state.turn(), text)));
    }
}
