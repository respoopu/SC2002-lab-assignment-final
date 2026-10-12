package restaurantrush.game;

import restaurantrush.customer.Customer;

/** Composition root: builds the game and creates arrivals. */
public final class GameSetup {
    private GameSetup() {
    }

    /** new GameState(GameConfig.base(), Menu.base()), the staff, and i -> arrival(i, arrivalTurns.get(i - 1)). */
    public static TurnController baseGame() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    /** new RegularCustomer("C" + arrivalIndex, turn, arrivalIndex). */
    public static Customer arrival(int arrivalIndex, int turn) {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }
}
