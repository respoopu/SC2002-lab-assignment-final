package restaurantrush.customer;

import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;

/** Loses 8 per waiting turn, pays full price, chooses menu.cycleChoice(arrivalIndex()) or, at indices divisible by 3, the combo if offered. */
public final class RegularCustomer extends Customer {
    private static final String COMBO_NAME = "Salad + Pasta combo";

    public RegularCustomer(String id, int arrivalTurn, int arrivalIndex) {
        super(id, arrivalTurn, arrivalIndex);
    }

    @Override
    public String typeName() { return "Regular"; }

    @Override
    public int waitingLossPerTurn() { return 8; }

    @Override
    public int discountPercent() { return 0; }

    /** Indices divisible by 3 choose the combo when the menu has it; otherwise the cycle. */
    @Override
    public MenuItem chooseMeal(Menu menu) {
        if (arrivalIndex() % 3 == 0) {
            for (MenuItem item : menu.items()) {
                if (item.name().equals(COMBO_NAME)) {
                    return item;
                }
            }
        }
        return menu.cycleChoice(arrivalIndex());
    }
}
