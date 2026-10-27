package restaurantrush.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import restaurantrush.customer.CriticCustomer;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.customer.VIPCustomer;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.ActionResult;
import restaurantrush.game.CustomerView;
import restaurantrush.game.GameConfig;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameResult;
import restaurantrush.game.GameSetup;
import restaurantrush.game.GameState;
import restaurantrush.game.OrderView;
import restaurantrush.game.Phase;
import restaurantrush.game.Snapshot;
import restaurantrush.game.StaffRole;
import restaurantrush.game.TableView;
import restaurantrush.game.TurnController;
import restaurantrush.game.TurnEnd;
import restaurantrush.game.TurnStart;
import restaurantrush.game.TurnSummary;
import restaurantrush.menu.ComboMeal;
import restaurantrush.menu.Menu;
import restaurantrush.menu.MenuItem;
import restaurantrush.menu.OrderStatus;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Host;
import restaurantrush.staff.Waiter;

/** Self-check for the game package: TurnController and GameSetup, played through the public API. Run with java -ea. */
public final class GameCheck {
    private static final GameConfig BASE = GameConfig.base();

    /** Stage 1 arrivals: every customer is Regular, so these traces survive Stage 2's GameSetup change. */
    private static final IntFunction<Customer> ALL_REGULAR =
            i -> new RegularCustomer("C" + i, BASE.arrivalTurns().get(i - 1), i);

    /** Stage 2 arrivals from GameSetup.arrival, built without baseGame() so Stage 3 setup changes leave them alone. */
    private static final IntFunction<Customer> STAGE_2 = i -> GameSetup.arrival(i, BASE.arrivalTurns().get(i - 1));

    private GameCheck() {
    }

    public static void main(String[] args) {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (!assertionsEnabled) {
            throw new IllegalStateException("GameCheck needs assertions enabled: run with java -ea");
        }

        checkSetup();
        checkBeginTurn();
        checkArrivalTurns();
        checkWrongPhase();
        checkPhaseAdvance();
        checkPastaExample();
        checkWaitingAbandonment();
        checkSeatedAbandonment();
        checkVictory();
        checkDefeat();
        checkStage2Victory();
        checkStage2Defeat();
        checkStage3Setup();
        checkHostPhase();
        checkHostAndWaiterSeating();
        checkHappyHourActivationRules();
        checkHappyHourPricingAndRecovery();
        checkHappyHourOnLastTurn();
        checkStage3Victory();
        checkStage3Defeat();

        System.out.println("OK GameCheck");
    }

    private static TurnController controller(IntFunction<Customer> arrivals) {
        return controller(new GameState(BASE, Menu.base()), arrivals);
    }

    private static TurnController controller(GameState state, IntFunction<Customer> arrivals) {
        return new TurnController(state, new Waiter(), new Chef(), new Cashier(), arrivals);
    }

    /** The Stage 3 game as GameSetup.baseGame() builds it, keeping the state so checks can read Happy Hour fields. */
    private static TurnController stage3(GameState state) {
        return new TurnController(state, new Host(), new Waiter(), new Chef(), new Cashier(), STAGE_2);
    }

    private static GameState stage3State() {
        return new GameState(BASE, GameSetup.stage3Menu());
    }

    /** "SEAT C1", "PREPARE O1", or "WAIT" as an Action for the role. */
    private static Action action(StaffRole role, String command) {
        String[] parts = command.split(" ");
        return new Action(role, ActionKind.valueOf(parts[0]), parts.length > 1 ? parts[1] : null);
    }

    private static void accept(TurnController controller, StaffRole role, String command) {
        ActionResult result = controller.submit(action(role, command));
        assert result.accepted() : command + " rejected on turn " + controller.snapshot().turn() + ": " + result.message();
    }

    /** Plays one whole turn, appending "turn: text" for every event to log. */
    private static TurnEnd playTurn(TurnController controller, String waiter, String chef, List<String> log) {
        record(controller.beginTurn().events(), log);
        ActionResult w = controller.submit(action(StaffRole.WAITER, waiter));
        assert w.accepted() : waiter + ": " + w.message();
        record(w.events(), log);
        ActionResult c = controller.submit(action(StaffRole.CHEF, chef));
        assert c.accepted() : chef + ": " + c.message();
        record(c.events(), log);
        TurnEnd end = controller.endTurn();
        record(end.events(), log);
        return end;
    }

    /** Plays all 18 turns; waiter has one command per turn, chef maps turn to command (WAIT otherwise). */
    private static List<TurnEnd> playGame(TurnController controller, List<String> waiter, Map<Integer, String> chef,
                                          List<String> log) {
        return playGame(controller, waiter, chef, log, new ArrayList<>());
    }

    /** As above, also appending the snapshot taken after each turn ends to snapshots. */
    private static List<TurnEnd> playGame(TurnController controller, List<String> waiter, Map<Integer, String> chef,
                                          List<String> log, List<Snapshot> snapshots) {
        List<TurnEnd> ends = new ArrayList<>();
        for (int turn = 1; turn <= BASE.turns(); turn++) {
            ends.add(playTurn(controller, waiter.get(turn - 1), chef.getOrDefault(turn, "WAIT"), log));
            snapshots.add(controller.snapshot());
        }
        return ends;
    }

    /** One Stage 3 turn: the Manager's choice when Happy Hour is still available, then Host, Waiter and Chef. Logs every event. */
    private static TurnEnd playStage3Turn(TurnController controller, boolean happyHour, String host, String waiter,
                                          String chef, List<String> log) {
        return playStage3Turn(controller, happyHour, host, waiter, chef, log, new ArrayList<>());
    }

