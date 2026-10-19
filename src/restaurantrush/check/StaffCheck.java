package restaurantrush.check;

import java.util.List;
import java.util.Optional;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameConfig;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameState;
import restaurantrush.game.StaffRole;
import restaurantrush.menu.Menu;
import restaurantrush.menu.Order;
import restaurantrush.menu.OrderStatus;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Table;
import restaurantrush.staff.Waiter;

/** Self-check for the staff package: Waiter, Chef, and Cashier. Run with java -ea. */
public final class StaffCheck {
    private StaffCheck() {
    }

    public static void main(String[] args) {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (!assertionsEnabled) {
            throw new IllegalStateException("StaffCheck needs assertions enabled: run with java -ea");
        }

        checkFixture();
        checkSeat();
        checkSeatRejections();
        checkTakeOrder();
        checkTakeOrderRejections();
        checkTakeOrderAfterServing();
        checkPrepare();
        checkPrepareRejections();
        checkServe();
        checkServeRejections();
        checkCollect();
        checkCollectWaitsForLaterTurn();
        checkWaiterLegalActions();
        checkChefLegalActions();
        checkRejectionsLeaveStateUnchanged();

        System.out.println("OK StaffCheck");
    }

    /** A base-setting state on the given turn with customers C1..Cn queued, arriving on turns 1, 2, ... */
    private static GameState stateWithQueue(int turn, int customerCount) {
        GameState state = new GameState(GameConfig.base(), Menu.base());
        state.setTurn(turn);
        for (int index = 1; index <= customerCount; index++) {
            Customer customer = new RegularCustomer("C" + index, index, index);
            state.addCustomer(customer);
            state.queue().add(customer);
        }
        return state;
    }

    private static Customer customer(GameState state, String id) {
        return state.customer(id).orElseThrow();
    }

    private static void checkFixture() {
        GameState state = stateWithQueue(3, 2);
        assert state.turn() == 3;
        assert state.customers().size() == 2;
        assert state.queue().view().size() == 2;
        assert customer(state, "C2").status() == CustomerStatus.WAITING;
        assert state.tables().stream().allMatch(Table::isFree);
        assert state.orders().isEmpty();
    }

    private static String occupant(GameState state, String tableId) {
        return state.table(tableId).orElseThrow().occupantId().orElse(null);
    }

    private static void checkSeat() {
        GameState state = stateWithQueue(1, 3);
        Waiter waiter = new Waiter();

        ActionResult first = waiter.seat(state, "C2");
        assert first.accepted();
        assert first.events().size() == 1;
        assert first.events().get(0).turn() == 1;
        assert first.events().get(0).text().equals("C2 seated at T1");
        assert state.queue().find("C2").isEmpty();
        assert state.queue().view().size() == 2;
        assert "C2".equals(occupant(state, "T1"));
        assert state.tableOf("C2").orElseThrow().id().equals("T1");
        assert customer(state, "C2").status() == CustomerStatus.SEATED;

        ActionResult second = waiter.seat(state, "C1");
        assert second.accepted();
        assert "C1".equals(occupant(state, "T2"));
        assert customer(state, "C1").status() == CustomerStatus.SEATED;
        assert state.queue().view().size() == 1;
    }

    private static void checkSeatRejections() {
        GameState state = stateWithQueue(1, 3);
        Waiter waiter = new Waiter();
        assert waiter.seat(state, "C1").accepted();
        assert waiter.seat(state, "C2").accepted();

        ActionResult full = waiter.seat(state, "C3");
        assert !full.accepted();
        assert !full.message().isEmpty();
        assert full.events().isEmpty();
        assert state.queue().find("C3").isPresent();
        assert customer(state, "C3").status() == CustomerStatus.WAITING;
        assert "C1".equals(occupant(state, "T1"));
        assert "C2".equals(occupant(state, "T2"));

        GameState fresh = stateWithQueue(1, 1);
        for (String id : new String[] {"C1x", "C9", "O1"}) {
            ActionResult unknown = waiter.seat(fresh, id);
            assert !unknown.accepted();
            assert unknown.message().contains(id);
        }
        assert waiter.seat(fresh, "C1").accepted();
        ActionResult again = waiter.seat(fresh, "C1");
        assert !again.accepted();
        assert "C1".equals(occupant(fresh, "T1"));
        assert fresh.table("T2").orElseThrow().isFree();
        assert fresh.queue().isEmpty();
        assert fresh.orders().isEmpty();
    }

