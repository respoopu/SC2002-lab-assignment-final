package restaurantrush.game;

import java.util.ArrayList;
import java.util.List;
import restaurantrush.customer.CriticCustomer;
import restaurantrush.customer.Customer;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.customer.VIPCustomer;
import restaurantrush.menu.ComboMeal;
import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Waiter;

/** Composition root: builds the game and creates arrivals. */
public final class GameSetup {
    private GameSetup() {
    }

    /** The Stage 3 game: new GameState(GameConfig.base(), stage3Menu()), the Host and the other staff, and i -> arrival(i, arrivalTurns.get(i - 1)). */
    public static TurnController baseGame() {
        GameConfig config = GameConfig.base();
        return new TurnController(new GameState(config, stage3Menu()), new Host(), new Waiter(), new Chef(), new Cashier(),
                i -> arrival(i, config.arrivalTurns().get(i - 1)));
    }

    /** Menu.base() with "Salad + Pasta combo" appended, built from that menu's own Salad and Pasta. */
    public static Menu stage3Menu() {
        Menu base = Menu.base();
        List<MenuItem> items = new ArrayList<>(base.items());
        items.add(new ComboMeal("Salad + Pasta combo", base.item("Salad"), base.item("Pasta")));
        return new Menu(items);
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
