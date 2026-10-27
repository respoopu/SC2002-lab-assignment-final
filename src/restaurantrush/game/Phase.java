package restaurantrush.game;

/** Turn phases in order. TURN_START awaits beginTurn; TURN_END awaits endTurn. HOST is used only by a game built with a Host. The phase alone enforces one task per role per turn. */
public enum Phase { TURN_START, HOST, WAITER, CHEF, TURN_END, GAME_OVER }