    private static GameState stateWithSeated(int turn) {
        GameState state = stateWithQueue(1, 3);
        Waiter waiter = new Waiter();
        assert waiter.seat(state, "C1").accepted();
        assert waiter.seat(state, "C2").accepted();
        state.setTurn(turn);
        return state;
    }

    private static void checkTakeOrder() {
        GameState state = stateWithSeated(2);
        Waiter waiter = new Waiter();

        ActionResult result = waiter.takeOrder(state, "C2");
        assert result.accepted();
        assert result.events().size() == 1;
        assert result.events().get(0).turn() == 2;
        assert result.events().get(0).text().equals("C2 orders Burger ($15.00) as O1");
        assert state.orders().size() == 1;
        Order order = state.order("O1").orElseThrow();
        assert order.customerId().equals("C2");
        assert order.item().name().equals("Burger");
        assert order.billCents() == 1500;
        assert order.placedTurn() == 2;
        assert order.status() == OrderStatus.PLACED;
        assert customer(state, "C2").order().orElseThrow() == order;
        assert customer(state, "C2").status() == CustomerStatus.SEATED;

        assert waiter.takeOrder(state, "C1").accepted();
        Order salad = state.order("O2").orElseThrow();
        assert salad.item().name().equals("Salad");
        assert salad.billCents() == 900;
    }

    private static void checkTakeOrderRejections() {
        GameState state = stateWithSeated(2);
        Waiter waiter = new Waiter();
        assert waiter.takeOrder(state, "C1").accepted();

        for (String id : new String[] {"C1", "C3", "C9"}) {
            ActionResult rejected = waiter.takeOrder(state, id);
            assert !rejected.accepted();
            assert rejected.message().contains(id);
            assert rejected.events().isEmpty();
        }
        assert waiter.takeOrder(state, "C1").message().equals("C1 has already ordered.");
        assert waiter.takeOrder(state, "C3").message().equals("C3 is not seated.");
        assert state.orders().size() == 1;
        assert customer(state, "C3").order().isEmpty();
        assert customer(state, "C3").status() == CustomerStatus.WAITING;

        assert waiter.takeOrder(state, "C2").accepted();
        assert state.order("O2").orElseThrow().customerId().equals("C2");
        assert state.orders().size() == 2;
    }

    /** A customer who is eating or ready to pay is no longer SEATED, but the reason given is the earlier order. */
    private static void checkTakeOrderAfterServing() {
        GameState state = stateWithSeated(2);
        Waiter waiter = new Waiter();
        assert waiter.takeOrder(state, "C1").accepted();
        assert new Chef().prepare(state, "O1").accepted();
        state.setTurn(3);
        assert waiter.serve(state, "O1").accepted();

        assert customer(state, "C1").status() == CustomerStatus.EATING;
        ActionResult eating = waiter.takeOrder(state, "C1");
        assert !eating.accepted();
        assert eating.message().equals("C1 has already ordered.");
        assert eating.events().isEmpty();

        customer(state, "C1").becomeReadyToPay(4);
        ActionResult readyToPay = waiter.takeOrder(state, "C1");
        assert !readyToPay.accepted();
        assert readyToPay.message().equals("C1 has already ordered.");
        assert readyToPay.events().isEmpty();

        customer(state, "C1").pay();
        assert waiter.takeOrder(state, "C1").message().equals("C1 has already ordered.");
        assert state.orders().size() == 1;
    }

