package restaurantrush.cli;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.StaffRole;

public final class CommandParser {
    private static final Pattern COMMAND = Pattern.compile("(SEAT|ORDER|SERVE|PREPARE)\\s+([A-Z][0-9]+)");
    private static final Pattern NUMBER = Pattern.compile("[0-9]+");

    private CommandParser() {
    }

    public static Optional<Action> parse(String line, StaffRole role) {
        if (role == null) {
            return Optional.empty();
        }
        String input = normalize(line);
        if (input.equals("WAIT")) {
            return Optional.of(new Action(role, ActionKind.WAIT, null));
        }

        Matcher matcher = COMMAND.matcher(input);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        ActionKind kind = ActionKind.valueOf(matcher.group(1));
        String targetId = matcher.group(2);
        if (targetId.charAt(0) != expectedPrefix(kind) || !allowed(kind, role)) {
            return Optional.empty();
        }
        return Optional.of(new Action(role, kind, targetId));
    }

    // A number picks from the legal actions printed above the prompt: 0 is WAIT, 1 to n the nth non-WAIT action.
    public static Optional<Action> parse(String line, StaffRole role, List<Action> legalActions) {
        String input = normalize(line);
        if (role == null || !NUMBER.matcher(input).matches()) {
            return parse(line, role);
        }
        int number = toNumber(input);
        if (number == 0) {
            return Optional.of(new Action(role, ActionKind.WAIT, null));
        }
        List<Action> numbered = numbered(legalActions);
        return number <= numbered.size() ? Optional.of(numbered.get(number - 1)) : Optional.empty();
    }

    public static String errorFor(String line, StaffRole role, List<Action> legalActions) {
        if (role == null || !NUMBER.matcher(normalize(line)).matches()) {
            return errorFor(line, role);
        }
        return parse(line, role, legalActions).isPresent() ? "" : "That number is not in the list. Choose a number from the list, or type a command.";
    }

    public static String errorFor(String line, StaffRole role) {
        if (role == null) {
            return "A staff role is required.";
        }
        String input = normalize(line);
        if (input.isEmpty()) {
            return "Enter a command for " + roleName(role) + ".";
        }
        if (parse(line, role).isPresent()) {
            return "";
        }

        String verb = firstToken(input);
        if (!isKnown(verb)) {
            return "Unknown command. Use " + commandsFor(role) + ".";
        }
        Matcher matcher = COMMAND.matcher(input);
        if (!matcher.matches()) {
            return "Invalid " + verb + " command. Use " + syntaxFor(verb) + ".";
        }

        ActionKind kind = ActionKind.valueOf(verb);
        if (matcher.group(2).charAt(0) != expectedPrefix(kind)) {
            return "Invalid target for " + verb + ". Use " + syntaxFor(verb) + ".";
        }
        return verb + " is not available to " + roleName(role) + ".";
    }

    private static String normalize(String line) {
        return line == null ? "" : line.strip().toUpperCase(Locale.ROOT);
    }

    private static List<Action> numbered(List<Action> legalActions) {
        return legalActions == null ? List.of()
                : legalActions.stream().filter(action -> action.kind() != ActionKind.WAIT).toList();
    }

    // Digits only, so the one failure is a number too large for an int, which no list can reach.
    private static int toNumber(String digits) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException tooLarge) {
            return Integer.MAX_VALUE;
        }
    }

    private static char expectedPrefix(ActionKind kind) {
        return switch (kind) {
            case SEAT, ORDER -> 'C';
            case SERVE, PREPARE -> 'O';
            case WAIT -> '\0';
        };
    }

    private static boolean allowed(ActionKind kind, StaffRole role) {
        return switch (role) {
            case WAITER -> kind == ActionKind.SEAT || kind == ActionKind.ORDER
                    || kind == ActionKind.SERVE || kind == ActionKind.WAIT;
            case CHEF -> kind == ActionKind.PREPARE || kind == ActionKind.WAIT;
        };
    }

    private static boolean isKnown(String verb) {
        return verb.equals("SEAT") || verb.equals("ORDER") || verb.equals("SERVE")
                || verb.equals("PREPARE") || verb.equals("WAIT");
    }

    private static String syntaxFor(String verb) {
        return switch (verb) {
            case "SEAT", "ORDER" -> verb + " C<n>";
            case "SERVE", "PREPARE" -> verb + " O<n>";
            default -> "WAIT";
        };
    }

    private static String commandsFor(StaffRole role) {
        return role == StaffRole.WAITER
                ? "SEAT C<n>, ORDER C<n>, SERVE O<n>, or WAIT"
                : "PREPARE O<n> or WAIT";
    }

    private static String firstToken(String input) {
        int space = input.indexOf(' ');
        int tab = input.indexOf('\t');
        int end = space < 0 ? tab : tab < 0 ? space : Math.min(space, tab);
        return end < 0 ? input : input.substring(0, end);
    }

    private static String roleName(StaffRole role) {
        return role == StaffRole.WAITER ? "Waiter" : "Chef";
    }
}