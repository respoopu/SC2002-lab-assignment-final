package restaurantrush.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.ActionResult;
import restaurantrush.game.CustomerView;
import restaurantrush.game.GameConfig;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameResult;
import restaurantrush.game.OrderView;
import restaurantrush.game.Phase;
import restaurantrush.game.Snapshot;
import restaurantrush.game.StaffRole;
import restaurantrush.game.TableView;
import restaurantrush.game.TurnController;
import restaurantrush.game.TurnEnd;
import restaurantrush.game.TurnStart;
import restaurantrush.game.TurnSummary;
import restaurantrush.menu.Money;

public final class ConsoleUI {
    private static final int WIDTH = 64;
    private static final String HEAVY_RULE = "=".repeat(WIDTH);
    private static final String LIGHT_RULE = "-".repeat(WIDTH);
    // Main offers only the base setting (GameConfig), so its turn count is the game length shown.
    private static final int TURNS = GameConfig.base().turns();
    private static final String HAPPY_HOUR_ERROR = "Invalid manager command. Use HAPPY_HOUR or SKIP.";
    private static final String RESULT_PREFIX = "   >> ";
    private static final String ERROR_PREFIX = "   !! ";

    private final Scanner in;
    private final PrintStream out;

    public ConsoleUI(Scanner in, PrintStream out) {
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
    }

    public boolean run(TurnController controller) {
        Objects.requireNonNull(controller);
        out.print(title());
        while (!controller.isOver()) {
            boolean activateHappyHour = false;
            if (controller.snapshot().happyHourAvailable()) {
                Optional<Boolean> choice = readHappyHour(controller.snapshot().turn() + 1);
                if (choice.isEmpty()) {
                    return false;
                }
                activateHappyHour = choice.orElseThrow();
            }
            TurnStart start = controller.beginTurn(activateHappyHour);
            out.print(render(start.snapshot(), start.events()));

            // Host (Stage 3), Waiter, then Chef, as the controller moves the phase; a rejected or unparsed line asks the same role again.
            Optional<StaffRole> headed = Optional.empty();
            while (controller.currentRole().isPresent()) {
                StaffRole role = controller.currentRole().orElseThrow();
                Snapshot snapshot = controller.snapshot();
                if (!headed.equals(Optional.of(role))) {
                    out.print(roleHeading(role));
                    headed = Optional.of(role);
                }
                List<Action> legalActions = snapshot.legalActions();
                printActions(snapshot, legalActions);
                out.print(prompt(role));
                Optional<String> line = readLine();
                if (line.isEmpty()) {
                    return false;
                }

                Optional<Action> action = CommandParser.parse(line.orElseThrow(), role, legalActions);
                if (action.isEmpty()) {
                    printLine(ERROR_PREFIX + CommandParser.errorFor(line.orElseThrow(), role, legalActions));
                    continue;
                }

                ActionResult result = controller.submit(action.orElseThrow());
                if (result.events().isEmpty()) {
                    printLine((result.accepted() ? RESULT_PREFIX : ERROR_PREFIX) + result.message());
                }
                for (GameEvent event : result.events()) {
                    printLine(RESULT_PREFIX + event.text());
                }
            }

            TurnEnd end = controller.endTurn();
            printLine("");
            printLine(LIGHT_RULE);
            printLine(" END OF TURN " + end.summary().turn());
            for (GameEvent event : end.events()) {
                printLine(RESULT_PREFIX + event.text());
            }
            out.print(render(end.summary(), end.result()));
        }
        return true;
    }

    public static String render(Snapshot snapshot) {
        return render(snapshot, List.of());
    }