    private static GameState stateWithPastaOrder() {
        GameState state = stateWithQueue(1, 3);
        Waiter waiter = new Waiter();
        assert waiter.seat(state, "C3").accepted();
        state.setTurn(2);
        assert waiter.takeOrder(state, "C3").accepted();
        return state;
    }

    private static void checkPrepare() {
        GameState state = stateWithPastaOrder();
        Chef chef = new Chef();
        Order order = state.order("O1").orElseThrow();

        ActionResult first = chef.prepare(state, "O1");
        assert first.accepted();
        assert first.events().size() == 1;
        assert first.events().get(0).text().equals("O1 unit 1 of 2 done");
        assert order.status() == OrderStatus.PREPARING;
        assert order.unitsDone() == 1;
        assert order.readyTurn() == 0;

        state.setTurn(3);
        ActionResult second = chef.prepare(state, "O1");
        assert second.accepted();
        assert second.events().get(0).turn() == 3;
        assert second.events().get(0).text().equals("O1 is READY");
        assert order.status() == OrderStatus.READY;
        assert order.unitsDone() == 2;
        assert order.readyTurn() == 3;
    }

    private static void checkPrepareRejections() {
        GameState state = stateWithPastaOrder();
        Chef chef = new Chef();
        Order order = state.order("O1").orElseThrow();
        assert chef.prepare(state, "O1").accepted();
        assert chef.prepare(state, "O1").accepted();

        ActionResult ready = chef.prepare(state, "O1");
        assert !ready.accepted();
        assert ready.message().contains("O1");
        assert ready.events().isEmpty();
        assert order.status() == OrderStatus.READY;
        assert order.unitsDone() == 2;

        ActionResult unknown = chef.prepare(state, "O9");
        assert !unknown.accepted();
        assert unknown.message().contains("O9");
        assert state.orders().size() == 1;
    }

    private static void checkServe() {
        GameState state = stateWithPastaOrder();
        Chef chef = new Chef();
        Waiter waiter = new Waiter();
        assert chef.prepare(state, "O1").accepted();
        state.setTurn(3);
        assert chef.prepare(state, "O1").accepted();

        ActionResult sameTurn = waiter.serve(state, "O1");
        assert !sameTurn.accepted();
        assert sameTurn.message().contains("O1");
        assert sameTurn.events().isEmpty();
        assert state.order("O1").orElseThrow().status() == OrderStatus.READY;
        assert customer(state, "C3").status() == CustomerStatus.SEATED;
        assert state.servedCount() == 0;

        state.setTurn(4);
        ActionResult served = waiter.serve(state, "O1");
        assert served.accepted();
        assert served.events().size() == 1;
        assert served.events().get(0).turn() == 4;
        assert served.events().get(0).text().equals("O1 served to C3");
        assert state.order("O1").orElseThrow().status() == OrderStatus.SERVED;
        assert state.order("O1").orElseThrow().servedTurn() == 4;
        assert customer(state, "C3").status() == CustomerStatus.EATING;
        assert customer(state, "C3").servedTurn() == 4;
        assert state.servedCount() == 1;
        assert "C3".equals(occupant(state, "T1"));

        assert !waiter.serve(state, "O1").accepted();
        assert state.servedCount() == 1;
    }

    private static void checkServeRejections() {
        GameState state = stateWithPastaOrder();
        Waiter waiter = new Waiter();

        ActionResult placed = waiter.serve(state, "O1");
        assert !placed.accepted();
        assert placed.message().contains("O1");
        assert state.order("O1").orElseThrow().status() == OrderStatus.PLACED;

        assert new Chef().prepare(state, "O1").accepted();
        state.setTurn(5);
        ActionResult preparing = waiter.serve(state, "O1");
        assert !preparing.accepted();
        assert state.order("O1").orElseThrow().status() == OrderStatus.PREPARING;

        ActionResult unknown = waiter.serve(state, "O9");
        assert !unknown.accepted();
        assert unknown.message().contains("O9");
        assert customer(state, "C3").status() == CustomerStatus.SEATED;
        assert state.servedCount() == 0;
    }

