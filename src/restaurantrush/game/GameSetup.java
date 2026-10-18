package restaurantrush.game;

import restaurantrush.customer.Customer;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.menu.Menu;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;

/** Composition root: builds the game and creates arrivals. */
public final class GameSetup {
    private GameSetup() {
    }

    /** new GameState(GameConfig.base(), Menu.base()), the staff, and i -> arrival(i, arrivalTurns.get(i - 1)). */
    public static TurnController baseGame() {
        GameConfig config = GameConfig.base();
        return new TurnController(new GameState(config, Menu.base()), new Waiter(), new Chef(), new Cashier(),
                i -> arrival(i, config.arrivalTurns().get(i - 1)));
    }

    /** new RegularCustomer("C" + arrivalIndex, turn, arrivalIndex). */
    public static Customer arrival(int arrivalIndex, int turn) {
        return new RegularCustomer("C" + arrivalIndex, turn, arrivalIndex);
    }
}
