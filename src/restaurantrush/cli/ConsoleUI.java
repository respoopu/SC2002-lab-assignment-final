package restaurantrush.cli;

import java.io.PrintStream;
import java.util.Optional;
import java.util.Scanner;
import restaurantrush.game.GameResult;
import restaurantrush.game.Snapshot;
import restaurantrush.game.TurnController;
import restaurantrush.game.TurnSummary;

/** Text in and out only. Holds no game rules and never imports a concrete customer class. */
public final class ConsoleUI {
    public ConsoleUI(Scanner in, PrintStream out) {
    }

    /** Runs the fixed loop in contract Section 7.6. Returns true when the game finished, false when input ended first. */
    public boolean run(TurnController controller) {
        throw new UnsupportedOperationException("Owned by workstream: CLI and reproducibility");
    }

    /** Start-of-turn display. Pure; uses "\n" only. */
    public static String render(Snapshot snapshot) {
        throw new UnsupportedOperationException("Owned by workstream: CLI and reproducibility");
    }

    /** End-of-turn display, with the result on the last turn. Pure; uses "\n" only. */
    public static String render(TurnSummary summary, Optional<GameResult> result) {
        throw new UnsupportedOperationException("Owned by workstream: CLI and reproducibility");
    }
}
