package restaurantrush.check;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import restaurantrush.cli.CommandParser;
import restaurantrush.cli.ConsoleUI;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.CustomerView;
import restaurantrush.game.GameConfig;
import restaurantrush.game.GameResult;
import restaurantrush.game.GameSetup;
import restaurantrush.game.GameState;
import restaurantrush.game.Phase;
import restaurantrush.game.Snapshot;
import restaurantrush.game.StaffRole;
import restaurantrush.game.TableView;
import restaurantrush.game.TurnController;
import restaurantrush.game.TurnSummary;
import restaurantrush.menu.Menu;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;

public final class CliCheck {
    private static final String OUT_OF_RANGE = "That number is not in the list. Choose a number from the list, or type a command.";
    private static final String MANAGER_PROMPT = "Happy Hour available: HAPPY_HOUR or SKIP\nManager> \n";
    private static final String MANAGER_ERROR = "Invalid manager command. Use HAPPY_HOUR or SKIP.\n";
    private static final String INPUT_ENDED = "Input ended before the game finished.\n";

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
        checkHostParsing();
        checkHappyHourParsing();
        checkRendering();
        checkHappyHourRendering();
        checkRunEndsEarly();
        checkNumberedRun();
        checkStage3Run();
        checkStage3InputEnds();
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
        assert CommandParser.errorFor("", StaffRole.CHEF).equals("Nothing was entered. Enter a command for Chef.");
        assert CommandParser.errorFor("   ", StaffRole.WAITER).equals("Nothing was entered. Enter a command for Waiter.");
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

    // The Host may only SEAT or WAIT; its errors name the Host and its two commands.
    private static void checkHostParsing() {
        expect("seat c3", StaffRole.HOST, new Action(StaffRole.HOST, ActionKind.SEAT, "C3"));
        expect(" WAIT ", StaffRole.HOST, new Action(StaffRole.HOST, ActionKind.WAIT, null));
        for (String verb : List.of("ORDER C1", "SERVE O1", "PREPARE O1")) {
            assert CommandParser.parse(verb, StaffRole.HOST).isEmpty() : verb;
            String name = verb.substring(0, verb.indexOf(' '));
            assert CommandParser.errorFor(verb, StaffRole.HOST).equals(name + " is not available to Host.") : verb;
        }
        assert CommandParser.errorFor("", StaffRole.HOST).equals("Nothing was entered. Enter a command for Host.");
        assert CommandParser.errorFor("HELLO", StaffRole.HOST).equals("Unknown command. Use SEAT C<n> or WAIT.");
        assert CommandParser.errorFor("SEAT O1", StaffRole.HOST).equals("Invalid target for SEAT. Use SEAT C<n>.");
        assert CommandParser.errorFor("SEAT", StaffRole.HOST).equals("Invalid SEAT command. Use SEAT C<n>.");
        // Adding the Host leaves the Waiter's and Chef's command lists as they were.
        assert CommandParser.errorFor("HELLO", StaffRole.WAITER).equals("Unknown command. Use SEAT C<n>, ORDER C<n>, SERVE O<n>, or WAIT.");
        assert CommandParser.errorFor("HELLO", StaffRole.CHEF).equals("Unknown command. Use PREPARE O<n> or WAIT.");

        Action seatFirst = new Action(StaffRole.HOST, ActionKind.SEAT, "C1");
        Action seatSecond = new Action(StaffRole.HOST, ActionKind.SEAT, "C2");
        Action hostWait = new Action(StaffRole.HOST, ActionKind.WAIT, null);
        List<Action> hostActions = List.of(seatFirst, seatSecond, hostWait);
        expect("1", StaffRole.HOST, hostActions, seatFirst);
        expect("2", StaffRole.HOST, hostActions, seatSecond);
        expect("0", StaffRole.HOST, hostActions, hostWait);
        expect("0", StaffRole.HOST, List.of(hostWait), hostWait);
        for (String number : List.of("3", "1")) {
            List<Action> actions = number.equals("1") ? List.of(hostWait) : hostActions;
            assert CommandParser.parse(number, StaffRole.HOST, actions).isEmpty() : number;
            assert CommandParser.errorFor(number, StaffRole.HOST, actions).equals(OUT_OF_RANGE) : number;
        }
        assert CommandParser.errorFor("ORDER C1", StaffRole.HOST, hostActions).equals("ORDER is not available to Host.");
    }

