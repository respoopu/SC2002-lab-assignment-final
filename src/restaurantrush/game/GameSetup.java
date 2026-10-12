package restaurantrush.game;

import restaurantrush.customer.Customer;

/** Composition root. Later stages register new types here. */
public final class GameSetup {
    private GameSetup() {
    }

    public static TurnController baseGame() {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }

    /** Stage 1: always a RegularCustomer. Stage 2 follows the arrival table in contract Section 6. */
    public static Customer arrival(int arrivalIndex, int turn) {
        throw new UnsupportedOperationException("Owned by workstream: Turn control and game state");
    }
}
