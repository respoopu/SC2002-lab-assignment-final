package restaurantrush.game;

import java.util.List;

public record TurnStart(List<GameEvent> events, Snapshot snapshot) {
    public TurnStart {
        events = List.copyOf(events);
    }
}
