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
        checkRendering();
        checkRunEndsEarly();
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
        assertOutput(noResult);

        String victory = ConsoleUI.render(new TurnSummary(18, 5, 1, 8000, 5, 400), Optional.of(GameResult.VICTORY));
        String defeat = ConsoleUI.render(new TurnSummary(18, 2, 4, 2500, 2, 120), Optional.of(GameResult.DEFEAT));
        assert victory.contains("VICTORY");
        assert defeat.contains("DEFEAT");
        assertOutput(victory);
        assertOutput(defeat);
    }

    private static void checkRunEndsEarly() {
        // The first four commands of fixtures/stage-1/defeat.in, so input ends long before turn 18.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        boolean finished = new ConsoleUI(new Scanner("WAIT\nWAIT\nWAIT\nWAIT\n"), out).run(GameSetup.baseGame());
        String transcript = buffer.toString(StandardCharsets.UTF_8);

        assert !finished;
        String[] lines = transcript.split("\n");
        assert lines[lines.length - 1].equals("Input ended before the game finished.");
        assertOutput(transcript);
    }

    private static Snapshot emptySnapshot(int turn, int paidCount, int satisfactionSum) {
        return new Snapshot(turn, Phase.WAITER, 0, paidCount, satisfactionSum,
                List.of(), List.of(), List.of(), List.of(), Optional.of(StaffRole.WAITER), List.of());
    }

    private static void expect(String line, StaffRole role, Action expected) {
        assert CommandParser.parse(line, role).equals(Optional.of(expected)) : line;
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