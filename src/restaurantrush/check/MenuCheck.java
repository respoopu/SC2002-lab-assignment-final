package restaurantrush.check;

import java.util.List;
import restaurantrush.menu.ComboMeal;
import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;
import restaurantrush.menu.Money;
import restaurantrush.menu.Order;
import restaurantrush.menu.OrderStatus;

/** Self-check for the menu package: Menu, MenuItem, ComboMeal, Money, and Order. Run with java -ea. */
public final class MenuCheck {
    private MenuCheck() {
    }

    public static void main(String[] args) {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (!assertionsEnabled) {
            throw new IllegalStateException("MenuCheck needs assertions enabled: run with java -ea");
        }

        checkNewOrder();
        checkPreparation();
        checkOneUnitItem();
        checkServe();
        checkPay();
        checkCancel();
        checkBill();
        checkComboMeal();
        checkComboValidation();
        checkHappyHourBill();
        checkApplyDiscount();
        checkFormat();
        checkMenu();

        System.out.println("OK MenuCheck");
    }

    private static Order pasta() {
        return new Order("O1", "C1", Menu.base().item("Pasta"), 0, 2);
    }

    private static Order salad() {
        return new Order("O2", "C2", Menu.base().item("Salad"), 0, 1);
    }

    private static Order ready() {
        Order order = salad();
        order.advanceOneUnit(2);
        return order;
    }

    private static Order served() {
        Order order = ready();
        order.markServed(3);
        return order;
    }

    private static Order paid() {
        Order order = served();
        order.markPaid();
        return order;
    }

    private static Order cancelled() {
        Order order = pasta();
        order.cancel();
        return order;
    }

    private static void checkNewOrder() {
        Order order = pasta();
        assert order.id().equals("O1");
        assert order.customerId().equals("C1");
        assert order.item().name().equals("Pasta");
        assert order.billCents() == 1800;
        assert order.status() == OrderStatus.PLACED;
        assert order.unitsDone() == 0;
        assert order.unitsRequired() == 2;
        assert order.placedTurn() == 2;
        assert order.readyTurn() == 0;
        assert order.servedTurn() == 0;
        assert order.isPreparable();
    }

    private static void checkPreparation() {
        Order order = pasta();
        order.advanceOneUnit(2);
        assert order.status() == OrderStatus.PREPARING;
        assert order.unitsDone() == 1;
        assert order.readyTurn() == 0;
        assert order.isPreparable();

        order.advanceOneUnit(3);
        assert order.status() == OrderStatus.READY;
        assert order.unitsDone() == 2;
        assert order.readyTurn() == 3;
        assert !order.isPreparable();

        assertIllegalState(() -> order.advanceOneUnit(4));
        assert order.status() == OrderStatus.READY;
        assert order.unitsDone() == 2;
    }

    private static void checkOneUnitItem() {
        Order order = salad();
        assert order.status() == OrderStatus.PLACED;
        order.advanceOneUnit(5);
        assert order.status() == OrderStatus.READY;
        assert order.unitsDone() == 1;
        assert order.readyTurn() == 5;
    }

    private static void checkServe() {
        Order order = pasta();
        order.advanceOneUnit(2);
        order.advanceOneUnit(3);
        order.markServed(4);
        assert order.status() == OrderStatus.SERVED;
        assert order.servedTurn() == 4;

        assertIllegalState(() -> pasta().markServed(4));
        Order preparing = pasta();
        preparing.advanceOneUnit(2);
        assertIllegalState(() -> preparing.markServed(4));
        assert preparing.status() == OrderStatus.PREPARING;
        assert preparing.servedTurn() == 0;
    }

    private static void checkPay() {
        assert paid().status() == OrderStatus.PAID;
        Order placed = pasta();
        assertIllegalState(placed::markPaid);
        assert placed.status() == OrderStatus.PLACED;
        Order preparing = pasta();
        preparing.advanceOneUnit(2);
        assertIllegalState(preparing::markPaid);
        assert preparing.status() == OrderStatus.PREPARING;
        Order ready = ready();
        assertIllegalState(ready::markPaid);
        assert ready.status() == OrderStatus.READY;
        Order paid = paid();
        assertIllegalState(paid::markPaid);
        assert paid.status() == OrderStatus.PAID;
    }

