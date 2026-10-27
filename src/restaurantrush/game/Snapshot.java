package restaurantrush.game;

import java.util.List;
import java.util.Optional;

/**
 * Read-only view of the game for the CLI. customers lists every arrived customer, including PAID and LEFT, in arrival order.
 * happyHourAvailable says whether the manager may activate Happy Hour before the next turn; happyHourRemaining is its
 * remaining duration. Both are false/0 in a game without a Host.
 */
public record Snapshot(int turn, Phase phase, int revenueCents, int paidCount, int satisfactionSum,
                       List<CustomerView> queue, List<TableView> tables, List<CustomerView> customers,
                       List<OrderView> orders, Optional<StaffRole> currentRole, List<Action> legalActions,
                       boolean happyHourAvailable, int happyHourRemaining) {
    public Snapshot {
        queue = List.copyOf(queue);
        tables = List.copyOf(tables);
        customers = List.copyOf(customers);
        orders = List.copyOf(orders);
        legalActions = List.copyOf(legalActions);
    }

    /** The Stage 1 and 2 snapshot: Happy Hour unavailable, 0 remaining. */
    public Snapshot(int turn, Phase phase, int revenueCents, int paidCount, int satisfactionSum,
                    List<CustomerView> queue, List<TableView> tables, List<CustomerView> customers,
                    List<OrderView> orders, Optional<StaffRole> currentRole, List<Action> legalActions) {
        this(turn, phase, revenueCents, paidCount, satisfactionSum, queue, tables, customers, orders, currentRole,
                legalActions, false, 0);
    }
}
