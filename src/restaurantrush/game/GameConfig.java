package restaurantrush.game;

import java.util.List;

/** Game settings. Only the base setting is offered by the CLI. */
public record GameConfig(int turns, int tableCount, List<Integer> arrivalTurns) {
    public GameConfig {
        arrivalTurns = List.copyOf(arrivalTurns);
    }

    public static GameConfig base() {
        return new GameConfig(18, 2, List.of(1, 4, 7, 10, 13, 16));
    }
}
