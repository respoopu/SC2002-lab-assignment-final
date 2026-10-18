package restaurantrush.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import restaurantrush.customer.Customer;
import restaurantrush.customer.CustomerStatus;
import restaurantrush.customer.RegularCustomer;
import restaurantrush.game.Action;
import restaurantrush.game.ActionKind;
import restaurantrush.game.ActionResult;
import restaurantrush.game.CustomerView;
import restaurantrush.game.GameConfig;
import restaurantrush.game.GameEvent;
import restaurantrush.game.GameResult;
import restaurantrush.game.GameSetup;
import restaurantrush.game.GameState;
import restaurantrush.game.Phase;
import restaurantrush.game.Snapshot;
import restaurantrush.game.StaffRole;
import restaurantrush.game.TableView;
import restaurantrush.game.TurnController;
import restaurantrush.game.TurnEnd;
import restaurantrush.game.TurnStart;
import restaurantrush.menu.Menu;
import restaurantrush.menu.OrderStatus;
import restaurantrush.staff.Cashier;
import restaurantrush.staff.Chef;
import restaurantrush.staff.Waiter;

/** Self-check for the game package: TurnController and GameSetup, played through the public API. Run with java -ea. */
public final class GameCheck {
    private static final GameConfig BASE = GameConfig.base();

    /** Stage 1 arrivals: every customer is Regular, so these traces survive Stage 2's GameSetup change. */
    private static final IntFunction<Customer> ALL_REGULAR =
            i -> new RegularCustomer("C" + i, BASE.arrivalTurns().get(i - 1), i);

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

        System.out.println("OK GameCheck");
    }

    private static TurnController controller(IntFunction<Customer> arrivals) {
        return controller(new GameState(BASE, Menu.base()), arrivals);
    }

    private static TurnController controller(GameState state, IntFunction<Customer> arrivals) {
        return new TurnController(state, new Waiter(), new Chef(), new Cashier(), arrivals);
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
        List<TurnEnd> ends = new ArrayList<>();
        for (int turn = 1; turn <= BASE.turns(); turn++) {
            ends.add(playTurn(controller, waiter.get(turn - 1), chef.getOrDefault(turn, "WAIT"), log));
        }
        return ends;
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

    private static List<String> ids(List<CustomerView> views) {
        return views.stream().map(CustomerView::id).toList();
    }

    private static void checkSetup() {
        Customer c3 = GameSetup.arrival(3, 7);
        assert c3.getClass() == RegularCustomer.class;
        assert c3.id().equals("C3") && c3.arrivalTurn() == 7 && c3.arrivalIndex() == 3;

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
        List<String> waits = new ArrayList<>();
        for (int turn = 1; turn <= BASE.turns(); turn++) {
            waits.add("WAIT");
        }
        List<TurnEnd> ends = playGame(game, waits, Map.of(), log);

        TurnEnd last = ends.get(17);
        assert last.result().equals(Optional.of(GameResult.DEFEAT));
        assert last.summary().paidCount() == 0;
        assert last.summary().leftCount() == 2;
        assert log.stream().filter(line -> line.contains(" left ")).toList()
                .equals(List.of("13: C1 left unhappy", "16: C2 left unhappy"));
        assert game.isOver();
    }
}
