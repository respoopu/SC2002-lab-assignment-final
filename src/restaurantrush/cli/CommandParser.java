package restaurantrush.cli;

import java.util.Optional;
import restaurantrush.game.Action;
import restaurantrush.game.StaffRole;

/** Pure parsing of one input line for the current role. Accepts only SEAT C<n>, ORDER C<n>, SERVE O<n>, PREPARE O<n>, WAIT, case-insensitive, and only the kinds that belong to the role. Existence and legality are decided by staff. */
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
