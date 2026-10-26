package restaurantrush.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import restaurantrush.customer.Customer;
import restaurantrush.customer.WaitingQueue;
import restaurantrush.menu.Menu;
import restaurantrush.menu.Order;
import restaurantrush.staff.Table;

/** Shared store of game objects and counters. Holds no game rules. */
public final class GameState {
    /** Happy Hour lasts for the activation turn and the next. */
    private static final int HAPPY_HOUR_TURNS = 2;

    private final GameConfig config;
    private final Menu menu;
    private final WaitingQueue queue = new WaitingQueue();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<Table> tables = new ArrayList<>();
    private int turn = 0;
    private Phase phase = Phase.TURN_START;
    private int nextOrderNumber = 1;
    private int servedCount;
    private int leftCount;
    private int paidCount;
    private int revenueCents;
    private int satisfactionSum;
    private int happyHourTurn;

    public GameState(GameConfig config, Menu menu) {
        this.config = config;
        this.menu = menu;
        for (int i = 1; i <= config.tableCount(); i++) {
            tables.add(new Table("T" + i));
        }
    }

    public GameConfig config() { return config; }
    public int turn() { return turn; }
    public Phase phase() { return phase; }
    public Menu menu() { return menu; }
    public WaitingQueue queue() { return queue; }

    public List<Customer> customers() { return Collections.unmodifiableList(customers); }
    public List<Order> orders() { return Collections.unmodifiableList(orders); }
    public List<Table> tables() { return Collections.unmodifiableList(tables); }

    public Optional<Customer> customer(String id) {
        return customers.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public Optional<Order> order(String id) {
        return orders.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    public Optional<Table> table(String id) {
        return tables.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    /** The table whose occupant is this customer. */
    public Optional<Table> tableOf(String customerId) {
        return tables.stream().filter(t -> t.occupantId().filter(customerId::equals).isPresent()).findFirst();
    }

    /** Call only after every check has passed, so a rejection never skips an ID. */
    public String nextOrderId() { return "O" + nextOrderNumber++; }
    public void addCustomer(Customer customer) { customers.add(customer); }
    public void addOrder(Order order) { orders.add(order); }

    /** Controller only. */
    public void setTurn(int turn) { this.turn = turn; }
    /** Controller only. */
    public void setPhase(Phase phase) { this.phase = phase; }

    public void recordServed() { servedCount++; }
    public void recordLeft() { leftCount++; }

    public void recordPayment(int cents, int satisfaction) {
        paidCount++;
        revenueCents += cents;
        satisfactionSum += satisfaction;
    }

    public int servedCount() { return servedCount; }
    public int leftCount() { return leftCount; }
    public int paidCount() { return paidCount; }
    public int revenueCents() { return revenueCents; }
    public int satisfactionSum() { return satisfactionSum; }

    /** True once Happy Hour has been activated; it can be activated only once per game. */
    public boolean happyHourUsed() { return happyHourTurn > 0; }

    /** True on the activation turn and the next. */
    public boolean happyHourActive() { return happyHourRemaining() > 0; }

    /** 2 on the activation turn, 1 on the next, otherwise 0. There is no end-of-turn decrement. */
    public int happyHourRemaining() {
        int elapsed = turn - happyHourTurn;
        return happyHourUsed() && elapsed >= 0 && elapsed < HAPPY_HOUR_TURNS ? HAPPY_HOUR_TURNS - elapsed : 0;
    }

    /** Controller only: records the current turn as the activation turn. Needs TURN_START after the turn has started and no earlier activation; otherwise throws before any change. */
    void activateHappyHour() {
        if (happyHourUsed()) {
            throw new IllegalStateException("Happy Hour was already activated on turn " + happyHourTurn);
        }
        if (phase != Phase.TURN_START || turn < 1) {
            throw new IllegalStateException("Happy Hour activates at the start of a turn, not on turn " + turn + " in phase " + phase);
        }
        happyHourTurn = turn;
    }
}
