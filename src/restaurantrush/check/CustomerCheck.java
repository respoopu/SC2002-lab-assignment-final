package restaurantrush.check;

import java.util.List;
import restaurantrush.customer.CriticCustomer;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.customer.VIPCustomer;
import restaurantrush.customer.WaitingQueue;
import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;
import restaurantrush.menu.Order;

/** Self-check for customer transitions, Regular/VIP/Critic behavior, and WaitingQueue. */
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
        newVip();
        vipMealChoice();
        vipSatisfaction();
        variantTransitions(new VIPCustomer("C2", 4, 2), new VIPCustomer("C5", 13, 5));
        newCritic();
        criticMealChoice();
        criticSatisfaction();
        variantTransitions(new CriticCustomer("C4", 10, 4), new CriticCustomer("C6", 16, 6));
        polymorphicBehavior();
        recovery();
        recoveryIgnoredWhenNotWaiting();
        comboChoice();
        variantsIgnoreCombo();

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

    private static void newVip() {
        Customer vip = new VIPCustomer("C2", 4, 2);
        assert vip.id().equals("C2");
        assert vip.arrivalTurn() == 4;
        assert vip.arrivalIndex() == 2;
        assert vip.typeName().equals("VIP");
        assert vip.status() == CustomerStatus.WAITING;
        assert vip.satisfaction() == 100;
        assert vip.order().isEmpty();
        assert vip.servedTurn() == 0;
        assert vip.readyToPayTurn() == 0;
        assert vip.waitingLossPerTurn() == 5;
        assert vip.discountPercent() == 10;
        assert vip.isWaitingForTableOrFood();
        Order discounted = new Order("O2", vip.id(), Menu.base().item("Burger"), vip.discountPercent(), 5);
        assert discounted.billCents() == 1350;
    }

    private static void vipMealChoice() {
        Menu menu = Menu.base();
        List<String> meals = List.of("Salad", "Burger", "Pasta", "Salad", "Burger", "Pasta");
        for (int index = 1; index <= 6; index++) {
            Customer vip = new VIPCustomer("C" + index, 1 + (index - 1) * 3, index);
            assert vip.chooseMeal(menu).name().equals(meals.get(index - 1));
            assert vip.chooseMeal(menu) == new RegularCustomer("C" + index, 1, index).chooseMeal(menu);
        }
    }

    private static void vipSatisfaction() {
        Customer vip = new VIPCustomer("C2", 4, 2);
        assert vip.loseSatisfaction() == 95;
        assert vip.loseSatisfaction() == 90;
        for (int loss = 3; loss <= 19; loss++) {
            assert vip.loseSatisfaction() == 100 - loss * 5;
        }
        assert vip.loseSatisfaction() == 0;
        assert vip.loseSatisfaction() == 0;
    }

    private static void newCritic() {
        Customer critic = new CriticCustomer("C4", 10, 4);
        assert critic.id().equals("C4");
        assert critic.arrivalTurn() == 10;
        assert critic.arrivalIndex() == 4;
        assert critic.typeName().equals("Critic");
        assert critic.status() == CustomerStatus.WAITING;
        assert critic.satisfaction() == 100;
        assert critic.order().isEmpty();
        assert critic.servedTurn() == 0;
        assert critic.readyToPayTurn() == 0;
        assert critic.waitingLossPerTurn() == 12;
        assert critic.discountPercent() == 0;
        assert critic.isWaitingForTableOrFood();
        Order fullPrice = new Order("O4", critic.id(), critic.chooseMeal(Menu.base()), critic.discountPercent(), 12);
        assert fullPrice.billCents() == 1800;
    }

    private static void criticMealChoice() {
        Menu menu = Menu.base();
        for (int index = 1; index <= 6; index++) {
            Customer critic = new CriticCustomer("C" + index, 1 + (index - 1) * 3, index);
            assert critic.chooseMeal(menu) == menu.item("Pasta");
        }
        assert new RegularCustomer("C4", 10, 4).chooseMeal(menu).name().equals("Salad");
        Menu withoutPasta = new Menu(List.of(menu.item("Salad"), menu.item("Burger"),
                new MenuItem("Soup", 1000, 1)));
        Customer critic = new CriticCustomer("C4", 10, 4);
        assertThrows(IllegalArgumentException.class, () -> critic.chooseMeal(withoutPasta));
    }

    private static void criticSatisfaction() {
        Customer critic = new CriticCustomer("C4", 10, 4);
        assert critic.loseSatisfaction() == 88;
        assert critic.loseSatisfaction() == 76;
        for (int loss = 3; loss <= 8; loss++) {
            assert critic.loseSatisfaction() == 100 - loss * 12;
        }
        assert critic.satisfaction() == 4;
        assert critic.loseSatisfaction() == 0;
        assert critic.loseSatisfaction() == 0;
    }

    private static void polymorphicBehavior() {
        List<Customer> customers = List.of(new RegularCustomer("C1", 1, 1),
                new VIPCustomer("C2", 4, 2), new CriticCustomer("C4", 10, 4));
        assert customers.stream().map(Customer::waitingLossPerTurn).toList().equals(List.of(8, 5, 12));
        assert customers.stream().map(Customer::discountPercent).toList().equals(List.of(0, 10, 0));
        assert customers.get(0).loseSatisfaction() == 92;
    }

    /** Base items plus the Stage 3 combo; choice goes by exact name, so a plain MenuItem stands in for ComboMeal. */
    private static Menu comboMenu() {
        Menu base = Menu.base();
        return new Menu(List.of(base.item("Salad"), base.item("Burger"), base.item("Pasta"),
                new MenuItem("Salad + Pasta combo", 2430, 3)));
    }

    private static void recovery() {
        Customer waiting = new RegularCustomer("C1", 1, 1);
        waiting.loseSatisfaction();
        waiting.loseSatisfaction();
        assert waiting.recover(0) == 84;
        assert waiting.recover(15) == 99;
        assert waiting.recover(15) == 100;
        assert waiting.satisfaction() == 100;
        assert waiting.status() == CustomerStatus.WAITING;

        Customer seated = new VIPCustomer("C2", 4, 2);
        seated.seat();
        for (int i = 0; i < 5; i++) {
            seated.loseSatisfaction();
        }
        assert seated.recover(15) == 90;
        assert seated.recover(Integer.MAX_VALUE) == 100;
        assert seated.status() == CustomerStatus.SEATED;

        Customer low = new CriticCustomer("C4", 10, 4);
        low.loseSatisfaction();
        assertThrows(IllegalArgumentException.class, () -> low.recover(-1));
        assertThrows(IllegalArgumentException.class, () -> low.recover(Integer.MIN_VALUE));
        assert low.satisfaction() == 88;
        assert low.recover(Integer.MAX_VALUE) == 100;
    }

    private static void recoveryIgnoredWhenNotWaiting() {
        Customer c = new RegularCustomer("C3", 7, 3);
        c.seat();
        c.loseSatisfaction();
        c.attachOrder(order());
        c.startEating(4);
        assert c.recover(15) == 92;
        assert c.status() == CustomerStatus.EATING;
        c.becomeReadyToPay(5);
        assert c.recover(15) == 92;
        assert c.status() == CustomerStatus.READY_TO_PAY;
        c.pay();
        assert c.recover(Integer.MAX_VALUE) == 92;
        assert c.status() == CustomerStatus.PAID;
        assertThrows(IllegalArgumentException.class, () -> c.recover(-1));
        assert c.satisfaction() == 92;

        Customer left = new RegularCustomer("C1", 1, 1);
        for (int i = 0; i < 13; i++) {
            left.loseSatisfaction();
        }
        left.leave();
        assert left.recover(15) == 0;
        assert left.status() == CustomerStatus.LEFT;
    }

    private static void comboChoice() {
        Menu combo = comboMenu();
        List<String> meals = List.of("Salad", "Burger", "Salad + Pasta combo", "Salad", "Burger", "Salad + Pasta combo");
        for (int index = 1; index <= 6; index++) {
            Customer regular = new RegularCustomer("C" + index, 1 + (index - 1) * 3, index);
            assert regular.chooseMeal(combo).name().equals(meals.get(index - 1));
        }
        assert new RegularCustomer("C3", 7, 3).chooseMeal(combo) == combo.items().get(3);

        Menu base = Menu.base();
        assert new RegularCustomer("C3", 7, 3).chooseMeal(base) == base.item("Pasta");
        assert new RegularCustomer("C6", 16, 6).chooseMeal(base) == base.item("Pasta");
    }

    private static void variantsIgnoreCombo() {
        Menu combo = comboMenu();
        List<String> meals = List.of("Salad", "Burger", "Pasta", "Salad", "Burger", "Pasta");
        for (int index = 1; index <= 6; index++) {
            Customer vip = new VIPCustomer("C" + index, 1 + (index - 1) * 3, index);
            assert vip.chooseMeal(combo).name().equals(meals.get(index - 1));
            Customer critic = new CriticCustomer("C" + index, 1 + (index - 1) * 3, index);
            assert critic.chooseMeal(combo) == combo.item("Pasta");
        }
    }

    private static void variantTransitions(Customer customer, Customer waiting) {
        assertThrows(IllegalStateException.class, waiting::pay);
        assert waiting.status() == CustomerStatus.WAITING;
        waiting.leave();
        assert waiting.status() == CustomerStatus.LEFT;

        customer.seat();
        assert customer.status() == CustomerStatus.SEATED;
        Order order = new Order("O1", customer.id(), customer.chooseMeal(Menu.base()), customer.discountPercent(), 2);
        customer.attachOrder(order);
        assert customer.order().orElseThrow() == order;
        customer.startEating(4);
        assert customer.status() == CustomerStatus.EATING;
        assert customer.servedTurn() == 4;
        customer.becomeReadyToPay(5);
        assert customer.status() == CustomerStatus.READY_TO_PAY;
        assert customer.readyToPayTurn() == 5;
        customer.pay();
        assert customer.status() == CustomerStatus.PAID;
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
