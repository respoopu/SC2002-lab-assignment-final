package restaurantrush.menu;

public class MenuItem {
    private final String name;
    private final int priceCents;
    private final int preparationUnits;

    public MenuItem(String name, int priceCents, int preparationUnits) {
        if (priceCents < 0 || preparationUnits < 1) {
            throw new IllegalArgumentException("price must be >= 0 and preparation units >= 1");
        }
        this.name = name;
        this.priceCents = priceCents;
        this.preparationUnits = preparationUnits;
    }

    public String name() { return name; }
    public int priceCents() { return priceCents; }
    public int preparationUnits() { return preparationUnits; }

    @Override
    public String toString() { return name; }
}