    private static void checkHappyHourParsing() {
        for (String yes : List.of("HAPPY_HOUR", "happy_hour", "  Happy_Hour\t")) {
            assert CommandParser.parseHappyHour(yes).equals(Optional.of(true)) : yes;
        }
        for (String no : List.of("SKIP", "skip", " Skip ")) {
            assert CommandParser.parseHappyHour(no).equals(Optional.of(false)) : no;
        }
        List<String> invalid = Arrays.asList(null, "", "   ", "HAPPY HOUR", "HAPPYHOUR", "HAPPY_HOUR now",
                "yes", "WAIT", "0", "1");
        for (String line : invalid) {
            assert CommandParser.parseHappyHour(line).isEmpty() : line;
        }
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

    // A HOST-phase start snapshot shows the remaining Happy Hour duration, including 0; other snapshots render as before.
    private static void checkHappyHourRendering() {
        CustomerView customer = new CustomerView("C1", "Regular", CustomerStatus.WAITING, 100, Optional.empty());
        List<TableView> tables = List.of(new TableView("T1", Optional.empty()), new TableView("T2", Optional.empty()));
        List<Action> hostActions = List.of(new Action(StaffRole.HOST, ActionKind.SEAT, "C1"),
                new Action(StaffRole.HOST, ActionKind.WAIT, null));
        for (int remaining : new int[] {2, 1, 0}) {
            Snapshot host = new Snapshot(7, Phase.HOST, 900, 1, 92, List.of(customer), tables, List.of(customer),
                    List.of(), Optional.of(StaffRole.HOST), hostActions, false, remaining);
            String display = ConsoleUI.render(host);
            assert display.startsWith("Turn 7\nRevenue: $9.00\nAverage satisfaction: 92.0\nHappy Hour remaining: "
                    + remaining + "\nQueue:\n") : display;
            assertOutput(display);
        }

        List<Action> waiterActions = List.of(new Action(StaffRole.WAITER, ActionKind.SEAT, "C1"));
        Snapshot legacy = new Snapshot(1, Phase.WAITER, 0, 0, 0, List.of(customer), tables, List.of(customer), List.of(),
                Optional.of(StaffRole.WAITER), waiterActions);
        assert !legacy.happyHourAvailable() && legacy.happyHourRemaining() == 0;
        assert legacy.equals(new Snapshot(1, Phase.WAITER, 0, 0, 0, List.of(customer), tables, List.of(customer),
                List.of(), Optional.of(StaffRole.WAITER), waiterActions, false, 0));
        String legacyDisplay = ConsoleUI.render(legacy);
        assert legacyDisplay.startsWith("Turn 1\nRevenue: $0.00\nAverage satisfaction: N/A\nQueue:\n") : legacyDisplay;
        assert !legacyDisplay.contains("Happy Hour");
    }

    private static void checkRunEndsEarly() {
        // The first four commands of fixtures/stage-1/defeat.in, so input ends long before turn 18.
        String transcript = playLegacyUntilInputEnds("WAIT\nWAIT\nWAIT\nWAIT\n");
        String[] lines = transcript.split("\n");
        assert lines[lines.length - 1].equals("Input ended before the game finished.");
        assertTwoRoles(transcript);
    }

    private static void checkNumberedRun() {
        // Turn 1: 2 and 99 are not in the list, so the Waiter is asked again; 1 seats C1 and the Chef waits with 0.
        // Turn 2: 1 takes C1's order and 1 prepares it.
        String numbers = playLegacyUntilInputEnds("2\n99\n1\n0\n1\n1\n");
        String retry = OUT_OF_RANGE + "\n";
        String turnOneWaiter = "Legal actions: [1] SEAT C1, [0] WAIT\nWaiter> \n";
        assert numbers.contains(turnOneWaiter + retry + turnOneWaiter + retry + turnOneWaiter
                + "C1 seated at T1\nLegal actions: [0] WAIT\nChef> \nChef waits.\n") : numbers;
        assert numbers.contains("Legal actions: [1] ORDER C1, [0] WAIT\nWaiter> \nC1 orders Salad ($9.00) as O1\n"
                + "Legal actions: [1] PREPARE O1, [0] WAIT\nChef> \nO1 is READY\n") : numbers;

        String waits = playLegacyUntilInputEnds("0\n");
        assert waits.contains(turnOneWaiter + "Waiter waits.\nLegal actions: [0] WAIT\nChef> \n") : waits;
        assertTwoRoles(numbers);
        assertTwoRoles(waits);
    }

    // Two Stage 3 turns on the base game, with a manager retry, Host retries, numbers at the Host prompt, and activation.
    private static void checkStage3Run() {
        // Turn 1: a Manager retry, then SKIP. The Host gets a parse error, an empty line, a rejected SEAT, and a number
        // outside the list before 1 seats C1; the Waiter takes C1's order with 1 and the Chef waits with 0.
        // Turn 2: Happy Hour starts and the Host waits with 0. Turn 3 has no Manager prompt, and input ends at the Host.
        String input = "hello\nskip\n" + "ORDER C1\n\nSEAT C9\n5\n1\n" + "1\n0\n"
                + "happy_hour\n" + "0\nWAIT\nPREPARE O1\n";
        String transcript = playUntilInputEnds(GameSetup.baseGame(), input);

        String hostTurnOne = "Legal actions: [1] SEAT C1, [0] WAIT\nHost> \n";
        assert transcript.startsWith(MANAGER_PROMPT + MANAGER_ERROR + MANAGER_PROMPT
                + "C1 (Regular) arrives\nTurn 1\nRevenue: $0.00\nAverage satisfaction: N/A\nHappy Hour remaining: 0\nQueue:\n")
                : transcript;
        assert transcript.contains(hostTurnOne + "ORDER is not available to Host.\n"
                + hostTurnOne + "Nothing was entered. Enter a command for Host.\n"
                + hostTurnOne + "C9 is not in the waiting queue.\n"
                + hostTurnOne + OUT_OF_RANGE + "\n"
                + hostTurnOne + "C1 seated at T1\n"
                + "Legal actions: [1] ORDER C1, [0] WAIT\nWaiter> \nC1 orders Salad ($9.00) as O1\n"
                + "Legal actions: [1] PREPARE O1, [0] WAIT\nChef> \nChef waits.\n") : transcript;
        assert transcript.contains("Turns used: 1\n" + MANAGER_PROMPT
                + "Happy Hour activated\nC1 recovers 8 satisfaction (100)\nTurn 2\nRevenue: $0.00\n"
                + "Average satisfaction: N/A\nHappy Hour remaining: 2\n") : transcript;
        assert transcript.contains("Legal actions: [0] WAIT\nHost> \nHost waits.\n"
                + "Legal actions: [0] WAIT\nWaiter> \nWaiter waits.\n") : transcript;
        assert transcript.contains("O1 is READY\n") : transcript;
        assert transcript.contains("Turns used: 2\nC1 recovers 8 satisfaction (100)\nTurn 3\n"
                + "Revenue: $0.00\nAverage satisfaction: N/A\nHappy Hour remaining: 1\n") : transcript;
        assert transcript.endsWith("Host> \n" + INPUT_ENDED) : transcript;
        assert transcript.split("Manager> ", -1).length - 1 == 3;
    }

    // Input that ends at the Manager prompt, after a manager retry, or at the Host prompt stops the run with the usual message.
    private static void checkStage3InputEnds() {
        assert playUntilInputEnds(GameSetup.baseGame(), "").equals(MANAGER_PROMPT + INPUT_ENDED);
        assert playUntilInputEnds(GameSetup.baseGame(), "maybe\n")
                .equals(MANAGER_PROMPT + MANAGER_ERROR + MANAGER_PROMPT + INPUT_ENDED);
        String atHost = playUntilInputEnds(GameSetup.baseGame(), "SKIP\n");
        assert atHost.startsWith(MANAGER_PROMPT + "C1 (Regular) arrives\nTurn 1\n") : atHost;
        assert atHost.endsWith("Legal actions: [1] SEAT C1, [0] WAIT\nHost> \n" + INPUT_ENDED) : atHost;
    }

    // The Stage 1 and 2 flow, built without a Host as earlier stages' GameCheck traces are.
    private static TurnController legacyGame() {
        GameConfig config = GameConfig.base();
        return new TurnController(new GameState(config, Menu.base()), new Waiter(), new Chef(), new Cashier(),
                i -> GameSetup.arrival(i, config.arrivalTurns().get(i - 1)));
    }

    private static String playLegacyUntilInputEnds(String input) {
        return playUntilInputEnds(legacyGame(), input);
    }

    // Plays the game until the given input runs out, which must happen before turn 18.
    private static String playUntilInputEnds(TurnController controller, String input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        boolean finished = new ConsoleUI(new Scanner(input), out).run(controller);
        String transcript = buffer.toString(StandardCharsets.UTF_8);

        assert !finished;
        assertOutput(transcript);
        return transcript;
    }

    // A game without a Host prints only the Waiter and Chef prompts and no Happy Hour lines.
    private static void assertTwoRoles(String transcript) {
        assert !transcript.contains("Manager> ") && !transcript.contains("Host> ") : transcript;
        assert !transcript.contains("Happy Hour") : transcript;
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