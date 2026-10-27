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
    private static final String HAPPY_HOUR_CHOICES = "Happy Hour available: HAPPY_HOUR or SKIP";
    private static final String HAPPY_HOUR_ERROR = "Invalid manager command. Use HAPPY_HOUR or SKIP.";

    private final Scanner in;
    private final PrintStream out;

    public ConsoleUI(Scanner in, PrintStream out) {
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
    }

    public boolean run(TurnController controller) {
        Objects.requireNonNull(controller);
        while (!controller.isOver()) {
            boolean activateHappyHour = false;
            if (controller.snapshot().happyHourAvailable()) {
                Optional<Boolean> choice = readHappyHour();
                if (choice.isEmpty()) {
                    return false;
                }
                activateHappyHour = choice.orElseThrow();
            }
            TurnStart start = controller.beginTurn(activateHappyHour);
            printEvents(start.events());
            out.print(render(start.snapshot()));

            // Host (Stage 3), Waiter, then Chef, as the controller moves the phase; a rejected or unparsed line asks the same role again.
            while (controller.currentRole().isPresent()) {
                StaffRole role = controller.currentRole().orElseThrow();
                List<Action> legalActions = controller.snapshot().legalActions();
                printActions(legalActions);
                out.print(prompt(role));
                Optional<String> line = readLine();
                if (line.isEmpty()) {
                    return false;
                }

                Optional<Action> action = CommandParser.parse(line.orElseThrow(), role, legalActions);
                if (action.isEmpty()) {
                    printLine(CommandParser.errorFor(line.orElseThrow(), role, legalActions));
                    continue;
                }

                ActionResult result = controller.submit(action.orElseThrow());
                if (result.events().isEmpty()) {
                    printLine(result.message());
                }
                printEvents(result.events());
            }

            TurnEnd end = controller.endTurn();
            printEvents(end.events());
            out.print(render(end.summary(), end.result()));
        }
        return true;
    }

    public static String render(Snapshot snapshot) {
        StringBuilder text = new StringBuilder();
        appendLine(text, "Turn " + snapshot.turn());
        appendLine(text, "Revenue: " + Money.format(snapshot.revenueCents()));
        appendLine(text, "Average satisfaction: " + average(snapshot.paidCount(), snapshot.satisfactionSum()));
        // Only a Stage 3 turn starts in the HOST phase, so the Stage 1 and 2 display is unchanged.
        if (snapshot.phase() == Phase.HOST) {
            appendLine(text, "Happy Hour remaining: " + snapshot.happyHourRemaining());
        }

        appendLine(text, "Queue:");
        if (snapshot.queue().isEmpty()) {
            appendLine(text, "  (empty)");
        } else {
            for (CustomerView customer : snapshot.queue()) {
                appendLine(text, "  " + customer.id() + " (" + customer.typeName() + ")");
            }
        }

        appendLine(text, "Tables:");
        for (TableView table : snapshot.tables()) {
            String state = table.occupantId().map(id -> "occupied by " + id).orElse("free");
            appendLine(text, "  " + table.id() + ": " + state);
        }

        appendLine(text, "Customers:");
        for (CustomerView customer : snapshot.customers()) {
            String order = customer.orderId().orElse("NO_ORDER");
            appendLine(text, "  " + customer.id() + " (" + customer.typeName() + "): "
                    + customer.status() + ", satisfaction " + customer.satisfaction() + ", order " + order);
        }

        appendLine(text, "Orders:");
        if (snapshot.orders().isEmpty()) {
            appendLine(text, "  (none)");
        } else {
            for (OrderView order : snapshot.orders()) {
                appendLine(text, "  " + order.id() + ": " + order.itemName() + " for " + order.customerId()
                        + ", " + order.status() + ", " + order.unitsDone() + "/" + order.unitsRequired()
                        + ", " + Money.format(order.billCents()));
            }
        }
        return text.toString();
    }

    public static String render(TurnSummary summary, Optional<GameResult> result) {
        StringBuilder text = new StringBuilder();
        appendLine(text, "End of turn " + summary.turn());
        appendLine(text, "Served: " + summary.servedCount());
        appendLine(text, "Unhappy departures: " + summary.leftCount());
        appendLine(text, "Revenue: " + Money.format(summary.revenueCents()));
        appendLine(text, "Average satisfaction: " + average(summary.paidCount(), summary.satisfactionSum()));
        appendLine(text, "Turns used: " + summary.turn());
        result.ifPresent(value -> appendLine(text, "Result: " + value));
        return text.toString();
    }

    private static String average(int paidCount, int satisfactionSum) {
        return paidCount == 0 ? "N/A" : String.format(Locale.ROOT, "%.1f", (double) satisfactionSum / paidCount);
    }

    // Numbered as CommandParser reads them: the non-WAIT actions from 1 in list order, and WAIT, which 7.4 lists last, as 0.
    private void printActions(List<Action> actions) {
        StringBuilder text = new StringBuilder("Legal actions: ");
        int number = 0;
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            Action action = actions.get(i);
            if (action.kind() == ActionKind.WAIT) {
                text.append("[0] WAIT");
            } else {
                number++;
                text.append("[" + number + "] " + action.kind() + " " + action.targetId());
            }
        }
        printLine(text.toString());
    }

    // Asks the manager until the answer is HAPPY_HOUR or SKIP; empty when input ends first.
    private Optional<Boolean> readHappyHour() {
        while (true) {
            printLine(HAPPY_HOUR_CHOICES);
            out.print("Manager> ");
            Optional<String> line = readLine();
            if (line.isEmpty()) {
                return Optional.empty();
            }
            Optional<Boolean> choice = CommandParser.parseHappyHour(line.orElseThrow());
            if (choice.isPresent()) {
                return choice;
            }
            printLine(HAPPY_HOUR_ERROR);
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

    private void printEvents(List<GameEvent> events) {
        for (GameEvent event : events) {
            printLine(event.text());
        }
    }

    private void printLine(String line) {
        out.print(line);
        out.print('\n');
    }

    private static String prompt(StaffRole role) {
        return switch (role) {
            case HOST -> "Host> ";
            case WAITER -> "Waiter> ";
            case CHEF -> "Chef> ";
        };
    }

    private static void appendLine(StringBuilder text, String line) {
        text.append(line).append('\n');
    }
}