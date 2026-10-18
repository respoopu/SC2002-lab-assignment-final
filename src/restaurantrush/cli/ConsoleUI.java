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
import restaurantrush.game.Snapshot;
import restaurantrush.game.StaffRole;
import restaurantrush.game.TableView;
import restaurantrush.game.TurnController;
import restaurantrush.game.TurnEnd;
import restaurantrush.game.TurnStart;
import restaurantrush.game.TurnSummary;
import restaurantrush.menu.Money;

public final class ConsoleUI {
    private final Scanner in;
    private final PrintStream out;

    public ConsoleUI(Scanner in, PrintStream out) {
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
    }

    public boolean run(TurnController controller) {
        Objects.requireNonNull(controller);
        while (!controller.isOver()) {
            TurnStart start = controller.beginTurn();
            printEvents(start.events());
            out.print(render(start.snapshot()));

            for (StaffRole role : List.of(StaffRole.WAITER, StaffRole.CHEF)) {
                boolean accepted = false;
                while (!accepted) {
                    printActions(controller.snapshot().legalActions());
                    out.print(prompt(role));
                    out.flush();
                    if (!in.hasNextLine()) {
                        out.print('\n');
                        printLine("Input ended before the game finished.");
                        return false;
                    }

                    String line = in.nextLine();
                    out.print('\n');
                    Optional<Action> action = CommandParser.parse(line, role);
                    if (action.isEmpty()) {
                        printLine(CommandParser.errorFor(line, role));
                        continue;
                    }

                    ActionResult result = controller.submit(action.orElseThrow());
                    if (result.events().isEmpty()) {
                        printLine(result.message());
                    }
                    printEvents(result.events());
                    accepted = result.accepted();
                }
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
        result.ifPresent(value -> appendLine(text, "Result: " + value));
        return text.toString();
    }

    private static String average(int paidCount, int satisfactionSum) {
        return paidCount == 0 ? "N/A" : String.format(Locale.ROOT, "%.1f", (double) satisfactionSum / paidCount);
    }

    private void printActions(List<Action> actions) {
        StringBuilder text = new StringBuilder("Legal actions: ");
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            Action action = actions.get(i);
            text.append(action.kind() == ActionKind.WAIT ? "WAIT" : action.kind() + " " + action.targetId());
        }
        printLine(text.toString());
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
        return role == StaffRole.WAITER ? "Waiter> " : "Chef> ";
    }

    private static void appendLine(StringBuilder text, String line) {
        text.append(line).append('\n');
    }
}