    private static GameState stateWithTwoReadyToPay() {
        GameState state = stateWithQueue(1, 2);
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        customer(state, "C1").loseSatisfaction();
        assert waiter.seat(state, "C2").accepted();
        assert waiter.seat(state, "C1").accepted();
        state.setTurn(2);
        assert waiter.takeOrder(state, "C2").accepted();
        assert waiter.takeOrder(state, "C1").accepted();
        assert chef.prepare(state, "O1").accepted();
        assert chef.prepare(state, "O2").accepted();
        state.setTurn(4);
        assert waiter.serve(state, "O1").accepted();
        assert waiter.serve(state, "O2").accepted();
        state.setTurn(5);
        customer(state, "C1").becomeReadyToPay(5);
        customer(state, "C2").becomeReadyToPay(5);
        return state;
    }

    private static void checkCollect() {
        Cashier cashier = new Cashier();
        assert cashier.collect(stateWithQueue(3, 2)).isEmpty();

        GameState state = stateWithTwoReadyToPay();
        state.setTurn(6);
        Optional<GameEvent> first = cashier.collect(state);
        assert first.isPresent();
        assert first.get().turn() == 6;
        assert first.get().text().equals("C1 paid $9.00 (satisfaction 92), T2 free");
        assert customer(state, "C1").status() == CustomerStatus.PAID;
        assert state.order("O2").orElseThrow().status() == OrderStatus.PAID;
        assert state.table("T2").orElseThrow().isFree();
        assert state.paidCount() == 1;
        assert state.revenueCents() == 900;
        assert state.satisfactionSum() == 92;

        assert customer(state, "C2").status() == CustomerStatus.READY_TO_PAY;
        assert state.order("O1").orElseThrow().status() == OrderStatus.SERVED;
        assert "C2".equals(occupant(state, "T1"));

        state.setTurn(7);
        Optional<GameEvent> second = cashier.collect(state);
        assert second.orElseThrow().text().equals("C2 paid $15.00 (satisfaction 100), T1 free");
        assert state.paidCount() == 2;
        assert state.revenueCents() == 2400;
        assert state.satisfactionSum() == 192;
        assert cashier.collect(state).isEmpty();
        assert state.paidCount() == 2;
    }

    private static void checkCollectWaitsForLaterTurn() {
        GameState state = stateWithTwoReadyToPay();
        assert new Cashier().collect(state).isEmpty();
        assert customer(state, "C1").status() == CustomerStatus.READY_TO_PAY;
        assert customer(state, "C2").status() == CustomerStatus.READY_TO_PAY;
        assert state.paidCount() == 0;
        assert state.revenueCents() == 0;
        assert !state.table("T1").orElseThrow().isFree();
        assert !state.table("T2").orElseThrow().isFree();
    }

    private static Action waiter(ActionKind kind, String targetId) {
        return new Action(StaffRole.WAITER, kind, targetId);
    }

    private static Action chef(ActionKind kind, String targetId) {
        return new Action(StaffRole.CHEF, kind, targetId);
    }

    private static void checkWaiterLegalActions() {
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        assert waiter.legalActions(stateWithQueue(1, 0)).equals(List.of(waiter(ActionKind.WAIT, null)));

        GameState state = stateWithQueue(1, 3);
        assert waiter.seat(state, "C3").accepted();
        assert waiter.legalActions(state).equals(List.of(
                waiter(ActionKind.SEAT, "C1"),
                waiter(ActionKind.SEAT, "C2"),
                waiter(ActionKind.ORDER, "C3"),
                waiter(ActionKind.WAIT, null)));

        assert waiter.seat(state, "C1").accepted();
        assert waiter.legalActions(state).equals(List.of(
                waiter(ActionKind.ORDER, "C1"),
                waiter(ActionKind.ORDER, "C3"),
                waiter(ActionKind.WAIT, null)));

        GameState full = stateWithQueue(1, 4);
        assert waiter.seat(full, "C1").accepted();
        full.setTurn(2);
        assert waiter.takeOrder(full, "C1").accepted();
        assert waiter.seat(full, "C3").accepted();
        assert chef.prepare(full, "O1").accepted();
        full.setTurn(3);
        assert waiter.legalActions(full).equals(List.of(
                waiter(ActionKind.ORDER, "C3"),
                waiter(ActionKind.SERVE, "O1"),
                waiter(ActionKind.WAIT, null)));

        GameState readyThisTurn = stateWithPastaOrder();
        assert chef.prepare(readyThisTurn, "O1").accepted();
        readyThisTurn.setTurn(3);
        assert chef.prepare(readyThisTurn, "O1").accepted();
        assert waiter.legalActions(readyThisTurn).equals(List.of(
                waiter(ActionKind.SEAT, "C1"),
                waiter(ActionKind.SEAT, "C2"),
                waiter(ActionKind.WAIT, null)));
    }