    // The start-of-turn display with the events that opened the turn under NEWS.
    private static String render(Snapshot snapshot, List<GameEvent> events) {
        StringBuilder text = new StringBuilder();
        appendLine(text, "");
        appendLine(text, HEAVY_RULE);
        String turn = " TURN " + snapshot.turn() + " of " + TURNS;
        // Only a Stage 3 turn starts in the HOST phase, so Stages 1 and 2 show no Happy Hour status.
        String happyHour = snapshot.phase() == Phase.HOST ? happyHourStatus(snapshot.happyHourRemaining()) : "";
        appendLine(text, (turn + " ".repeat(Math.max(1, WIDTH - turn.length() - happyHour.length())) + happyHour)
                .stripTrailing());
        appendLine(text, HEAVY_RULE);
        appendLine(text, " Revenue " + Money.format(snapshot.revenueCents()) + "  |  Paid customers "
                + snapshot.paidCount() + "  |  Average satisfaction "
                + average(snapshot.paidCount(), snapshot.satisfactionSum()));

        if (!events.isEmpty()) {
            appendLine(text, "");
            appendLine(text, " NEWS");
            for (GameEvent event : events) {
                appendLine(text, "   * " + event.text());
            }
        }

        appendLine(text, "");
        appendLine(text, " WAITING QUEUE");
        if (snapshot.queue().isEmpty()) {
            appendLine(text, "   (nobody is waiting)");
        } else {
            for (CustomerView customer : snapshot.queue()) {
                appendLine(text, "   " + pad(customer.id(), 4) + pad(customer.typeName(), 9)
                        + satisfaction(customer.satisfaction()));
            }
        }

        appendLine(text, "");
        appendLine(text, " TABLES");
        for (TableView table : snapshot.tables()) {
            String occupant = table.occupantId()
                    .map(id -> "Customer " + id + " - " + customerStatus(customer(snapshot, id)))
                    .orElse("free");
            appendLine(text, "   Table " + pad(table.id(), 4) + occupant);
        }

        appendLine(text, "");
        appendLine(text, " CUSTOMERS");
        if (snapshot.customers().isEmpty()) {
            appendLine(text, "   (nobody has arrived yet)");
        } else {
            appendLine(text, "   " + pad("ID", 4) + pad("Type", 9) + pad("Satisfaction", 17) + pad("Order", 7) + "Status");
            for (CustomerView customer : snapshot.customers()) {
                appendLine(text, "   " + pad(customer.id(), 4) + pad(customer.typeName(), 9)
                        + pad(satisfaction(customer.satisfaction()), 17) + pad(customer.orderId().orElse("-"), 7)
                        + customerStatus(customer));
            }
        }

        appendLine(text, "");
        appendLine(text, " ORDERS");
        if (snapshot.orders().isEmpty()) {
            appendLine(text, "   (no orders yet)");
        } else {
            appendLine(text, "   " + pad("ID", 4) + pad("Dish", 22) + pad("For", 5) + pad("Units", 6)
                    + String.format(Locale.ROOT, "%8s", "Bill") + "  Status");
            for (OrderView order : snapshot.orders()) {
                appendLine(text, "   " + pad(order.id(), 4) + pad(order.itemName(), 22) + pad(order.customerId(), 5)
                        + pad(order.unitsDone() + "/" + order.unitsRequired(), 6)
                        + String.format(Locale.ROOT, "%8s", Money.format(order.billCents())) + "  "
                        + orderStatus(order));
            }
        }
        return text.toString();
    }

    // The brief's end-of-turn results (Section 3.5), then the final result after the last turn; run prints the heading and events above it.
    public static String render(TurnSummary summary, Optional<GameResult> result) {
        StringBuilder text = new StringBuilder();
        String average = average(summary.paidCount(), summary.satisfactionSum());
        appendLine(text, " Served: " + summary.servedCount() + "   Unhappy departures: " + summary.leftCount()
                + "   Revenue: " + Money.format(summary.revenueCents()));
        appendLine(text, " Average satisfaction: " + average + "   Turns used: " + summary.turn() + " of " + TURNS);
        result.ifPresent(value -> {
            appendLine(text, "");
            appendLine(text, HEAVY_RULE);
            appendLine(text, center("*** " + value + " ***"));
            appendLine(text, center(value == GameResult.VICTORY
                    ? "Well done, manager!"
                    : "Better luck next time, manager."));
            appendLine(text, HEAVY_RULE);
            appendLine(text, " Paid customers: " + summary.paidCount() + "   Revenue: "
                    + Money.format(summary.revenueCents()) + "   Average satisfaction: " + average);
            appendLine(text, " Result: " + value);
        });
        return text.toString();
    }

    private static String title() {
        StringBuilder text = new StringBuilder();
        appendLine(text, HEAVY_RULE);
        appendLine(text, center("R E S T A U R A N T   R U S H"));
        appendLine(text, HEAVY_RULE);
        appendLine(text, " You are the manager. The game lasts " + TURNS + " turns, and each turn you");
        appendLine(text, " choose what your staff do.");
        appendLine(text, "");
        appendLine(text, " At each prompt, type the number next to an action, or type");
        appendLine(text, " the command itself, such as SEAT C1. 0 means wait.");
        return text.toString();
    }

    private static String happyHourStatus(int remaining) {
        return remaining == 0 ? "Happy Hour: off" : "HAPPY HOUR ON (turns left: " + remaining + ")";
    }

    private static String customerStatus(CustomerView customer) {
        return switch (customer.status()) {
            case WAITING -> "Waiting for a table";
            case SEATED -> customer.orderId().isEmpty() ? "Seated, has not ordered" : "Waiting for food";
            case EATING -> "Eating";
            case READY_TO_PAY -> "Ready to pay";
            case PAID -> "Paid";
            case LEFT -> "Left unhappy";
        };
    }

