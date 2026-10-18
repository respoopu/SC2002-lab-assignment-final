package restaurantrush.customer;

import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;

/** Loses 5 per waiting turn, gets 10% off, chooses menu.cycleChoice(arrivalIndex()). */
public final class VIPCustomer extends Customer {
    public VIPCustomer(String id, int arrivalTurn, int arrivalIndex) {
        super(id, arrivalTurn, arrivalIndex);
    }

    @Override
    public String typeName() { return "VIP"; }

    @Override
    public int waitingLossPerTurn() { return 5; }

    @Override
    public int discountPercent() { return 10; }

    @Override
    public MenuItem chooseMeal(Menu menu) {
        return menu.cycleChoice(arrivalIndex());
    }
}