    private static void checkChefLegalActions() {
        Chef chef = new Chef();
        GameState state = stateWithSeated(2);
        assert chef.legalActions(state).equals(List.of(chef(ActionKind.WAIT, null)));

        Waiter waiter = new Waiter();
        assert waiter.takeOrder(state, "C2").accepted();
        assert waiter.takeOrder(state, "C1").accepted();
        assert chef.legalActions(state).equals(List.of(
                chef(ActionKind.PREPARE, "O1"),
                chef(ActionKind.PREPARE, "O2"),
                chef(ActionKind.WAIT, null)));

        assert chef.prepare(state, "O1").accepted();
        assert chef.legalActions(state).equals(List.of(
                chef(ActionKind.PREPARE, "O2"),
                chef(ActionKind.WAIT, null)));
    }

    private static String fingerprint(GameState state) {
        StringBuilder text = new StringBuilder();
        for (Customer customer : state.customers()) {
            text.append(customer.id()).append(' ').append(customer.status()).append(' ').append(customer.satisfaction())
                    .append(' ').append(customer.order().map(Order::id).orElse("-")).append('\n');
        }
        for (Order order : state.orders()) {
            text.append(order.id()).append(' ').append(order.status()).append(' ').append(order.unitsDone())
                    .append(' ').append(order.readyTurn()).append(' ').append(order.servedTurn()).append('\n');
        }
        state.tables().forEach(table -> text.append(table.id()).append(' ').append(table.occupantId().orElse("-")).append('\n'));
        state.queue().view().forEach(customer -> text.append("queued ").append(customer.id()).append('\n'));
        text.append(state.servedCount()).append(' ').append(state.paidCount()).append(' ').append(state.revenueCents())
                .append(' ').append(state.satisfactionSum());
        return text.toString();
    }

    private static void checkRejectionsLeaveStateUnchanged() {
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        GameState state = stateWithQueue(1, 4);
        assert waiter.seat(state, "C1").accepted();
        assert waiter.seat(state, "C3").accepted();
        state.setTurn(2);
        assert waiter.takeOrder(state, "C1").accepted();
        assert waiter.takeOrder(state, "C3").accepted();
        assert chef.prepare(state, "O1").accepted();
        assert chef.prepare(state, "O2").accepted();

        String before = fingerprint(state);
        List<ActionResult> rejections = List.of(
                waiter.seat(state, "C2"),
                waiter.seat(state, "C9"),
                waiter.takeOrder(state, "C1"),
                waiter.takeOrder(state, "C2"),
                waiter.takeOrder(state, "C9"),
                waiter.serve(state, "O1"),
                waiter.serve(state, "O2"),
                waiter.serve(state, "O9"),
                chef.prepare(state, "O1"),
                chef.prepare(state, "O9"));
        for (ActionResult result : rejections) {
            assert !result.accepted();
            assert !result.message().isEmpty();
            assert result.events().isEmpty();
        }
        assert fingerprint(state).equals(before);

        state.setTurn(3);
        assert waiter.serve(state, "O1").accepted();
        assert !fingerprint(state).equals(before);
        assert state.orders().size() == 2;
        assert state.nextOrderId().equals("O3");
    }
}