    private static void checkCancel() {
        Order placed = pasta();
        placed.cancel();
        assert placed.status() == OrderStatus.CANCELLED;
        assert !placed.isPreparable();
        assertIllegalState(() -> placed.advanceOneUnit(3));

        Order preparing = pasta();
        preparing.advanceOneUnit(2);
        preparing.cancel();
        assert preparing.status() == OrderStatus.CANCELLED;
        assert !preparing.isPreparable();

        Order ready = ready();
        ready.cancel();
        assert ready.status() == OrderStatus.CANCELLED;
        assert !ready.isPreparable();

        Order served = served();
        assertIllegalState(served::cancel);
        assert served.status() == OrderStatus.SERVED;
        Order paid = paid();
        assertIllegalState(paid::cancel);
        assert paid.status() == OrderStatus.PAID;

        Order cancelled = cancelled();
        assertIllegalState(cancelled::cancel);
        assertIllegalState(() -> cancelled.advanceOneUnit(3));
        assertIllegalState(() -> cancelled.markServed(3));
        assertIllegalState(cancelled::markPaid);
        assert cancelled.status() == OrderStatus.CANCELLED;
        assert cancelled.unitsDone() == 0;
        assert cancelled.servedTurn() == 0;
    }

    private static void checkBill() {
        assert new Order("O3", "C3", Menu.base().item("Burger"), 10, 1).billCents() == 1350;

        Order order = new Order("O4", "C4", Menu.base().item("Pasta"), 0, 1);
        assert order.billCents() == 1800;
        order.advanceOneUnit(1);
        assert order.billCents() == 1800;
        order.advanceOneUnit(2);
        assert order.billCents() == 1800;
        order.markServed(3);
        assert order.billCents() == 1800;
        order.markPaid();
        assert order.billCents() == 1800;

        Order cancelled = new Order("O5", "C5", Menu.base().item("Burger"), 10, 1);
        cancelled.cancel();
        assert cancelled.billCents() == 1350;
    }

    private static ComboMeal saladPasta() {
        Menu menu = Menu.base();
        return new ComboMeal("Salad + Pasta combo", menu.item("Salad"), menu.item("Pasta"));
    }

    private static void checkComboMeal() {
        Menu menu = Menu.base();
        MenuItem salad = menu.item("Salad");
        MenuItem pasta = menu.item("Pasta");
        ComboMeal combo = new ComboMeal("Salad + Pasta combo", salad, pasta);
        assert combo.first() == salad;
        assert combo.second() == pasta;
        assertItem(combo, "Salad + Pasta combo", 2430, 3);
        assert combo.toString().equals("Salad + Pasta combo");

        MenuItem item = combo;
        Order order = new Order("O6", "C3", item, 0, 8);
        assert order.item() == combo;
        assert order.billCents() == 2430;
        assert order.unitsRequired() == 3;
        order.advanceOneUnit(8);
        assert order.status() == OrderStatus.PREPARING;
        order.advanceOneUnit(9);
        assert order.status() == OrderStatus.PREPARING;
        assert order.unitsDone() == 2;
        order.advanceOneUnit(10);
        assert order.status() == OrderStatus.READY;
        assert order.readyTurn() == 10;

        // 90% of 5 cents is 4.5, rounded half up to 5.
        assertItem(new ComboMeal("Tiny", new MenuItem("A", 3, 1), new MenuItem("B", 2, 2)), "Tiny", 5, 3);
        assertItem(new ComboMeal("Free", new MenuItem("A", 0, 1), new MenuItem("B", 0, 1)), "Free", 0, 2);
        assert new ComboMeal("Burger pair", menu.item("Burger"), menu.item("Burger")).priceCents() == 2700;

        assert menu.items().size() == 3;
        assertItem(salad, "Salad", 900, 1);
        assertItem(pasta, "Pasta", 1800, 2);
    }

    private static void checkComboValidation() {
        Menu menu = Menu.base();
        MenuItem salad = menu.item("Salad");
        MenuItem pasta = menu.item("Pasta");
        assertThrows(() -> new ComboMeal(null, salad, pasta), NullPointerException.class);
        assertThrows(() -> new ComboMeal("Combo", null, pasta), NullPointerException.class);
        assertThrows(() -> new ComboMeal("Combo", salad, null), NullPointerException.class);
        assertIllegalArgument(() -> new ComboMeal("", salad, pasta));
        assertIllegalArgument(() -> new ComboMeal("   ", salad, pasta));
    }

