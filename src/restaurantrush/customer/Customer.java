package restaurantrush.customer;

import java.util.Optional;
import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;
import restaurantrush.menu.Order;

/** A customer owns its status, satisfaction, and type-specific behavior. Illegal transitions throw IllegalStateException. */
public abstract class Customer {
    /** arrivalIndex is 1-based. */
    protected Customer(String id, int arrivalTurn, int arrivalIndex) {
    }

    public String id() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public int arrivalTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public int arrivalIndex() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** Display name such as "Regular". */
    public String typeName() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public CustomerStatus status() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public int satisfaction() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public Optional<Order> order() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** 0 until served. */
    public int servedTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** 0 until ready to pay. */
    public int readyToPayTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public abstract int waitingLossPerTurn();

    public abstract int discountPercent();

    public abstract MenuItem chooseMeal(Menu menu);

    /** True when status is WAITING or SEATED. */
    public boolean isWaitingForTableOrFood() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** WAITING to SEATED. */
    public void seat() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** SEATED with no order. */
    public void attachOrder(Order order) {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** SEATED with a READY order to EATING; sets servedTurn. */
    public void startEating(int turn) {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** EATING to READY_TO_PAY; sets readyToPayTurn. */
    public void becomeReadyToPay(int turn) {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** READY_TO_PAY to PAID. */
    public void pay() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** WAITING or SEATED to LEFT. */
    public void leave() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** Subtracts waitingLossPerTurn with a floor of 0 and returns the new value. */
    public int loseSatisfaction() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }
}