    /** As above, also appending the start-of-turn snapshot to starts. */
    private static TurnEnd playStage3Turn(TurnController controller, boolean happyHour, String host, String waiter,
                                          String chef, List<String> log, List<Snapshot> starts) {
        assert controller.snapshot().happyHourAvailable() || !happyHour;
        TurnStart start = controller.beginTurn(happyHour);
        assert start.snapshot().phase() == Phase.HOST;
        starts.add(start.snapshot());
        record(start.events(), log);
        for (String[] choice : new String[][] {{"HOST", host}, {"WAITER", waiter}, {"CHEF", chef}}) {
            ActionResult result = controller.submit(action(StaffRole.valueOf(choice[0]), choice[1]));
            assert result.accepted() : choice[0] + " " + choice[1] + " on turn " + controller.snapshot().turn() + ": "
                    + result.message();
            record(result.events(), log);
        }
        TurnEnd end = controller.endTurn();
        record(end.events(), log);
        return end;
    }

    private static List<String> allWait() {
        List<String> waits = new ArrayList<>();
        for (int turn = 1; turn <= BASE.turns(); turn++) {
            waits.add("WAIT");
        }
        return waits;
    }

    private static void record(List<GameEvent> events, List<String> log) {
        events.forEach(e -> log.add(e.turn() + ": " + e.text()));
    }

