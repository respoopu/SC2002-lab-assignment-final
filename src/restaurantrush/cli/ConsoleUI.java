package restaurantrush.cli;

import java.io.PrintStream;
import java.util.Scanner;
import restaurantrush.game.TurnController;

/** Text in and out only. Holds no game rules and never imports a concrete customer class. */
public final class ConsoleUI {
    public ConsoleUI(Scanner in, PrintStream out) {
    }

    /** Runs the fixed loop in contract Section 7.6 until the game is over. */
    public void run(TurnController controller) {
        throw new UnsupportedOperationException("Owned by workstream: CLI and reproducibility");
    }
}
