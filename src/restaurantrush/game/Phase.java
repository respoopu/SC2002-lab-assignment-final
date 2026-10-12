package restaurantrush.game;

/** Turn phases in order. Stage 3 inserts HOST before WAITER. */
public enum Phase { TURN_START, WAITER, CHEF, EATING_AND_PAYMENT, WAITING_AND_ABANDONMENT, TURN_END, GAME_OVER }