    private static CustomerView customer(Snapshot snapshot, String id) {
        return snapshot.customers().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    private static Optional<String> occupant(Snapshot snapshot, String tableId) {
        return snapshot.tables().stream().filter(t -> t.id().equals(tableId)).findFirst().orElseThrow().occupantId();
    }

    private static OrderView order(Snapshot snapshot, String id) {
        return snapshot.orders().stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
    }

    /** The customer's satisfaction at the end of each turn from first to last, inclusive. */
    private static List<Integer> satisfactionByTurn(List<Snapshot> snapshots, String id, int first, int last) {
        return snapshots.subList(first - 1, last).stream().map(s -> customer(s, id).satisfaction()).toList();
    }

    private static List<String> ids(List<CustomerView> views) {
        return views.stream().map(CustomerView::id).toList();
    }

    private static void checkSetup() {
        Customer c2 = GameSetup.arrival(2, 4);
        assert c2.getClass() == VIPCustomer.class;
        assert c2.id().equals("C2") && c2.arrivalTurn() == 4 && c2.arrivalIndex() == 2;
        assert c2.typeName().equals("VIP");

        Customer c4 = GameSetup.arrival(4, 10);
        assert c4.getClass() == CriticCustomer.class;
        assert c4.id().equals("C4") && c4.arrivalTurn() == 10 && c4.arrivalIndex() == 4;
        assert c4.typeName().equals("Critic");

        int[][] regulars = {{1, 1}, {3, 7}, {5, 13}, {6, 16}};
        for (int[] arrival : regulars) {
            Customer regular = GameSetup.arrival(arrival[0], arrival[1]);
            assert regular.getClass() == RegularCustomer.class : "arrival " + arrival[0];
            assert regular.id().equals("C" + arrival[0]);
            assert regular.arrivalTurn() == arrival[1] && regular.arrivalIndex() == arrival[0];
            assert regular.typeName().equals("Regular");
        }

        TurnController game = GameSetup.baseGame();
        assert game.snapshot().turn() == 0;
        assert game.phase() == Phase.TURN_START;
        assert !game.isOver();
        assert game.currentRole().isEmpty();
    }

    private static void checkBeginTurn() {
        GameState state = new GameState(BASE, Menu.base());
        TurnController game = controller(state, ALL_REGULAR);

        TurnStart start = game.beginTurn();
        assert start.events().equals(List.of(new GameEvent(1, "C1 (Regular) arrives")));
        Snapshot snapshot = start.snapshot();
        assert snapshot.turn() == 1;
        assert ids(snapshot.queue()).equals(List.of("C1"));
        assert ids(snapshot.customers()).equals(List.of("C1"));
        assert snapshot.tables().stream().allMatch(t -> t.occupantId().isEmpty());
        assert snapshot.phase() == Phase.WAITER && game.phase() == Phase.WAITER;
        assert game.currentRole().equals(Optional.of(StaffRole.WAITER));
        assert snapshot.currentRole().equals(Optional.of(StaffRole.WAITER));
        assert snapshot.legalActions().equals(new Waiter().legalActions(state));

        accept(game, StaffRole.WAITER, "WAIT");
        accept(game, StaffRole.CHEF, "WAIT");
        game.endTurn();
        TurnStart second = game.beginTurn();
        assert second.events().isEmpty();
        assert second.snapshot().turn() == 2;
        assert ids(second.snapshot().queue()).equals(List.of("C1"));
    }

    private static void checkArrivalTurns() {
        TurnController game = controller(ALL_REGULAR);
        List<Integer> arrivalTurns = new ArrayList<>();
        for (int turn = 1; turn <= BASE.turns(); turn++) {
            TurnStart start = game.beginTurn();
            if (!start.events().isEmpty()) {
                assert start.events().size() == 1;
                assert start.events().get(0).text().equals("C" + (arrivalTurns.size() + 1) + " (Regular) arrives");
                arrivalTurns.add(turn);
            }
            accept(game, StaffRole.WAITER, "WAIT");
            accept(game, StaffRole.CHEF, "WAIT");
            game.endTurn();
        }
        assert arrivalTurns.equals(List.of(1, 4, 7, 10, 13, 16)) : arrivalTurns;
        assert ids(game.snapshot().customers()).equals(List.of("C1", "C2", "C3", "C4", "C5", "C6"));
    }

    private static void checkWrongPhase() {
        TurnController game = controller(ALL_REGULAR);
        assertThrows(game::endTurn);
        assert !game.submit(action(StaffRole.WAITER, "WAIT")).accepted();
        assert game.phase() == Phase.TURN_START;

        game.beginTurn();
        assertThrows(game::beginTurn);
        assertThrows(game::endTurn);
        ActionResult chefEarly = game.submit(action(StaffRole.CHEF, "WAIT"));
        assert !chefEarly.accepted();
        assert game.phase() == Phase.WAITER;

        accept(game, StaffRole.WAITER, "WAIT");
        assert !game.submit(action(StaffRole.WAITER, "WAIT")).accepted();
        assert game.phase() == Phase.CHEF;
        accept(game, StaffRole.CHEF, "WAIT");
        assert !game.submit(action(StaffRole.CHEF, "WAIT")).accepted();
        assertThrows(game::beginTurn);
        assert game.phase() == Phase.TURN_END;
    }

    /** The Stage 3 base game: Host phase first, the combo on the menu, and Happy Hour available before turn 1. */
    private static void checkStage3Setup() {
        Menu menu = GameSetup.stage3Menu();
        List<MenuItem> items = menu.items();
        assert items.size() == 4;
        assert items.subList(0, 3).stream().map(MenuItem::name).toList().equals(List.of("Salad", "Burger", "Pasta"));
        ComboMeal combo = (ComboMeal) menu.item("Salad + Pasta combo");
        assert items.get(3) == combo;
        assert combo.first() == items.get(0) && combo.second() == items.get(2);
        assert combo.priceCents() == 2430 && combo.preparationUnits() == 3;
        assert Menu.base().items().size() == 3;

        TurnController game = GameSetup.baseGame();
        Snapshot before = game.snapshot();
        assert before.turn() == 0 && before.phase() == Phase.TURN_START;
        assert before.happyHourAvailable() && before.happyHourRemaining() == 0;
        assert before.legalActions().isEmpty() && before.currentRole().isEmpty();

        TurnStart start = game.beginTurn(false);
        assert start.events().equals(List.of(new GameEvent(1, "C1 (Regular) arrives")));
        Snapshot snapshot = start.snapshot();
        assert snapshot.phase() == Phase.HOST && game.phase() == Phase.HOST;
        assert game.currentRole().equals(Optional.of(StaffRole.HOST));
        assert snapshot.currentRole().equals(Optional.of(StaffRole.HOST));
        assert snapshot.legalActions().equals(List.of(action(StaffRole.HOST, "SEAT C1"), action(StaffRole.HOST, "WAIT")));
        assert !snapshot.happyHourAvailable() && snapshot.happyHourRemaining() == 0;

        // A legacy controller has no Host phase and never offers Happy Hour.
        TurnController legacy = controller(STAGE_2);
        assert !legacy.snapshot().happyHourAvailable() && legacy.snapshot().happyHourRemaining() == 0;
        assert legacy.beginTurn(false).snapshot().phase() == Phase.WAITER;
    }

    /** In the HOST phase only HOST SEAT and HOST WAIT are accepted; every rejection leaves the whole snapshot unchanged. */
    private static void checkHostPhase() {
        TurnController game = stage3(stage3State());
        game.beginTurn(false);
        Snapshot before = game.snapshot();
        List<Action> rejected = List.of(
                action(StaffRole.WAITER, "SEAT C1"),
                action(StaffRole.WAITER, "WAIT"),
                action(StaffRole.CHEF, "WAIT"),
                action(StaffRole.HOST, "ORDER C1"),
                action(StaffRole.HOST, "SERVE O1"),
                action(StaffRole.HOST, "PREPARE O1"),
                action(StaffRole.HOST, "SEAT C9"),
                action(StaffRole.HOST, "SEAT O1"));
        for (Action action : rejected) {
            ActionResult result = game.submit(action);
            assert !result.accepted() && result.events().isEmpty() && !result.message().isEmpty() : action;
            assert game.snapshot().equals(before) : action;
        }
        assert game.submit(action(StaffRole.HOST, "SEAT C9")).message().equals("C9 is not in the waiting queue.");
        assert game.phase() == Phase.HOST;

        // Host WAIT uses the Host's task only; the Waiter may still seat the customer the Host left in the queue.
        assert game.submit(action(StaffRole.HOST, "WAIT")).equals(ActionResult.accepted("Host waits.", List.of()));
        assert game.phase() == Phase.WAITER;
        assert !game.submit(action(StaffRole.HOST, "WAIT")).accepted();
        assert game.snapshot().legalActions().equals(List.of(action(StaffRole.WAITER, "SEAT C1"), action(StaffRole.WAITER, "WAIT")));
        ActionResult seat = game.submit(action(StaffRole.WAITER, "SEAT C1"));
        assert seat.events().equals(List.of(new GameEvent(1, "C1 seated at T1")));
        assert game.phase() == Phase.CHEF;
        accept(game, StaffRole.CHEF, "WAIT");
        assert game.phase() == Phase.TURN_END && game.currentRole().isEmpty();
        assert !game.submit(action(StaffRole.HOST, "WAIT")).accepted();
        game.endTurn();
        assert game.phase() == Phase.TURN_START;
        assert !game.submit(action(StaffRole.HOST, "WAIT")).accepted();
    }

    /** Host and Waiter seat different queued customers in one turn; then a full house leaves the Host only WAIT. */
    private static void checkHostAndWaiterSeating() {
        TurnController game = stage3(stage3State());
        List<String> log = new ArrayList<>();
        for (int turn = 1; turn <= 3; turn++) {
            playStage3Turn(game, false, "WAIT", "WAIT", "WAIT", log);
        }
        game.beginTurn(false);
        assert ids(game.snapshot().queue()).equals(List.of("C1", "C2"));
        assert game.submit(action(StaffRole.HOST, "SEAT C2")).events().equals(List.of(new GameEvent(4, "C2 seated at T1")));
        assert game.snapshot().legalActions().equals(List.of(action(StaffRole.WAITER, "SEAT C1"),
                action(StaffRole.WAITER, "ORDER C2"), action(StaffRole.WAITER, "WAIT")));
        assert game.submit(action(StaffRole.WAITER, "SEAT C1")).events().equals(List.of(new GameEvent(4, "C1 seated at T2")));
        accept(game, StaffRole.CHEF, "WAIT");
        game.endTurn();

        game.beginTurn(false);
        Snapshot snapshot = game.snapshot();
        assert occupant(snapshot, "T1").equals(Optional.of("C2")) && occupant(snapshot, "T2").equals(Optional.of("C1"));
        assert snapshot.queue().isEmpty();
        assert snapshot.legalActions().equals(List.of(action(StaffRole.HOST, "WAIT")));
        ActionResult full = game.submit(action(StaffRole.HOST, "SEAT C1"));
        assert !full.accepted() && game.snapshot().equals(snapshot);
    }

    /** Wrong-phase, repeated, and legacy activation throw before any change; activation is once per game. */
    private static void checkHappyHourActivationRules() {
        TurnController legacy = controller(STAGE_2);
        Snapshot legacyBefore = legacy.snapshot();
        assertThrows(() -> legacy.beginTurn(true));
        assert legacy.snapshot().equals(legacyBefore);
        assert legacy.beginTurn(false).events().equals(List.of(new GameEvent(1, "C1 (Regular) arrives")));
        assertThrows(() -> legacy.beginTurn(true));

        GameState state = stage3State();
        TurnController game = stage3(state);
        game.beginTurn(false);
        Snapshot hostPhase = game.snapshot();
        assertThrows(() -> game.beginTurn(true));
        assert game.snapshot().equals(hostPhase) && !state.happyHourUsed();
        accept(game, StaffRole.HOST, "WAIT");
        accept(game, StaffRole.WAITER, "WAIT");
        assertThrows(() -> game.beginTurn(true));
        accept(game, StaffRole.CHEF, "WAIT");
        assertThrows(() -> game.beginTurn(true));
        assert !state.happyHourUsed();
        game.endTurn();

        assert game.snapshot().happyHourAvailable() && !state.happyHourActive();
        TurnStart activation = game.beginTurn(true);
        assert activation.events().equals(List.of(new GameEvent(2, "Happy Hour activated"),
                new GameEvent(2, "C1 recovers 8 satisfaction (100)")));
        assert activation.snapshot().happyHourRemaining() == 2 && !activation.snapshot().happyHourAvailable();
        assert state.happyHourUsed() && state.happyHourActive() && state.happyHourRemaining() == 2;
        accept(game, StaffRole.HOST, "WAIT");
        accept(game, StaffRole.WAITER, "WAIT");
        accept(game, StaffRole.CHEF, "WAIT");
        game.endTurn();

        Snapshot between = game.snapshot();
        assert between.phase() == Phase.TURN_START && !between.happyHourAvailable();
        assertThrows(() -> game.beginTurn(true));
        assert game.snapshot().equals(between);
        TurnStart next = game.beginTurn(false);
        assert next.events().equals(List.of(new GameEvent(3, "C1 recovers 8 satisfaction (100)")));
        assert next.snapshot().happyHourRemaining() == 1 && state.happyHourActive();
    }

    /**
     * Turn 1 locks O1 at full price before Happy Hour; turn 4 activates it, and VIP C2 orders Burger through the Waiter at
     * $15.00 x 0.9 x 0.8 = $10.80. Both bills stay as locked after Happy Hour ends and are the amounts paid.
     */
    private static void checkHappyHourPricingAndRecovery() {
        GameState state = stage3State();
        TurnController game = stage3(state);
        List<String> log = new ArrayList<>();
        playStage3Turn(game, false, "SEAT C1", "ORDER C1", "WAIT", log);
        playStage3Turn(game, false, "WAIT", "WAIT", "WAIT", log);
        playStage3Turn(game, false, "WAIT", "WAIT", "WAIT", log);
        assert customer(game.snapshot(), "C1").satisfaction() == 76;

        TurnStart turn4 = game.beginTurn(true);
        assert turn4.events().equals(List.of(new GameEvent(4, "Happy Hour activated"),
                new GameEvent(4, "C1 recovers 15 satisfaction (91)"), new GameEvent(4, "C2 (VIP) arrives")));
        assert customer(turn4.snapshot(), "C2").satisfaction() == 100;
        accept(game, StaffRole.HOST, "SEAT C2");
        ActionResult vipOrder = game.submit(action(StaffRole.WAITER, "ORDER C2"));
        assert vipOrder.events().equals(List.of(new GameEvent(4, "C2 orders Burger ($10.80) as O2")));
        accept(game, StaffRole.CHEF, "PREPARE O1");
        game.endTurn();
        assert order(game.snapshot(), "O1").billCents() == 900;
        assert order(game.snapshot(), "O2").billCents() == 1080;

        // Turn 5: still active, so both seated customers recover; C2's 5 points reach the cap of 100.
        TurnStart turn5 = game.beginTurn(false);
        assert turn5.events().equals(List.of(new GameEvent(5, "C1 recovers 15 satisfaction (98)"),
                new GameEvent(5, "C2 recovers 5 satisfaction (100)")));
        assert turn5.snapshot().happyHourRemaining() == 1;
        accept(game, StaffRole.HOST, "WAIT");
        accept(game, StaffRole.WAITER, "SERVE O1");
        accept(game, StaffRole.CHEF, "PREPARE O2");
        game.endTurn();

        playStage3Turn(game, false, "WAIT", "SERVE O2", "WAIT", log);
        assert !state.happyHourActive() && state.happyHourRemaining() == 0;
        playStage3Turn(game, false, "WAIT", "WAIT", "WAIT", log);
        playStage3Turn(game, false, "WAIT", "WAIT", "WAIT", log);
        assert log.stream().filter(line -> line.contains(" recovers ")).toList().isEmpty();
        assert log.contains("7: C1 paid $9.00 (satisfaction 98), T1 free") : log;
        assert log.contains("8: C2 paid $10.80 (satisfaction 95), T2 free") : log;
        assert order(game.snapshot(), "O1").billCents() == 900 && order(game.snapshot(), "O2").billCents() == 1080;
        assert game.snapshot().revenueCents() == 1980;
    }

    /** Turn 18 activation recovers every customer still waiting, not the one who left, and adds no turn 19. */
    private static void checkHappyHourOnLastTurn() {
        GameState state = stage3State();
        TurnController game = stage3(state);
        List<String> log = new ArrayList<>();
        for (int turn = 1; turn <= 17; turn++) {
            playStage3Turn(game, false, "WAIT", "WAIT", "WAIT", log);
        }
        assert game.snapshot().happyHourAvailable();
        TurnStart last = game.beginTurn(true);
        assert last.events().equals(List.of(new GameEvent(18, "Happy Hour activated"),
                new GameEvent(18, "C2 recovers 15 satisfaction (45)"),
                new GameEvent(18, "C3 recovers 15 satisfaction (27)"),
                new GameEvent(18, "C4 recovers 15 satisfaction (19)"),
                new GameEvent(18, "C5 recovers 15 satisfaction (75)"),
                new GameEvent(18, "C6 recovers 15 satisfaction (99)")));
        CustomerView c1 = customer(last.snapshot(), "C1");
        assert c1.status() == CustomerStatus.LEFT && c1.satisfaction() == 0;
        assert last.snapshot().happyHourRemaining() == 2;
        accept(game, StaffRole.HOST, "WAIT");
        accept(game, StaffRole.WAITER, "WAIT");
        accept(game, StaffRole.CHEF, "WAIT");
        TurnEnd end = game.endTurn();

        assert end.summary().turn() == 18 && end.summary().leftCount() == 1;
        assert end.result().equals(Optional.of(GameResult.DEFEAT));
        assert game.isOver() && game.phase() == Phase.GAME_OVER && state.turn() == 18;
        assert !game.snapshot().happyHourAvailable();
        assertThrows(() -> game.beginTurn(false));
        assertThrows(() -> game.beginTurn(true));
        assert game.snapshot().turn() == 18;
        Map<String, Integer> waiting = Map.of("C2", 40, "C3", 19, "C4", 7, "C5", 67, "C6", 91);
        waiting.forEach((id, satisfaction) -> {
            assert customer(game.snapshot(), id).satisfaction() == satisfaction : id;
        });
    }

    /** The fixtures/stage-3/host-combo-happy-hour trace from issue #19, on GameSetup.baseGame(). */
    private static void checkStage3Victory() {
        TurnController game = GameSetup.baseGame();
        String[][] turns = {
            {"SEAT C1", "ORDER C1", "PREPARE O1"}, {"WAIT", "SERVE O1", "WAIT"}, {"WAIT", "WAIT", "WAIT"},
            {"SEAT C2", "ORDER C2", "PREPARE O2"}, {"WAIT", "SERVE O2", "WAIT"}, {"WAIT", "WAIT", "WAIT"},
            {"SEAT C3", "ORDER C3", "PREPARE O3"}, {"WAIT", "WAIT", "PREPARE O3"}, {"WAIT", "WAIT", "PREPARE O3"},
            {"SEAT C4", "SERVE O3", "WAIT"}, {"WAIT", "ORDER C4", "PREPARE O4"}, {"WAIT", "WAIT", "PREPARE O4"},
            {"SEAT C5", "SERVE O4", "WAIT"}, {"WAIT", "ORDER C5", "PREPARE O5"}, {"WAIT", "SERVE O5", "WAIT"},
            {"WAIT", "WAIT", "WAIT"}, {"WAIT", "WAIT", "WAIT"}, {"WAIT", "WAIT", "WAIT"}};
        List<String> log = new ArrayList<>();
        List<Snapshot> starts = new ArrayList<>();
        List<Snapshot> ends = new ArrayList<>();
        List<TurnEnd> turnEnds = new ArrayList<>();
        int managerChoices = 0;
        for (int turn = 1; turn <= BASE.turns(); turn++) {
            if (game.snapshot().happyHourAvailable()) {
                managerChoices++;
            }
            assert game.snapshot().happyHourAvailable() == (turn <= 7) : turn;
            String[] staff = turns[turn - 1];
            turnEnds.add(playStage3Turn(game, turn == 7, staff[0], staff[1], staff[2], log, starts));
            ends.add(game.snapshot());
        }
        assert managerChoices + 3 * BASE.turns() == 61;

        assert starts.stream().map(Snapshot::happyHourRemaining).toList()
                .equals(List.of(0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assert log.stream().filter(line -> line.contains("Happy Hour") || line.contains(" recovers ")).toList()
                .equals(List.of("7: Happy Hour activated", "8: C3 recovers 8 satisfaction (100)"));
        assert log.indexOf("7: Happy Hour activated") == log.indexOf("7: C3 (Regular) arrives") - 1;
        // Only waiting or seated customers recover: on turn 7 C1 has paid and C2 is ready to pay, so neither changes.
        CustomerView c1AtActivation = customer(starts.get(6), "C1");
        CustomerView c2AtActivation = customer(starts.get(6), "C2");
        assert c1AtActivation.status() == CustomerStatus.PAID && c1AtActivation.satisfaction() == 92;
        assert c2AtActivation.status() == CustomerStatus.READY_TO_PAY && c2AtActivation.satisfaction() == 95;
        assert customer(starts.get(6), "C3").satisfaction() == 100;
        assert customer(starts.get(7), "C3").satisfaction() == 100;
        assert satisfactionByTurn(ends, "C3", 7, 9).equals(List.of(92, 92, 84));

        assert log.stream().filter(line -> line.contains(" seated at ")).toList().equals(List.of(
                "1: C1 seated at T1", "4: C2 seated at T2", "7: C3 seated at T1", "10: C4 seated at T2",
                "13: C5 seated at T1"));
        // A payment at the end of a turn frees its table only after that turn's Host has acted.
        assert log.indexOf("4: C2 seated at T2") < log.indexOf("4: C1 paid $9.00 (satisfaction 92), T1 free");
        assert log.indexOf("7: C3 seated at T1") < log.indexOf("7: C2 paid $13.50 (satisfaction 95), T2 free");

        assert log.contains("7: C3 orders Salad + Pasta combo ($19.44) as O3");
        assert log.containsAll(List.of("7: O3 unit 1 of 3 done", "8: O3 unit 2 of 3 done", "9: O3 is READY"));
        Snapshot last = ends.get(17);
        OrderView o3 = order(last, "O3");
        assert o3.itemName().equals("Salad + Pasta combo") && o3.unitsRequired() == 3 && o3.billCents() == 1944;
        Map<String, Integer> bills = Map.of("O1", 900, "O2", 1350, "O3", 1944, "O4", 1800, "O5", 1500);
        bills.forEach((id, cents) -> {
            assert order(last, id).billCents() == cents : id;
            assert order(last, id).status() == OrderStatus.PAID : id;
        });

        List<String> payments = log.stream().filter(line -> line.contains(" paid ")).toList();
        assert payments.equals(List.of(
                "4: C1 paid $9.00 (satisfaction 92), T1 free",
                "7: C2 paid $13.50 (satisfaction 95), T2 free",
                "12: C3 paid $19.44 (satisfaction 84), T1 free",
                "15: C4 paid $18.00 (satisfaction 64), T2 free",
                "17: C5 paid $15.00 (satisfaction 84), T1 free")) : payments;
        for (String id : List.of("C1", "C2", "C3", "C4", "C5")) {
            assert customer(last, id).status() == CustomerStatus.PAID : id;
        }
        CustomerView c6 = customer(last, "C6");
        assert c6.status() == CustomerStatus.WAITING && c6.satisfaction() == 76 && c6.orderId().isEmpty();
        assert last.orders().size() == 5;

        TurnSummary summary = turnEnds.get(17).summary();
        assert summary.turn() == 18 && summary.paidCount() == 5 && summary.revenueCents() == 7494;
        assert summary.satisfactionSum() == 419 && summary.servedCount() == 5 && summary.leftCount() == 0;
        assert turnEnds.get(17).result().equals(Optional.of(GameResult.VICTORY));
        assert turnEnds.subList(0, 17).stream().allMatch(e -> e.result().isEmpty());
        assert game.isOver();
    }

    /** The fixtures/stage-3/defeat trace: SKIP and three WAITs every turn, so Happy Hour is offered on all 18 turns. */
    private static void checkStage3Defeat() {
        GameState state = stage3State();
        TurnController game = stage3(state);
        List<String> log = new ArrayList<>();
        int accepted = 0;
        TurnEnd end = null;
        for (int turn = 1; turn <= BASE.turns(); turn++) {
            assert game.snapshot().happyHourAvailable() : turn;
            end = playStage3Turn(game, false, "WAIT", "WAIT", "WAIT", log);
            accepted += 4;
            assert game.snapshot().happyHourRemaining() == 0;
        }
        assert accepted == 72;
        assert !state.happyHourUsed();
        assert log.stream().filter(line -> line.contains(" left ")).toList()
                .equals(List.of("13: C1 left unhappy", "18: C4 left unhappy"));
        assert log.stream().noneMatch(line -> line.contains("Happy Hour") || line.contains(" recovers ")
                || line.contains(" orders ") || line.contains(" served ") || line.contains(" paid "));

        TurnSummary summary = end.summary();
        assert summary.revenueCents() == 0 && summary.paidCount() == 0 && summary.servedCount() == 0;
        assert summary.satisfactionSum() == 0 && summary.leftCount() == 2;
        assert end.result().equals(Optional.of(GameResult.DEFEAT));

        Snapshot snapshot = game.snapshot();
        assert snapshot.orders().isEmpty();
        assert snapshot.tables().stream().allMatch(t -> t.occupantId().isEmpty());
        Map<String, Integer> waiting = Map.of("C2", 25, "C3", 4, "C5", 52, "C6", 76);
        waiting.forEach((id, satisfaction) -> {
            assert customer(snapshot, id).status() == CustomerStatus.WAITING : id;
            assert customer(snapshot, id).satisfaction() == satisfaction : id;
        });
        assert game.isOver();
    }

    private static void assertThrows(Runnable call) {
        try {
            call.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("expected IllegalStateException");
    }

    private static void checkPhaseAdvance() {
        TurnController game = controller(ALL_REGULAR);
        game.beginTurn();

        Snapshot before = game.snapshot();
        assert !game.submit(action(StaffRole.WAITER, "SERVE O1")).accepted();
        assert !game.submit(action(StaffRole.WAITER, "SEAT C9")).accepted();
        assert !game.submit(action(StaffRole.WAITER, "PREPARE O1")).accepted();
        assert game.phase() == Phase.WAITER;
        assert game.snapshot().equals(before);

        ActionResult seat = game.submit(action(StaffRole.WAITER, "SEAT C1"));
        assert seat.accepted();
        assert game.phase() == Phase.CHEF;
        assert game.currentRole().equals(Optional.of(StaffRole.CHEF));
        assert game.snapshot().legalActions().equals(List.of(action(StaffRole.CHEF, "WAIT")));

        before = game.snapshot();
        assert !game.submit(action(StaffRole.CHEF, "PREPARE O1")).accepted();
        assert !game.submit(action(StaffRole.CHEF, "SEAT C1")).accepted();
        assert game.snapshot().equals(before);

        ActionResult wait = game.submit(action(StaffRole.CHEF, "WAIT"));
        assert wait.equals(ActionResult.accepted("Chef waits.", List.of()));
        assert game.phase() == Phase.TURN_END;
        assert game.currentRole().isEmpty();
        assert game.snapshot().legalActions().isEmpty();

        game.endTurn();
        game.beginTurn();
        assert game.submit(action(StaffRole.WAITER, "WAIT")).equals(ActionResult.accepted("Waiter waits.", List.of()));
        accept(game, StaffRole.CHEF, "WAIT");
    }

    /** The brief's Pasta example on turns 1 to 6: C1 is given arrival index 3 so it orders Pasta. */
    private static void checkPastaExample() {
        TurnController game = controller(i -> new RegularCustomer("C" + i, BASE.arrivalTurns().get(i - 1), i == 1 ? 3 : i));
        List<String> log = new ArrayList<>();
        List<Integer> satisfaction = new ArrayList<>();

        playTurn(game, "SEAT C1", "WAIT", log);
        satisfaction.add(c1Satisfaction(game));
        playTurn(game, "ORDER C1", "PREPARE O1", log);
        satisfaction.add(c1Satisfaction(game));
        assert log.contains("2: C1 orders Pasta ($18.00) as O1");

        game.beginTurn();
        Snapshot before = game.snapshot();
        assert !game.submit(action(StaffRole.WAITER, "SERVE O1")).accepted();
        assert game.snapshot().equals(before);
        accept(game, StaffRole.WAITER, "WAIT");
        accept(game, StaffRole.CHEF, "PREPARE O1");
        assert game.snapshot().orders().get(0).status() == OrderStatus.READY;
        game.endTurn();
        satisfaction.add(c1Satisfaction(game));

        playTurn(game, "SERVE O1", "WAIT", log);
        satisfaction.add(c1Satisfaction(game));
        assert satisfaction.equals(List.of(92, 84, 76, 76)) : satisfaction;

        TurnEnd turn5 = playTurn(game, "WAIT", "WAIT", log);
        assert turn5.events().contains(new GameEvent(5, "C1 is ready to pay"));
        assert turn5.summary().paidCount() == 0;

        TurnEnd turn6 = playTurn(game, "WAIT", "WAIT", log);
        assert turn6.events().contains(new GameEvent(6, "C1 paid $18.00 (satisfaction 76), T1 free"));
        assert turn6.summary().revenueCents() == 1800;
        assert turn6.summary().satisfactionSum() == 76;
        assert turn6.summary().paidCount() == 1;
        assert occupant(game.snapshot(), "T1").isEmpty();
        assert customer(game.snapshot(), "C1").status() == CustomerStatus.PAID;
    }

    private static int c1Satisfaction(TurnController game) {
        return customer(game.snapshot(), "C1").satisfaction();
    }

    private static void checkWaitingAbandonment() {
        TurnController game = controller(ALL_REGULAR);
        List<String> log = new ArrayList<>();
        TurnEnd end = null;
        for (int turn = 1; turn <= 13; turn++) {
            end = playTurn(game, "WAIT", "WAIT", log);
            if (turn < 13) {
                assert customer(game.snapshot(), "C1").status() == CustomerStatus.WAITING;
            }
        }
        assert end.events().contains(new GameEvent(13, "C1 left unhappy"));
        assert end.summary().leftCount() == 1;
        Snapshot snapshot = game.snapshot();
        assert customer(snapshot, "C1").status() == CustomerStatus.LEFT;
        assert customer(snapshot, "C1").satisfaction() == 0;
        assert !ids(snapshot.queue()).contains("C1");
        assert ids(snapshot.customers()).contains("C1");
    }

    private static void checkSeatedAbandonment() {
        TurnController game = controller(ALL_REGULAR);
        List<String> log = new ArrayList<>();
        playTurn(game, "SEAT C1", "WAIT", log);
        playTurn(game, "ORDER C1", "WAIT", log);
        TurnEnd end = null;
        for (int turn = 3; turn <= 13; turn++) {
            end = playTurn(game, "WAIT", "WAIT", log);
        }
        assert end.events().contains(new GameEvent(13, "C1 left unhappy"));
        Snapshot snapshot = game.snapshot();
        assert customer(snapshot, "C1").status() == CustomerStatus.LEFT;
        assert snapshot.orders().get(0).status() == OrderStatus.CANCELLED;
        assert occupant(snapshot, "T1").isEmpty();
        assert snapshot.tables().stream().map(TableView::occupantId).allMatch(Optional::isEmpty);
    }

    /** The wave 4 victory fixture and sequence-diagram scenario. */
    private static void checkVictory() {
        TurnController game = controller(ALL_REGULAR);
        List<String> waiter = List.of("SEAT C1", "ORDER C1", "SERVE O1", "SEAT C2", "ORDER C2", "SERVE O2",
                "SEAT C3", "ORDER C3", "WAIT", "SERVE O3", "SEAT C4", "ORDER C4", "SERVE O4", "SEAT C5", "ORDER C5",
                "SERVE O5", "WAIT", "WAIT");
        Map<Integer, String> chef = Map.of(2, "PREPARE O1", 5, "PREPARE O2", 8, "PREPARE O3", 9, "PREPARE O3",
                12, "PREPARE O4", 15, "PREPARE O5");
        List<String> log = new ArrayList<>();
        List<TurnEnd> ends = playGame(game, waiter, chef, log);

        List<String> payments = log.stream().filter(line -> line.contains(" paid ")).toList();
        assert payments.equals(List.of(
                "5: C1 paid $9.00 (satisfaction 84), T1 free",
                "8: C2 paid $15.00 (satisfaction 84), T2 free",
                "12: C3 paid $18.00 (satisfaction 76), T1 free",
                "15: C4 paid $9.00 (satisfaction 76), T2 free",
                "18: C5 paid $15.00 (satisfaction 76), T1 free")) : payments;
        assert log.contains("11: C4 seated at T2");
        assert log.contains("14: C5 seated at T1");

        Snapshot snapshot = game.snapshot();
        assert customer(snapshot, "C6").status() == CustomerStatus.WAITING;
        assert customer(snapshot, "C6").satisfaction() == 76;

        TurnEnd last = ends.get(17);
        assert last.summary().turn() == 18;
        assert last.summary().paidCount() == 5;
        assert last.summary().revenueCents() == 6600;
        assert last.summary().satisfactionSum() == 396;
        assert last.result().equals(Optional.of(GameResult.VICTORY));
        assert ends.subList(0, 17).stream().allMatch(e -> e.result().isEmpty());
        assert game.isOver() && game.phase() == Phase.GAME_OVER;
        assertThrows(game::beginTurn);
        assert !game.submit(action(StaffRole.WAITER, "WAIT")).accepted();
    }

    private static void checkDefeat() {
        TurnController game = controller(ALL_REGULAR);
        List<String> log = new ArrayList<>();
        List<TurnEnd> ends = playGame(game, allWait(), Map.of(), log);

        TurnEnd last = ends.get(17);
        assert last.result().equals(Optional.of(GameResult.DEFEAT));
        assert last.summary().paidCount() == 0;
        assert last.summary().leftCount() == 2;
        assert log.stream().filter(line -> line.contains(" left ")).toList()
                .equals(List.of("13: C1 left unhappy", "16: C2 left unhappy"));
        assert game.isOver();
    }

    /** The Stage 2 vip-critic fixture: C2 (VIP) and C4 (Critic) both pay. */
    private static void checkStage2Victory() {
        TurnController game = controller(STAGE_2);
        List<String> waiter = List.of("SEAT C1", "ORDER C1", "SERVE O1", "SEAT C2", "ORDER C2", "SERVE O2",
                "SEAT C3", "ORDER C3", "WAIT", "SERVE O3", "SEAT C4", "ORDER C4", "SEAT C5", "SERVE O4", "ORDER C5",
                "SERVE O5", "WAIT", "WAIT");
        Map<Integer, String> chef = Map.of(2, "PREPARE O1", 5, "PREPARE O2", 8, "PREPARE O3", 9, "PREPARE O3",
                12, "PREPARE O4", 13, "PREPARE O4", 15, "PREPARE O5");
        List<String> log = new ArrayList<>();
        List<Snapshot> snapshots = new ArrayList<>();
        List<TurnEnd> ends = playGame(game, waiter, chef, log, snapshots);

        assert log.contains("4: C2 (VIP) arrives");
        assert log.contains("10: C4 (Critic) arrives");

        Snapshot snapshot = game.snapshot();
        OrderView o2 = order(snapshot, "O2");
        assert o2.customerId().equals("C2") && o2.itemName().equals("Burger") && o2.billCents() == 1350 : o2;
        OrderView o4 = order(snapshot, "O4");
        assert o4.customerId().equals("C4") && o4.itemName().equals("Pasta") && o4.billCents() == 1800 : o4;

        assert satisfactionByTurn(snapshots, "C2", 4, 5).equals(List.of(95, 90));
        assert satisfactionByTurn(snapshots, "C4", 10, 13).equals(List.of(88, 76, 64, 52));

        List<String> payments = log.stream().filter(line -> line.contains(" paid ")).toList();
        assert payments.equals(List.of(
                "5: C1 paid $9.00 (satisfaction 84), T1 free",
                "8: C2 paid $13.50 (satisfaction 90), T2 free",
                "12: C3 paid $18.00 (satisfaction 76), T1 free",
                "16: C4 paid $18.00 (satisfaction 52), T2 free",
                "18: C5 paid $15.00 (satisfaction 76), T1 free")) : payments;
        assert log.contains("11: C4 seated at T2");
        assert log.contains("13: C5 seated at T1");

        assert customer(snapshot, "C6").status() == CustomerStatus.WAITING;
        assert customer(snapshot, "C6").satisfaction() == 76;

        TurnEnd last = ends.get(17);
        assert last.summary().turn() == 18;
        assert last.summary().paidCount() == 5;
        assert last.summary().revenueCents() == 7350;
        assert last.summary().satisfactionSum() == 378;
        assert last.result().equals(Optional.of(GameResult.VICTORY));
        assert ends.subList(0, 17).stream().allMatch(e -> e.result().isEmpty());
        assert game.isOver();
    }

    /** The Stage 2 defeat fixture: all WAIT, and the Critic walks out on turn 18. */
    private static void checkStage2Defeat() {
        TurnController game = controller(STAGE_2);
        List<String> log = new ArrayList<>();
        List<TurnEnd> ends = playGame(game, allWait(), Map.of(), log);

        assert log.stream().filter(line -> line.contains(" left ")).toList()
                .equals(List.of("13: C1 left unhappy", "18: C4 left unhappy"));
        TurnEnd last = ends.get(17);
        assert last.summary().leftCount() == 2;
        assert last.summary().paidCount() == 0;
        assert last.result().equals(Optional.of(GameResult.DEFEAT));

        Snapshot snapshot = game.snapshot();
        Map<String, Integer> waiting = Map.of("C2", 25, "C3", 4, "C5", 52, "C6", 76);
        waiting.forEach((id, satisfaction) -> {
            assert customer(snapshot, id).status() == CustomerStatus.WAITING : id;
            assert customer(snapshot, id).satisfaction() == satisfaction : id + " " + customer(snapshot, id).satisfaction();
        });
        assert game.isOver();
    }
}
