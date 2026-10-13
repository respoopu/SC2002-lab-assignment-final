package restaurantrush.staff;

import java.util.List;
import java.util.Optional;
import restaurantrush.customer.Customer;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;

/** Seating logic shared by the Waiter and, in Stage 3, the Host. */
final class Seating {
    private Seating() {
    }

    /** Lowest-numbered free table; tables are stored in creation order T1, T2, ... */
    static Optional<Table> firstFreeTable(GameState state) {
        return state.tables().stream().filter(Table::isFree).findFirst();
    }

    /** Checks: customer in queue, a table free. Effects: queue.remove, table.seat, customer.seat, event. */
    static ActionResult seat(GameState state, String customerId) {
        if (state.queue().find(customerId).isEmpty()) {
            return ActionResult.rejected(customerId + " is not in the waiting queue.");
        }
        Optional<Table> table = firstFreeTable(state);
        if (table.isEmpty()) {
            return ActionResult.rejected("No table is free.");
        }

        Customer customer = state.queue().remove(customerId);
        table.get().seat(customerId);
        customer.seat();
        String text = customerId + " seated at " + table.get().id();
        return ActionResult.accepted(text + ".", List.of(new GameEvent(state.turn(), text)));
    }
}
