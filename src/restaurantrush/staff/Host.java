package restaurantrush.staff;

import java.util.ArrayList;
import java.util.List;
import restaurantrush.customer.Customer;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameState;
import restaurantrush.game.StaffRole;

/** Stage 3: seats at most one queued customer per turn, before the Waiter acts. Rejections change nothing. */
public final class Host {
    /** SEAT per queued customer in queue order if a table is free, then WAIT. */
    public List<Action> legalActions(GameState state) {
        List<Action> actions = new ArrayList<>();
        if (Seating.firstFreeTable(state).isPresent()) {
            for (Customer customer : state.queue().view()) {
                actions.add(new Action(StaffRole.HOST, ActionKind.SEAT, customer.id()));
            }
        }
        actions.add(new Action(StaffRole.HOST, ActionKind.WAIT, null));
        return List.copyOf(actions);
    }

    /** The Waiter's seating rule: customer in queue, a table free; queue.remove, lowest-numbered free table seat, customer.seat, event. */
    public ActionResult seat(GameState state, String customerId) {
        return Seating.seat(state, customerId);
    }
}
