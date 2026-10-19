package restaurantrush.game;

import restaurantrush.customer.CriticCustomer;
import restaurantrush.customer.Customer;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.customer.VIPCustomer;
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

    /** Follows the arrival table: VIP for index 2, Critic for index 4, Regular otherwise; ID "C" + arrivalIndex. */
    public static Customer arrival(int arrivalIndex, int turn) {
        String id = "C" + arrivalIndex;
        return switch (arrivalIndex) {
            case 2 -> new VIPCustomer(id, turn, arrivalIndex);
            case 4 -> new CriticCustomer(id, turn, arrivalIndex);
            default -> new RegularCustomer(id, turn, arrivalIndex);
        };
    }
}