    private static void checkHappyHourBill() {
        Menu menu = Menu.base();
        MenuItem burger = menu.item("Burger");
        assert new Order("O1", "C1", saladPasta(), 0, 7, true).billCents() == 1944;
        assert new Order("O2", "C2", burger, 10, 7, true).billCents() == 1080;
        // VIP combo: 2430 -> 2187 after 10% off -> 1749.6, rounded half up to 1750 after Happy Hour.
        assert new Order("O3", "C3", saladPasta(), 10, 7, false).billCents() == 2187;
        assert new Order("O4", "C4", saladPasta(), 10, 7, true).billCents() == 1750;
        assert new Order("O5", "C5", menu.item("Pasta"), 0, 7, true).billCents() == 1440;
        assert new Order("O6", "C6", new MenuItem("Tiny", 5, 1), 10, 7, true).billCents() == 4;
        // 13 -> 11.7 -> 12 -> 9.6 -> 10; Happy Hour first or a single 28% off would give 9.
        assert new Order("O7", "C7", new MenuItem("Tiny", 13, 1), 10, 7, true).billCents() == 10;

        for (MenuItem item : List.of(menu.item("Salad"), burger, menu.item("Pasta"), saladPasta())) {
            for (int percent : new int[] {0, 10}) {
                assert new Order("O8", "C8", item, percent, 1, false).billCents()
                        == new Order("O8", "C8", item, percent, 1).billCents();
            }
        }

        Order order = new Order("O9", "C9", saladPasta(), 10, 7, true);
        assert order.status() == OrderStatus.PLACED;
        assert order.placedTurn() == 7;
        order.advanceOneUnit(7);
        assert order.billCents() == 1750;
        order.advanceOneUnit(8);
        order.advanceOneUnit(9);
        assert order.billCents() == 1750;
        order.markServed(10);
        assert order.billCents() == 1750;
        order.markPaid();
        assert order.status() == OrderStatus.PAID;
        assert order.billCents() == 1750;

        Order cancelled = new Order("O10", "C10", burger, 10, 7, true);
        cancelled.advanceOneUnit(7);
        cancelled.cancel();
        assert cancelled.billCents() == 1080;

        assertIllegalArgument(() -> new Order("O11", "C11", burger, -1, 7, true));
        assertIllegalArgument(() -> new Order("O12", "C12", burger, 101, 7, false));
    }

    private static void checkApplyDiscount() {
        assert Money.applyDiscount(1500, 10) == 1350;
        assert Money.applyDiscount(1350, 20) == 1080;
        assert Money.applyDiscount(2187, 20) == 1750;
        assert Money.applyDiscount(5, 10) == 5;
        assert Money.applyDiscount(1800, 0) == 1800;
        assertIllegalArgument(() -> Money.applyDiscount(-1, 0));
        assertIllegalArgument(() -> Money.applyDiscount(1500, -1));
        assertIllegalArgument(() -> Money.applyDiscount(1500, 101));
    }

    private static void checkFormat() {
        assert Money.format(1800).equals("$18.00");
        assert Money.format(5).equals("$0.05");
        assert Money.format(2430).equals("$24.30");
        assertIllegalArgument(() -> Money.format(-1));
    }

    private static void checkMenu() {
        Menu menu = Menu.base();
        List<MenuItem> items = menu.items();
        assert items.size() == 3;
        assertItem(items.get(0), "Salad", 900, 1);
        assertItem(items.get(1), "Burger", 1500, 1);
        assertItem(items.get(2), "Pasta", 1800, 2);

        assertItem(menu.item("Pasta"), "Pasta", 1800, 2);
        assertIllegalArgument(() -> menu.item("Soup"));

        String[] cycle = {"Salad", "Burger", "Pasta", "Salad", "Burger", "Pasta"};
        for (int index = 1; index <= cycle.length; index++) {
            assert menu.cycleChoice(index).name().equals(cycle[index - 1]);
        }
        assertIllegalArgument(() -> menu.cycleChoice(0));

        boolean unmodifiable = false;
        try {
            items.add(new MenuItem("Soup", 700, 1));
        } catch (UnsupportedOperationException expected) {
            unmodifiable = true;
        }
        assert unmodifiable;
        assert menu.items().size() == 3;
    }

    private static void assertItem(MenuItem item, String name, int priceCents, int preparationUnits) {
        assert item.name().equals(name);
        assert item.priceCents() == priceCents;
        assert item.preparationUnits() == preparationUnits;
    }

    private static void assertIllegalState(Runnable action) {
        assertThrows(action, IllegalStateException.class);
    }

    private static void assertIllegalArgument(Runnable action) {
        assertThrows(action, IllegalArgumentException.class);
    }

    private static void assertThrows(Runnable action, Class<? extends RuntimeException> expected) {
        try {
            action.run();
        } catch (RuntimeException e) {
            assert expected.isInstance(e) : "expected " + expected.getSimpleName() + " but got " + e;
            return;
        }
        assert false : "expected " + expected.getSimpleName();
    }
}
