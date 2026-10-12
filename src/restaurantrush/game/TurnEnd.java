package restaurantrush.game;

import java.util.List;
import java.util.Optional;

public record TurnEnd(List<GameEvent> events, TurnSummary summary, Optional<GameResult> result) {
    public TurnEnd {
        events = List.copyOf(events);
    }
}
