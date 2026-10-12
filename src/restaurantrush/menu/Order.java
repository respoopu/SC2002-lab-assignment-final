package restaurantrush.menu;

/** An order owns its preparation progress and its locked bill. Illegal transitions throw IllegalStateException. */
public final class Order {
    /** billCents is Money.applyDiscount(item.priceCents(), customerDiscountPercent), locked at creation. */
    public Order(String id, String customerId, MenuItem item, int customerDiscountPercent, int placedTurn) {
    }

    public String id() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    public String customerId() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    public MenuItem item() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    public int billCents() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    public OrderStatus status() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    public int unitsDone() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    public int unitsRequired() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    public int placedTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    /** 0 until READY. */
    public int readyTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    /** 0 until SERVED. */
    public int servedTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    /** PLACED or PREPARING. */
    public boolean isPreparable() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    /** To PREPARING, or READY when the last unit is done (sets readyTurn). */
    public void advanceOneUnit(int turn) {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    /** READY to SERVED. */
    public void markServed(int turn) {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    /** SERVED to PAID. */
    public void markPaid() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }

    /** PLACED, PREPARING, or READY to CANCELLED. */
    public void cancel() {
        throw new UnsupportedOperationException("Owned by workstream: Menu, orders, and pricing");
    }
}
