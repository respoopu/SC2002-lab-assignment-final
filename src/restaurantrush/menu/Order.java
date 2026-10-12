package restaurantrush.menu;

/** An order owns its preparation progress and its locked bill. Illegal transitions throw IllegalStateException. */
public final class Order {
    private final String id;
    private final String customerId;
    private final MenuItem item;
    private final int billCents;
    private final int placedTurn;
    private OrderStatus status = OrderStatus.PLACED;
    private int unitsDone;
    private int readyTurn;
    private int servedTurn;

    /** billCents is Money.applyDiscount(item.priceCents(), customerDiscountPercent), locked at creation. */
    public Order(String id, String customerId, MenuItem item, int customerDiscountPercent, int placedTurn) {
        this.id = id;
        this.customerId = customerId;
        this.item = item;
        this.billCents = Money.applyDiscount(item.priceCents(), customerDiscountPercent);
        this.placedTurn = placedTurn;
    }

    public String id() { return id; }
    public String customerId() { return customerId; }
    public MenuItem item() { return item; }
    public int billCents() { return billCents; }
    public OrderStatus status() { return status; }
    public int unitsDone() { return unitsDone; }
    public int unitsRequired() { return item.preparationUnits(); }
    public int placedTurn() { return placedTurn; }

    /** 0 until READY. */
    public int readyTurn() { return readyTurn; }

    /** 0 until SERVED. */
    public int servedTurn() { return servedTurn; }

    /** PLACED or PREPARING. */
    public boolean isPreparable() {
        return status == OrderStatus.PLACED || status == OrderStatus.PREPARING;
    }

    /** To PREPARING, or READY when the last unit is done (sets readyTurn). */
    public void advanceOneUnit(int turn) {
        if (!isPreparable()) {
            throw illegal("advance");
        }
        unitsDone++;
        if (unitsDone == unitsRequired()) {
            status = OrderStatus.READY;
            readyTurn = turn;
        } else {
            status = OrderStatus.PREPARING;
        }
    }

    /** READY to SERVED. */
    public void markServed(int turn) {
        if (status != OrderStatus.READY) {
            throw illegal("serve");
        }
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    /** SERVED to PAID. */
    public void markPaid() {
        if (status != OrderStatus.SERVED) {
            throw illegal("pay");
        }
        status = OrderStatus.PAID;
    }

    /** PLACED, PREPARING, or READY to CANCELLED. */
    public void cancel() {
        if (!isPreparable() && status != OrderStatus.READY) {
            throw illegal("cancel");
        }
        status = OrderStatus.CANCELLED;
    }

    private IllegalStateException illegal(String action) {
        return new IllegalStateException("cannot " + action + " order " + id + " in status " + status);
    }
}
