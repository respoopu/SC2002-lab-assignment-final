package restaurantrush.check;

import java.util.List;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.customer.WaitingQueue;
import restaurantrush.menu.Menu;
import restaurantrush.menu.Order;

/** Self-check for the customer package: Customer transitions, RegularCustomer behavior, and WaitingQueue. */
public final class CustomerCheck {
    public static void main(String[] args) {
        boolean enabled = false;
        assert enabled = true;
        if (!enabled) {
            throw new IllegalStateException("CustomerCheck must run with java -ea");
        }

        newRegular();
        mealChoice();
        seating();
        ordering();
        eatingAndPaying();
        leaving();
        satisfaction();
        queue();

        System.out.print("OK CustomerCheck\n");
    }

    private static void newRegular() {
        Customer c = new RegularCustomer("C1", 1, 1);
        assert c.id().equals("C1");
        assert c.arrivalTurn() == 1;
        assert c.arrivalIndex() == 1;
        assert c.typeName().equals("Regular");
        assert c.status() == CustomerStatus.WAITING;
        assert c.satisfaction() == 100;
        assert c.order().isEmpty();
        assert c.servedTurn() == 0;
        assert c.readyToPayTurn() == 0;
        assert c.waitingLossPerTurn() == 8;
        assert c.discountPercent() == 0;
        assert c.isWaitingForTableOrFood();
    }

    private static void mealChoice() {
        Menu menu = Menu.base();
        assert new RegularCustomer("C1", 1, 1).chooseMeal(menu).name().equals("Salad");
        assert new RegularCustomer("C2", 4, 2).chooseMeal(menu).name().equals("Burger");
        assert new RegularCustomer("C3", 7, 3).chooseMeal(menu).name().equals("Pasta");
        assert new RegularCustomer("C4", 10, 4).chooseMeal(menu).name().equals("Salad");
        assert new RegularCustomer("C6", 16, 6).chooseMeal(menu).name().equals("Pasta");
    }

    private static void seating() {
        Customer c = new RegularCustomer("C1", 1, 1);
        c.seat();
        assert c.status() == CustomerStatus.SEATED;
        assert c.isWaitingForTableOrFood();
        assertThrows(IllegalStateException.class, c::seat);
    }

    private static void ordering() {
        Customer waiting = new RegularCustomer("C1", 1, 1);
        assertThrows(IllegalStateException.class, () -> waiting.attachOrder(order()));

        Customer c = new RegularCustomer("C1", 1, 1);
        c.seat();
        Order order = order();
        c.attachOrder(order);
        assert c.order().orElseThrow() == order;
        assertThrows(IllegalStateException.class, () -> c.attachOrder(order()));
    }

    private static void eatingAndPaying() {
        Customer waiting = new RegularCustomer("C1", 1, 1);
        assertThrows(IllegalStateException.class, () -> waiting.startEating(4));

        Customer noOrder = new RegularCustomer("C2", 4, 2);
        noOrder.seat();
        assertThrows(IllegalStateException.class, () -> noOrder.startEating(4));
        assertThrows(IllegalStateException.class, () -> noOrder.becomeReadyToPay(5));

        Customer c = new RegularCustomer("C3", 7, 3);
        c.seat();
        c.attachOrder(order());
        c.startEating(4);
        assert c.status() == CustomerStatus.EATING;
        assert c.servedTurn() == 4;
        assert !c.isWaitingForTableOrFood();
        assertThrows(IllegalStateException.class, c::pay);

        c.becomeReadyToPay(5);
        assert c.status() == CustomerStatus.READY_TO_PAY;
        assert c.readyToPayTurn() == 5;

        c.pay();
        assert c.status() == CustomerStatus.PAID;
    }

    private static void leaving() {
        Customer waiting = new RegularCustomer("C1", 1, 1);
        waiting.leave();
        assert waiting.status() == CustomerStatus.LEFT;

        Customer seated = new RegularCustomer("C2", 4, 2);
        seated.seat();
        seated.leave();
        assert seated.status() == CustomerStatus.LEFT;

        Customer c = new RegularCustomer("C3", 7, 3);
        c.seat();
        c.attachOrder(order());
        c.startEating(4);
        assertThrows(IllegalStateException.class, c::leave);
        c.becomeReadyToPay(5);
        assertThrows(IllegalStateException.class, c::leave);
        c.pay();
        assertThrows(IllegalStateException.class, c::leave);
    }

    private static void satisfaction() {
        Customer c = new RegularCustomer("C1", 1, 1);
        assert c.loseSatisfaction() == 92;
        assert c.loseSatisfaction() == 84;
        for (int i = 3; i <= 12; i++) {
            c.loseSatisfaction();
        }
        assert c.satisfaction() == 4;
        assert c.loseSatisfaction() == 0;
        assert c.satisfaction() == 0;
        assert c.loseSatisfaction() == 0;

        Customer seated = new RegularCustomer("C2", 4, 2);
        seated.seat();
        assert seated.loseSatisfaction() == 92;

        Customer eating = new RegularCustomer("C3", 7, 3);
        eating.seat();
        eating.attachOrder(order());
        eating.startEating(4);
        assertThrows(IllegalStateException.class, eating::loseSatisfaction);
        eating.becomeReadyToPay(5);
        assertThrows(IllegalStateException.class, eating::loseSatisfaction);
        eating.pay();
        assertThrows(IllegalStateException.class, eating::loseSatisfaction);
        assert eating.satisfaction() == 100;

        Customer left = new RegularCustomer("C4", 10, 4);
        left.leave();
        assertThrows(IllegalStateException.class, left::loseSatisfaction);
        assert left.satisfaction() == 100;
    }

    private static void queue() {
        WaitingQueue q = new WaitingQueue();
        assert q.isEmpty();

        Customer c1 = new RegularCustomer("C1", 1, 1);
        Customer c2 = new RegularCustomer("C2", 4, 2);
        Customer c3 = new RegularCustomer("C3", 7, 3);
        q.add(c1);
        q.add(c2);
        q.add(c3);
        assert !q.isEmpty();
        assert q.view().equals(List.of(c1, c2, c3));
        assert q.find("C2").orElseThrow() == c2;
        assert q.find("C9").isEmpty();

        assert q.remove("C2") == c2;
        assert q.view().equals(List.of(c1, c3));
        assertThrows(IllegalArgumentException.class, () -> q.remove("C9"));
        assertThrows(UnsupportedOperationException.class, () -> q.view().add(c2));
        assert q.view().equals(List.of(c1, c3));
    }

    /** A reference to attach; no Order method is called. */
    private static Order order() {
        return new Order("O1", "C1", Menu.base().item("Pasta"), 0, 2);
    }

    private static void assertThrows(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            assert expected.isInstance(e) : "expected " + expected.getSimpleName() + " but got " + e;
            return;
        }
        assert false : "expected " + expected.getSimpleName();
    }
}
