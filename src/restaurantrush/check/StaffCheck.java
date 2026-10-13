package restaurantrush.check;

import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.game.GameConfig;
import restaurantrush.game.GameState;
import restaurantrush.menu.Menu;

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
}
