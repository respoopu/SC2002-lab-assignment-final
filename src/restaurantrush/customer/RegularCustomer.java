package restaurantrush.customer;

import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;

/** Loses 8 per waiting turn, pays full price, chooses menu.cycleChoice(arrivalIndex()). */
public final class RegularCustomer extends Customer {
    public RegularCustomer(String id, int arrivalTurn, int arrivalIndex) {
        super(id, arrivalTurn, arrivalIndex);
    }

    @Override
    public int waitingLossPerTurn() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    @Override
    public int discountPercent() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    @Override
    public MenuItem chooseMeal(Menu menu) {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }
}
