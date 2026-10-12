package restaurantrush.customer;

import java.util.Optional;
import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;
import restaurantrush.menu.Order;

/** A customer owns its status, satisfaction, and type-specific behavior. Illegal transitions throw IllegalStateException. */
public abstract class Customer {
    private static final int START_SATISFACTION = 100;

    private final String id;
    private final int arrivalTurn;
    private final int arrivalIndex;
    private CustomerStatus status = CustomerStatus.WAITING;
    private int satisfaction = START_SATISFACTION;
    private Order order;
    private int servedTurn;
    private int readyToPayTurn;

    /** arrivalIndex is 1-based. */
    protected Customer(String id, int arrivalTurn, int arrivalIndex) {
        this.id = id;
        this.arrivalTurn = arrivalTurn;
        this.arrivalIndex = arrivalIndex;
    }

    public String id() { return id; }
    public int arrivalTurn() { return arrivalTurn; }
    public int arrivalIndex() { return arrivalIndex; }

    /** Display name such as "Regular". */
    public abstract String typeName();

    public CustomerStatus status() { return status; }
    public int satisfaction() { return satisfaction; }
    public Optional<Order> order() { return Optional.ofNullable(order); }

    /** 0 until served. */
    public int servedTurn() { return servedTurn; }

    /** 0 until ready to pay. */
    public int readyToPayTurn() { return readyToPayTurn; }

    public abstract int waitingLossPerTurn();

    public abstract int discountPercent();

    public abstract MenuItem chooseMeal(Menu menu);

    /** True when status is WAITING or SEATED. */
    public boolean isWaitingForTableOrFood() {
        return status == CustomerStatus.WAITING || status == CustomerStatus.SEATED;
    }

    /** WAITING to SEATED. */
    public void seat() {
        requireStatus("seat", CustomerStatus.WAITING);
        status = CustomerStatus.SEATED;
    }

    /** SEATED with no order. Stores the reference only; a customer never calls a method on its Order. */
    public void attachOrder(Order order) {
        requireStatus("attach an order", CustomerStatus.SEATED);
        if (this.order != null) {
            throw new IllegalStateException(id + " already has an order");
        }
        this.order = order;
    }

    /** SEATED with an order attached to EATING; sets servedTurn. The Waiter checks that the order is READY. */
    public void startEating(int turn) {
        requireStatus("start eating", CustomerStatus.SEATED);
        if (order == null) {
            throw new IllegalStateException(id + " has no order to eat");
        }
        status = CustomerStatus.EATING;
        servedTurn = turn;
    }

    /** EATING to READY_TO_PAY; sets readyToPayTurn. */
    public void becomeReadyToPay(int turn) {
        requireStatus("become ready to pay", CustomerStatus.EATING);
        status = CustomerStatus.READY_TO_PAY;
        readyToPayTurn = turn;
    }

    /** READY_TO_PAY to PAID. */
    public void pay() {
        requireStatus("pay", CustomerStatus.READY_TO_PAY);
        status = CustomerStatus.PAID;
    }

    /** WAITING or SEATED to LEFT. */
    public void leave() {
        if (!isWaitingForTableOrFood()) {
            throw new IllegalStateException(id + " cannot leave while " + status);
        }
        status = CustomerStatus.LEFT;
    }

    /** Subtracts waitingLossPerTurn with a floor of 0 and returns the new value. */
    public int loseSatisfaction() {
        satisfaction = Math.max(0, satisfaction - waitingLossPerTurn());
        return satisfaction;
    }

    private void requireStatus(String action, CustomerStatus required) {
        if (status != required) {
            throw new IllegalStateException(id + " cannot " + action + " while " + status);
        }
    }
}
