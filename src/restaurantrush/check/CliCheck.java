package restaurantrush.check;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import restaurantrush.cli.CommandParser;
import restaurantrush.cli.ConsoleUI;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.CustomerView;
import restaurantrush.game.GameResult;
import restaurantrush.game.GameSetup;
import restaurantrush.game.Phase;
import restaurantrush.game.Snapshot;
import restaurantrush.game.StaffRole;
import restaurantrush.game.TableView;
import restaurantrush.game.TurnSummary;

public final class CliCheck {
    private CliCheck() {
    }

    public static void main(String[] args) {
        boolean enabled = false;
        assert enabled = true;
        if (!enabled) {
            throw new IllegalStateException("CliCheck needs assertions enabled: run with java -ea");
        }
        checkParsing();
        checkNumbers();
        checkRendering();
        checkRunEndsEarly();
        checkNumberedRun();
        System.out.print("OK CliCheck\n");
    }

    private static void checkParsing() {
        expect("seat c2", StaffRole.WAITER, new Action(StaffRole.WAITER, ActionKind.SEAT, "C2"));
        expect("  Order C1 ", StaffRole.WAITER, new Action(StaffRole.WAITER, ActionKind.ORDER, "C1"));
        expect("SERVE O1", StaffRole.WAITER, new Action(StaffRole.WAITER, ActionKind.SERVE, "O1"));
        expect("wait", StaffRole.WAITER, new Action(StaffRole.WAITER, ActionKind.WAIT, null));
        expect("PREPARE O1", StaffRole.CHEF, new Action(StaffRole.CHEF, ActionKind.PREPARE, "O1"));
        expect("WAIT", StaffRole.CHEF, new Action(StaffRole.CHEF, ActionKind.WAIT, null));

        List<InvalidInput> invalid = List.of(
                new InvalidInput("PREPARE O1", StaffRole.WAITER),
                new InvalidInput("SEAT C1", StaffRole.CHEF),
                new InvalidInput("SEAT O1", StaffRole.WAITER),
                new InvalidInput("SERVE C1", StaffRole.WAITER),
                new InvalidInput("SEAT", StaffRole.WAITER),
                new InvalidInput("seat c2 now", StaffRole.WAITER),
                new InvalidInput("", StaffRole.WAITER),
                new InvalidInput("HELLO", StaffRole.WAITER));
        for (InvalidInput input : invalid) {
            assert CommandParser.parse(input.line(), input.role()).isEmpty() : input.line();
            String error = CommandParser.errorFor(input.line(), input.role());
            assert error != null && !error.isBlank() : input.line();
            assertAscii(error);
        }
        assert CommandParser.errorFor("PREPARE O1", StaffRole.WAITER).contains("PREPARE");
        assert CommandParser.errorFor("PREPARE O1", StaffRole.WAITER).contains("Waiter");
        assert CommandParser.errorFor("SEAT C1", StaffRole.CHEF).contains("Chef");
    }

