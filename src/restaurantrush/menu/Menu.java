package restaurantrush.menu;

import java.util.List;

/** The menu catalog. Stage 3 adds the combo entry. */
public final class Menu {
    private final List<MenuItem> items;

    public Menu(List<MenuItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("menu needs at least one item");
        }
        this.items = List.copyOf(items);
    }

    /** Salad $9.00 1 unit, Burger $15.00 1 unit, Pasta $18.00 2 units. */
    public static Menu base() {
        return new Menu(List.of(
                new MenuItem("Salad", 900, 1),
                new MenuItem("Burger", 1500, 1),
                new MenuItem("Pasta", 1800, 2)));
    }

    public List<MenuItem> items() { return items; }

    /** 1-based arrival index cycles the base items: 1 Salad, 2 Burger, 3 Pasta, 4 Salad, ... */
    public MenuItem cycleChoice(int arrivalIndex) {
        if (arrivalIndex < 1) {
            throw new IllegalArgumentException("arrival index is 1-based");
        }
        return items.get((arrivalIndex - 1) % 3);
    }
}
