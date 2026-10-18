package restaurantrush.customer;

import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;

/** Loses 12 per waiting turn, pays full price, always chooses Pasta. */
public final class CriticCustomer extends Customer {
    public CriticCustomer(String id, int arrivalTurn, int arrivalIndex) {
        super(id, arrivalTurn, arrivalIndex);
    }

    @Override
    public String typeName() { return "Critic"; }

    @Override
    public int waitingLossPerTurn() { return 12; }

    @Override
    public int discountPercent() { return 0; }

    @Override
    public MenuItem chooseMeal(Menu menu) {
        return menu.item("Pasta");
    }
}