    private static void checkNumbers() {
        Action seat = new Action(StaffRole.WAITER, ActionKind.SEAT, "C2");
        Action order = new Action(StaffRole.WAITER, ActionKind.ORDER, "C1");
        Action serve = new Action(StaffRole.WAITER, ActionKind.SERVE, "O1");
        Action waiterWait = new Action(StaffRole.WAITER, ActionKind.WAIT, null);
        List<Action> waiterActions = List.of(seat, order, serve, waiterWait);
        expect("1", StaffRole.WAITER, waiterActions, seat);
        expect(" 2 ", StaffRole.WAITER, waiterActions, order);
        expect("3", StaffRole.WAITER, waiterActions, serve);
        expect("03", StaffRole.WAITER, waiterActions, serve);
        expect("0", StaffRole.WAITER, waiterActions, waiterWait);
        expect("00", StaffRole.WAITER, List.of(waiterWait), waiterWait);

        Action prepareFirst = new Action(StaffRole.CHEF, ActionKind.PREPARE, "O1");
        Action prepareSecond = new Action(StaffRole.CHEF, ActionKind.PREPARE, "O2");
        Action chefWait = new Action(StaffRole.CHEF, ActionKind.WAIT, null);
        List<Action> chefActions = List.of(prepareFirst, prepareSecond, chefWait);
        expect("1", StaffRole.CHEF, chefActions, prepareFirst);
        expect("2", StaffRole.CHEF, chefActions, prepareSecond);
        expect("0", StaffRole.CHEF, chefActions, chefWait);
        expect("0", StaffRole.CHEF, List.of(chefWait), chefWait);

        // A number outside the list is a parse error, so the prompt asks again without using the task.
        List<InvalidInput> outOfRange = List.of(
                new InvalidInput("4", StaffRole.WAITER),
                new InvalidInput("99", StaffRole.WAITER),
                new InvalidInput("99999999999", StaffRole.WAITER),
                new InvalidInput("3", StaffRole.CHEF));
        for (InvalidInput input : outOfRange) {
            List<Action> actions = input.role() == StaffRole.WAITER ? waiterActions : chefActions;
            assert CommandParser.parse(input.line(), input.role(), actions).isEmpty() : input.line();
            assert CommandParser.errorFor(input.line(), input.role(), actions)
                    .equals("That number is not in the list. Choose a number from the list, or type a command.") : input.line();
        }
        assert CommandParser.parse("1", StaffRole.WAITER, List.of(waiterWait)).isEmpty();
        assert CommandParser.parse("1", StaffRole.WAITER, null).isEmpty();
        assert CommandParser.errorFor("2", StaffRole.WAITER, waiterActions).isEmpty();
        assertAscii(CommandParser.errorFor("4", StaffRole.WAITER, waiterActions));

        // Text keeps its two-argument meaning; the list does not limit it, because submit decides legality.
        expect("seat c9", StaffRole.WAITER, waiterActions, new Action(StaffRole.WAITER, ActionKind.SEAT, "C9"));
        expect("  Order C1 ", StaffRole.WAITER, waiterActions, order);
        expect("wait", StaffRole.WAITER, waiterActions, waiterWait);
        expect("PREPARE O2", StaffRole.CHEF, chefActions, prepareSecond);
        List<InvalidInput> text = List.of(
                new InvalidInput("PREPARE O1", StaffRole.WAITER),
                new InvalidInput("SEAT C1", StaffRole.CHEF),
                new InvalidInput("SEAT", StaffRole.WAITER),
                new InvalidInput("", StaffRole.WAITER),
                new InvalidInput("HELLO", StaffRole.WAITER),
                new InvalidInput("-1", StaffRole.WAITER),
                new InvalidInput("1 2", StaffRole.WAITER),
                new InvalidInput("1a", StaffRole.CHEF));
        for (InvalidInput input : text) {
            List<Action> actions = input.role() == StaffRole.WAITER ? waiterActions : chefActions;
            assert CommandParser.parse(input.line(), input.role(), actions).isEmpty() : input.line();
            assert CommandParser.errorFor(input.line(), input.role(), actions)
                    .equals(CommandParser.errorFor(input.line(), input.role())) : input.line();
        }
        assert CommandParser.parse("1", null, waiterActions).isEmpty();
        assert CommandParser.errorFor("1", null, waiterActions).equals(CommandParser.errorFor("1", null));
    }

