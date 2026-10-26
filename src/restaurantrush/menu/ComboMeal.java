package restaurantrush.menu;

import java.util.Objects;

/** Two existing menu items sold together: 90% of their combined price, half up, and the sum of their units. */
public final class ComboMeal extends MenuItem {
    private static final int COMBO_DISCOUNT_PERCENT = 10;

    private final MenuItem first;
    private final MenuItem second;

    /** NullPointerException for a null name or component; IllegalArgumentException for a blank name. */
    public ComboMeal(String name, MenuItem first, MenuItem second) {
        super(requireName(name), comboPrice(first, second), first.preparationUnits() + second.preparationUnits());
        this.first = first;
        this.second = second;
    }

    public MenuItem first() { return first; }
    public MenuItem second() { return second; }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "combo name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("combo name must not be blank");
        }
        return name;
    }

    private static int comboPrice(MenuItem first, MenuItem second) {
        Objects.requireNonNull(first, "first component");
        Objects.requireNonNull(second, "second component");
        return Money.applyDiscount(first.priceCents() + second.priceCents(), COMBO_DISCOUNT_PERCENT);
    }
}
