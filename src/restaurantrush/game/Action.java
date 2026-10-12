package restaurantrush.game;

/** One manager choice for one role. targetId is null for WAIT. */
public record Action(StaffRole role, ActionKind kind, String targetId) {
}
