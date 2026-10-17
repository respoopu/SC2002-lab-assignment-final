package restaurantrush.staff;

import java.util.Comparator;
import java.util.Optional;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;
import restaurantrush.menu.Money;
import restaurantrush.menu.Order;

/** Automatic: at most one payment per turn, oldest first. */
public final class Cashier {
    private static final Comparator<Customer> OLDEST_FIRST = Comparator
            .comparingInt(Customer::readyToPayTurn)
            .thenComparingInt(Customer::arrivalTurn)
            .thenComparingInt(Customer::arrivalIndex);

    /** Pays the customer with the earliest readyToPayTurn before this turn, then earliest arrival. Effects: order.markPaid, customer.pay, state.tableOf(id).release, state.recordPayment, event. Empty when nobody qualifies. */
    public Optional<GameEvent> collect(GameState state) {
        Optional<Customer> next = state.customers().stream()
                .filter(c -> c.status() == CustomerStatus.READY_TO_PAY && c.readyToPayTurn() < state.turn())
                .min(OLDEST_FIRST);
        if (next.isEmpty()) {
            return Optional.empty();
        }

        Customer customer = next.get();
        Order order = customer.order().orElseThrow();
        Table table = state.tableOf(customer.id()).orElseThrow();
        order.markPaid();
        customer.pay();
        table.release();
        state.recordPayment(order.billCents(), customer.satisfaction());
        return Optional.of(new GameEvent(state.turn(), customer.id() + " paid " + Money.format(order.billCents())
                + " (satisfaction " + customer.satisfaction() + "), " + table.id() + " free"));
    }
}