    private static String orderStatus(OrderView order) {
        return switch (order.status()) {
            case PLACED -> "Placed";
            case PREPARING -> "Preparing";
            case READY -> "Ready";
            case SERVED -> "Served";
            case PAID -> "Paid";
            case CANCELLED -> "Cancelled";
        };
    }

    private static CustomerView customer(Snapshot snapshot, String id) {
        for (CustomerView customer : snapshot.customers()) {
            if (customer.id().equals(id)) {
                return customer;
            }
        }
        throw new IllegalArgumentException("No customer " + id + " in the snapshot");
    }

    private static Optional<OrderView> order(Snapshot snapshot, String id) {
        for (OrderView order : snapshot.orders()) {
            if (order.id().equals(id)) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    // A ten-cell bar, one # per full 10 points, then the value.
    private static String satisfaction(int value) {
        int filled = value / 10;
        return "[" + "#".repeat(filled) + "-".repeat(10 - filled) + "]" + String.format(Locale.ROOT, "%4d", value);
    }

    private static String average(int paidCount, int satisfactionSum) {
        return paidCount == 0 ? "N/A" : String.format(Locale.ROOT, "%.1f", (double) satisfactionSum / paidCount);
    }

    private static String roleHeading(StaffRole role) {
        String name = roleName(role);
        return "\n" + LIGHT_RULE + "\n " + name.toUpperCase(Locale.ROOT) + "'S TURN\n What should the " + name + " do?\n";
    }

    private static String roleName(StaffRole role) {
        return switch (role) {
            case HOST -> "Host";
            case WAITER -> "Waiter";
            case CHEF -> "Chef";
        };
    }

    // What an action does, in words, from the snapshot the actions came from.
    private static String describe(Snapshot snapshot, Action action) {
        String id = action.targetId();
        return switch (action.kind()) {
            case SEAT -> "Seat customer " + id + " (" + customer(snapshot, id).typeName() + ")";
            case ORDER -> "Take customer " + id + "'s order";
            case SERVE -> order(snapshot, id)
                    .map(order -> "Serve order " + id + " (" + order.itemName() + ") to " + order.customerId())
                    .orElse("Serve order " + id);
            case PREPARE -> order(snapshot, id)
                    .map(order -> "Prepare order " + id + " (" + order.itemName() + ", " + order.unitsDone() + "/"
                            + order.unitsRequired() + ")")
                    .orElse("Prepare order " + id);
            case WAIT -> "Do nothing";
        };
    }

    // Numbered as CommandParser reads them: the non-WAIT actions from 1 in list order, and WAIT, which 7.4 lists last, as 0.
    private void printActions(Snapshot snapshot, List<Action> actions) {
        int number = 0;
        for (Action action : actions) {
            String command = action.kind() == ActionKind.WAIT ? "WAIT" : action.kind() + " " + action.targetId();
            int shown = action.kind() == ActionKind.WAIT ? 0 : ++number;
            printLine("   [" + shown + "] " + pad(command, 13) + describe(snapshot, action));
        }
    }

    // Asks the manager until the answer is HAPPY_HOUR or SKIP; empty when input ends first.
    private Optional<Boolean> readHappyHour(int turn) {
        printLine("");
        printLine(LIGHT_RULE);
        printLine(" START OF TURN " + turn + " - MANAGER'S DECISION");
        printLine(" Happy Hour is available.");
        printLine(" Type HAPPY_HOUR to start it now, or SKIP.");
        while (true) {
            out.print("Manager> ");
            Optional<String> line = readLine();
            if (line.isEmpty()) {
                return Optional.empty();
            }
            Optional<Boolean> choice = CommandParser.parseHappyHour(line.orElseThrow());
            if (choice.isPresent()) {
                return choice;
            }
            printLine(ERROR_PREFIX + HAPPY_HOUR_ERROR);
        }
    }

    // Reads the answer to the prompt just printed; empty, after the end-of-input message, when input has ended.
    private Optional<String> readLine() {
        out.flush();
        if (!in.hasNextLine()) {
            out.print('\n');
            printLine("Input ended before the game finished.");
            return Optional.empty();
        }
        String line = in.nextLine();
        out.print('\n');
        return Optional.of(line);
    }

    private void printLine(String line) {
        out.print(line);
        out.print('\n');
    }

    private static String prompt(StaffRole role) {
        return roleName(role) + "> ";
    }

    private static String center(String line) {
        return " ".repeat(Math.max(0, (WIDTH - line.length()) / 2)) + line;
    }

    private static String pad(String value, int width) {
        return value.length() >= width ? value + " " : value + " ".repeat(width - value.length());
    }

    private static void appendLine(StringBuilder text, String line) {
        text.append(line).append('\n');
    }
}
