package restaurantrush.game;

/** A displayable event such as an arrival, a food choice, or a payment. */
public record GameEvent(int turn, String text) {
}
