package restaurantrush.check;

import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameConfig;
import restaurantrush.game.GameState;
import restaurantrush.menu.Menu;
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
        assert state.tables().stream().allMatch(table -> table.isFree());
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
}
