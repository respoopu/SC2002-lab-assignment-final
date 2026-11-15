package restaurantrush.check;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
import restaurantrush.game.OrderView;
import restaurantrush.game.Phase;
import restaurantrush.game.Snapshot;
import restaurantrush.game.StaffRole;
import restaurantrush.game.TableView;
import restaurantrush.game.TurnController;
import restaurantrush.game.TurnSummary;
import restaurantrush.menu.Menu;
import restaurantrush.menu.OrderStatus;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;

public final class CliCheck {
    private static final String OUT_OF_RANGE = "That number is not in the list. Choose a number from the list, or type a command.";
    private static final String HEAVY_RULE = "=".repeat(64);
    private static final String LIGHT_RULE = "-".repeat(64);
    private static final String TITLE = HEAVY_RULE + "\n                 R E S T A U R A N T   R U S H\n" + HEAVY_RULE + "\n"
            + " You are the manager. The game lasts 18 turns, and each turn you\n choose what your staff do.\n\n"
            + " At each prompt, type the number next to an action, or type\n the command itself, such as SEAT C1. 0 means wait.\n";
    private static final String RESULT = "   >> ";
    private static final String ERROR = "   !! ";
    private static final String MANAGER_PROMPT = "Manager> \n";
    private static final String MANAGER_ERROR = ERROR + "Invalid manager command. Use HAPPY_HOUR or SKIP.\n";
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
        assert firstDisplay.startsWith("\n" + HEAVY_RULE + "\n TURN 1 of 18\n" + HEAVY_RULE + "\n"
                + " Revenue $0.00  |  Paid customers 0  |  Average satisfaction N/A\n") : firstDisplay;
        assert firstDisplay.contains("\n WAITING QUEUE\n   C1  Regular  [##########] 100\n") : firstDisplay;
        assert firstDisplay.contains("\n TABLES\n   Table T1  free\n   Table T2  free\n") : firstDisplay;
        assert firstDisplay.contains("   C1  Regular  [##########] 100 -      Waiting for a table\n") : firstDisplay;
        assert firstDisplay.endsWith("\n ORDERS\n   (no orders yet)\n") : firstDisplay;
        assert !firstDisplay.contains("NEWS");
        assertOutput(firstDisplay);

        // A seated customer shows at the table, and each order its progress and bill.
        CustomerView seated = new CustomerView("C2", "VIP", CustomerStatus.SEATED, 95, Optional.of("O2"));
        Snapshot later = new Snapshot(4, Phase.WAITER, 900, 1, 92, List.of(),
                List.of(new TableView("T1", Optional.empty()), new TableView("T2", Optional.of("C2"))),
                List.of(seated), List.of(new OrderView("O2", "C2", "Burger", OrderStatus.READY, 1, 1, 1350)),
                Optional.of(StaffRole.WAITER), List.of());
        String laterDisplay = ConsoleUI.render(later);
        assert laterDisplay.contains(" Revenue $9.00  |  Paid customers 1  |  Average satisfaction 92.0\n\n"
                + " WAITING QUEUE\n   (nobody is waiting)\n") : laterDisplay;
        assert laterDisplay.contains("   Table T2  Customer C2 - Waiting for food\n") : laterDisplay;
        assert laterDisplay.contains("   C2  VIP      [#########-]  95 O2     Waiting for food\n") : laterDisplay;
        assert laterDisplay.contains("   O2  Burger                C2   1/1     $13.50  Ready\n") : laterDisplay;
        assertOutput(laterDisplay);

        assert ConsoleUI.render(emptySnapshot(2, 2, 160)).contains("Average satisfaction 80.0");
        String average = ConsoleUI.render(emptySnapshot(3, 3, 229));
        assert average.contains("Average satisfaction 76.3");
        assertOutput(ConsoleUI.render(emptySnapshot(2, 2, 160)));
        assertOutput(average);

        TurnSummary summary = new TurnSummary(2, 4, 1, 4520, 0, 0);
        String noResult = ConsoleUI.render(summary, Optional.empty());
        assert noResult.equals(" Served: 4   Unhappy departures: 1   Revenue: $45.20\n"
                + " Average satisfaction: N/A   Turns used: 2 of 18\n") : noResult;
        assertOutput(noResult);

