package restaurantrush.game;

import java.util.List;
import java.util.Optional;

/** Read-only view of the game for the CLI. customers lists every arrived customer, including PAID and LEFT, in arrival order. */
public record Snapshot(int turn, Phase phase, int revenueCents, int paidCount, int satisfactionSum,
                       List<CustomerView> queue, List<TableView> tables, List<CustomerView> customers,
                       List<OrderView> orders, Optional<StaffRole> currentRole, List<Action> legalActions) {
    public Snapshot {
        queue = List.copyOf(queue);
        tables = List.copyOf(tables);
        customers = List.copyOf(customers);
        orders = List.copyOf(orders);
        legalActions = List.copyOf(legalActions);
    }
}