    private static void checkRendering() {
        CustomerView customer = new CustomerView("C1", "Regular", CustomerStatus.WAITING, 100, Optional.empty());
        Snapshot firstTurn = new Snapshot(1, Phase.WAITER, 0, 0, 0,
                List.of(customer),
                List.of(new TableView("T1", Optional.empty()), new TableView("T2", Optional.empty())),
                List.of(customer), List.of(), Optional.of(StaffRole.WAITER),
                List.of(new Action(StaffRole.WAITER, ActionKind.SEAT, "C1")));
        String firstDisplay = ConsoleUI.render(firstTurn);
        assert firstDisplay.contains("Turn 1");
        assert firstDisplay.contains("$0.00");
        assert firstDisplay.contains("N/A");
        assert firstDisplay.contains("C1 (Regular)");
        assert firstDisplay.contains("WAITING");
        assert firstDisplay.contains("100");
        assert firstDisplay.contains("NO_ORDER");
        assert firstDisplay.contains("T1: free");
        assert firstDisplay.contains("T2: free");
        assertOutput(firstDisplay);

        assert ConsoleUI.render(emptySnapshot(2, 2, 160)).contains("80.0");
        String average = ConsoleUI.render(emptySnapshot(3, 3, 229));
        assert average.contains("76.3");
        assertOutput(ConsoleUI.render(emptySnapshot(2, 2, 160)));
        assertOutput(average);

        TurnSummary summary = new TurnSummary(2, 4, 1, 4520, 0, 0);
        String noResult = ConsoleUI.render(summary, Optional.empty());
        assert noResult.contains("End of turn 2");
        assert noResult.contains("Served: 4");
        assert noResult.contains("Unhappy departures: 1");
        assert noResult.contains("$45.20");
        assert noResult.contains("N/A");
        assert noResult.endsWith("Turns used: 2\n");
        assertOutput(noResult);

        String victory = ConsoleUI.render(new TurnSummary(18, 5, 1, 8000, 5, 400), Optional.of(GameResult.VICTORY));
        String defeat = ConsoleUI.render(new TurnSummary(18, 2, 4, 2500, 2, 120), Optional.of(GameResult.DEFEAT));
        assert victory.endsWith("Turns used: 18\nResult: VICTORY\n");
        assert defeat.endsWith("Turns used: 18\nResult: DEFEAT\n");
        assertOutput(victory);
        assertOutput(defeat);
    }

    private static void checkRunEndsEarly() {
        // The first four commands of fixtures/stage-1/defeat.in, so input ends long before turn 18.
        String transcript = playUntilInputEnds("WAIT\nWAIT\nWAIT\nWAIT\n");
        String[] lines = transcript.split("\n");
        assert lines[lines.length - 1].equals("Input ended before the game finished.");
    }

    private static void checkNumberedRun() {
        // Turn 1: 2 and 99 are not in the list, so the Waiter is asked again; 1 seats C1 and the Chef waits with 0.
        // Turn 2: 1 takes C1's order and 1 prepares it.
        String numbers = playUntilInputEnds("2\n99\n1\n0\n1\n1\n");
        String retry = "That number is not in the list. Choose a number from the list, or type a command.\n";
        String turnOneWaiter = "Legal actions: [1] SEAT C1, [0] WAIT\nWaiter> \n";
        assert numbers.contains(turnOneWaiter + retry + turnOneWaiter + retry + turnOneWaiter
                + "C1 seated at T1\nLegal actions: [0] WAIT\nChef> \nChef waits.\n") : numbers;
        assert numbers.contains("Legal actions: [1] ORDER C1, [0] WAIT\nWaiter> \nC1 orders Salad ($9.00) as O1\n"
                + "Legal actions: [1] PREPARE O1, [0] WAIT\nChef> \nO1 is READY\n") : numbers;

        String waits = playUntilInputEnds("0\n");
        assert waits.contains(turnOneWaiter + "Waiter waits.\nLegal actions: [0] WAIT\nChef> \n") : waits;
    }

    // Plays the base game until the given input runs out, which must happen before turn 18.
    private static String playUntilInputEnds(String input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        boolean finished = new ConsoleUI(new Scanner(input), out).run(GameSetup.baseGame());
        String transcript = buffer.toString(StandardCharsets.UTF_8);

        assert !finished;
        assertOutput(transcript);
        return transcript;
    }

    private static Snapshot emptySnapshot(int turn, int paidCount, int satisfactionSum) {
        return new Snapshot(turn, Phase.WAITER, 0, paidCount, satisfactionSum,
                List.of(), List.of(), List.of(), List.of(), Optional.of(StaffRole.WAITER), List.of());
    }

    private static void expect(String line, StaffRole role, Action expected) {
        assert CommandParser.parse(line, role).equals(Optional.of(expected)) : line;
    }

    private static void expect(String line, StaffRole role, List<Action> legalActions, Action expected) {
        assert CommandParser.parse(line, role, legalActions).equals(Optional.of(expected)) : line;
        assert CommandParser.errorFor(line, role, legalActions).isEmpty() : line;
    }

    private static void assertOutput(String text) {
        assert !text.contains("\r");
        assert text.endsWith("\n");
        assertAscii(text);
    }

    private static void assertAscii(String text) {
        for (int i = 0; i < text.length(); i++) {
            assert text.charAt(i) <= 0x7f : "non-ASCII output";
        }
    }

    private record InvalidInput(String line, StaffRole role) {
    }
}