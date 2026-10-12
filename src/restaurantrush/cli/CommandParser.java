package restaurantrush.cli;

import java.util.Optional;
import restaurantrush.game.Action;
import restaurantrush.game.StaffRole;

/** Pure parsing of one input line for the current role. Case-insensitive: SEAT C2, ORDER C1, SERVE O1, PREPARE O1, WAIT. */
public final class CommandParser {
    private CommandParser() {
    }

    /** Empty when the line is not a well-formed command for this role; see errorFor. */
    public static Optional<Action> parse(String line, StaffRole role) {
        throw new UnsupportedOperationException("Owned by workstream: CLI and reproducibility");
    }

    /** The message printed when parse is empty. */
    public static String errorFor(String line, StaffRole role) {
        throw new UnsupportedOperationException("Owned by workstream: CLI and reproducibility");
    }
}
