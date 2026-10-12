package restaurantrush.game;

/** End-of-turn figures. The average is satisfactionSum / paidCount, or N/A when paidCount is 0. */
public record TurnSummary(int turn, int servedCount, int leftCount, int revenueCents, int paidCount, int satisfactionSum) {
}
