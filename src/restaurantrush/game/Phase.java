package restaurantrush.game;

/** Turn phases in order. TURN_START awaits beginTurn; TURN_END awaits endTurn. The phase alone enforces one task per role per turn. */
public enum Phase { TURN_START, WAITER, CHEF, TURN_END, GAME_OVER }
