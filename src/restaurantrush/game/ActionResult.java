package restaurantrush.game;

import java.util.List;

/** Outcome of a submitted action. A rejection changes no state and consumes no task. */
public record ActionResult(boolean accepted, String message, List<GameEvent> events) {
    public ActionResult {
        events = List.copyOf(events);
    }

    public static ActionResult accepted(String message, List<GameEvent> events) {
        return new ActionResult(true, message, List.copyOf(events));
    }

    public static ActionResult rejected(String reason) {
        return new ActionResult(false, reason, List.of());
    }
}
