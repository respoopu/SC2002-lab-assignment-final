package restaurantrush.customer;

import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;

/** Loses 8 per waiting turn, pays full price, chooses menu.cycleChoice(arrivalIndex()). */
public final class RegularCustomer extends Customer {
    public RegularCustomer(String id, int arrivalTurn, int arrivalIndex) {
        super(id, arrivalTurn, arrivalIndex);
    }

    @Override
    public String typeName() { return "Regular"; }

    @Override
    public int waitingLossPerTurn() { return 8; }

    @Override
    public int discountPercent() { return 0; }

    @Override
    public MenuItem chooseMeal(Menu menu) {
        return menu.cycleChoice(arrivalIndex());
    }
}