        String victory = ConsoleUI.render(new TurnSummary(18, 5, 1, 8000, 5, 400), Optional.of(GameResult.VICTORY));
        String defeat = ConsoleUI.render(new TurnSummary(18, 2, 4, 2500, 2, 120), Optional.of(GameResult.DEFEAT));
        assert victory.contains("Turns used: 18 of 18\n\n" + HEAVY_RULE + "\n") : victory;
        assert victory.contains("*** VICTORY ***\n") : victory;
        assert victory.endsWith(" Paid customers: 5   Revenue: $80.00   Average satisfaction: 80.0\n Result: VICTORY\n")
                : victory;
        assert defeat.contains("*** DEFEAT ***\n") : defeat;
        assert defeat.endsWith("\n Result: DEFEAT\n") : defeat;
        assertOutput(victory);
        assertOutput(defeat);
    }

    // A HOST-phase start snapshot shows the remaining Happy Hour duration, including 0; other snapshots show no Happy Hour status.
    private static void checkHappyHourRendering() {
        CustomerView customer = new CustomerView("C1", "Regular", CustomerStatus.WAITING, 100, Optional.empty());
        List<TableView> tables = List.of(new TableView("T1", Optional.empty()), new TableView("T2", Optional.empty()));
        List<Action> hostActions = List.of(new Action(StaffRole.HOST, ActionKind.SEAT, "C1"),
                new Action(StaffRole.HOST, ActionKind.WAIT, null));
        String[] banners = {
            " TURN 7 of 18                                    Happy Hour: off",
            " TURN 7 of 18                      HAPPY HOUR ON (turns left: 1)",
            " TURN 7 of 18                      HAPPY HOUR ON (turns left: 2)",
        };
        for (int remaining : new int[] {2, 1, 0}) {
            Snapshot host = new Snapshot(7, Phase.HOST, 900, 1, 92, List.of(customer), tables, List.of(customer),
                    List.of(), Optional.of(StaffRole.HOST), hostActions, false, remaining);
            String display = ConsoleUI.render(host);
            assert display.startsWith("\n" + HEAVY_RULE + "\n" + banners[remaining] + "\n" + HEAVY_RULE + "\n"
                    + " Revenue $9.00  |  Paid customers 1  |  Average satisfaction 92.0\n") : display;
            assert banners[remaining].length() == HEAVY_RULE.length();
            assertOutput(display);
        }

        List<Action> waiterActions = List.of(new Action(StaffRole.WAITER, ActionKind.SEAT, "C1"));
        Snapshot legacy = new Snapshot(1, Phase.WAITER, 0, 0, 0, List.of(customer), tables, List.of(customer), List.of(),
                Optional.of(StaffRole.WAITER), waiterActions);
        assert !legacy.happyHourAvailable() && legacy.happyHourRemaining() == 0;
        assert legacy.equals(new Snapshot(1, Phase.WAITER, 0, 0, 0, List.of(customer), tables, List.of(customer),
                List.of(), Optional.of(StaffRole.WAITER), waiterActions, false, 0));
        String legacyDisplay = ConsoleUI.render(legacy);
        assert legacyDisplay.startsWith("\n" + HEAVY_RULE + "\n TURN 1 of 18\n" + HEAVY_RULE + "\n") : legacyDisplay;
        assert !legacyDisplay.contains("Happy Hour") && !legacyDisplay.contains("HAPPY HOUR");
    }

    private static void checkRunEndsEarly() {
        // The first four commands of fixtures/stage-1/defeat.in, so input ends long before turn 18.
        String transcript = playLegacyUntilInputEnds("WAIT\nWAIT\nWAIT\nWAIT\n");
        String[] lines = transcript.split("\n");
        assert lines[lines.length - 1].equals("Input ended before the game finished.");
        assert transcript.startsWith(TITLE) : transcript;
        assertTwoRoles(transcript);
    }

    private static void checkNumberedRun() {
        // Turn 1: 2 and 99 are not in the list, so the Waiter is asked again; 1 seats C1 and the Chef waits with 0.
        // Turn 2: 1 takes C1's order and 1 prepares it.
        String numbers = playLegacyUntilInputEnds("2\n99\n1\n0\n1\n1\n");
        String retry = ERROR + OUT_OF_RANGE + "\n";
        String seatList = "   [1] SEAT C1      Seat customer C1 (Regular)\n   [0] WAIT         Do nothing\nWaiter> \n";
        assert numbers.contains(heading("Waiter") + seatList + retry + seatList + retry + seatList
                + RESULT + "Customer C1 is seated at table T1\n" + heading("Chef")
                + "   [0] WAIT         Do nothing\nChef> \n" + RESULT + "Chef waits.\n") : numbers;
        assert numbers.contains(heading("Waiter") + "   [1] ORDER C1     Take customer C1's order\n"
                + "   [0] WAIT         Do nothing\nWaiter> \n" + RESULT + "Customer C1 orders Salad for $9.00 (order O1)\n"
                + heading("Chef") + "   [1] PREPARE O1   Prepare order O1 (Salad, 0/1)\n"
                + "   [0] WAIT         Do nothing\nChef> \n" + RESULT + "Order O1 is READY to serve\n") : numbers;

        String waits = playLegacyUntilInputEnds("0\n");
        assert waits.contains(seatList + RESULT + "Waiter waits.\n" + heading("Chef")
                + "   [0] WAIT         Do nothing\nChef> \n") : waits;
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

        String hostList = "   [1] SEAT C1      Seat customer C1 (Regular)\n   [0] WAIT         Do nothing\nHost> \n";
        assert transcript.startsWith(TITLE + managerHeading(1) + MANAGER_PROMPT + MANAGER_ERROR + MANAGER_PROMPT
                + "\n" + HEAVY_RULE + "\n TURN 1 of 18                                    Happy Hour: off\n") : transcript;
        assert transcript.contains(heading("Host") + hostList + ERROR + "ORDER is not available to Host.\n"
                + hostList + ERROR + "Nothing was entered. Enter a command for Host.\n"
                + hostList + ERROR + "C9 is not in the waiting queue.\n"
                + hostList + ERROR + OUT_OF_RANGE + "\n"
                + hostList + RESULT + "Customer C1 is seated at table T1\n"
                + heading("Waiter") + "   [1] ORDER C1     Take customer C1's order\n   [0] WAIT         Do nothing\n"
                + "Waiter> \n" + RESULT + "Customer C1 orders Salad for $9.00 (order O1)\n"
                + heading("Chef") + "   [1] PREPARE O1   Prepare order O1 (Salad, 0/1)\n   [0] WAIT         Do nothing\n"
                + "Chef> \n" + RESULT + "Chef waits.\n") : transcript;
        assert transcript.contains("Turns used: 1 of 18\n" + managerHeading(2) + MANAGER_PROMPT + "\n" + HEAVY_RULE
                + "\n TURN 2 of 18                      HAPPY HOUR ON (turns left: 2)\n") : transcript;
        assert transcript.contains("\n NEWS\n   * Happy Hour activated\n   * Customer C1 recovers 8 satisfaction (now 100)\n")
                : transcript;
        assert transcript.contains(heading("Host") + "   [0] WAIT         Do nothing\nHost> \n" + RESULT + "Host waits.\n"
                + heading("Waiter") + "   [0] WAIT         Do nothing\nWaiter> \n" + RESULT + "Waiter waits.\n") : transcript;
        assert transcript.contains(RESULT + "Order O1 is READY to serve\n") : transcript;
        assert transcript.contains("Turns used: 2 of 18\n\n" + HEAVY_RULE
                + "\n TURN 3 of 18                      HAPPY HOUR ON (turns left: 1)\n") : transcript;
        assert transcript.contains("\n NEWS\n   * Customer C1 recovers 8 satisfaction (now 100)\n\n WAITING QUEUE\n")
                : transcript;
        assert transcript.endsWith("Host> \n" + INPUT_ENDED) : transcript;
        assert transcript.split("Manager> ", -1).length - 1 == 3;
    }

    // Input that ends at the Manager prompt, after a manager retry, or at the Host prompt stops the run with the usual message.
    private static void checkStage3InputEnds() {
        assert playUntilInputEnds(GameSetup.baseGame(), "").equals(TITLE + managerHeading(1) + MANAGER_PROMPT + INPUT_ENDED);
        assert playUntilInputEnds(GameSetup.baseGame(), "maybe\n")
                .equals(TITLE + managerHeading(1) + MANAGER_PROMPT + MANAGER_ERROR + MANAGER_PROMPT + INPUT_ENDED);
        String atHost = playUntilInputEnds(GameSetup.baseGame(), "SKIP\n");
        assert atHost.startsWith(TITLE + managerHeading(1) + MANAGER_PROMPT + "\n" + HEAVY_RULE + "\n TURN 1 of 18 ")
                : atHost;
        assert atHost.contains("\n NEWS\n   * C1 (Regular) arrives\n") : atHost;
        assert atHost.endsWith(heading("Host") + "   [1] SEAT C1      Seat customer C1 (Regular)\n"
                + "   [0] WAIT         Do nothing\nHost> \n" + INPUT_ENDED) : atHost;
    }

    private static String heading(String role) {
        return "\n" + LIGHT_RULE + "\n " + role.toUpperCase(Locale.ROOT) + "'S TURN\n What should the " + role
                + " do?\n";
    }

    private static String managerHeading(int turn) {
        return "\n" + LIGHT_RULE + "\n START OF TURN " + turn + " - MANAGER'S DECISION\n Happy Hour is available.\n"
                + " Type HAPPY_HOUR to start it now, or SKIP.\n";
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