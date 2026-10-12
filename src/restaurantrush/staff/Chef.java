package restaurantrush.staff;

import java.util.List;
import restaurantrush.game.Action;
import restaurantrush.game.ActionResult;
import restaurantrush.game.GameState;

/** One preparation unit per turn. */
public final class Chef {
    /** PREPARE per preparable order in order-ID order, then WAIT. */
    public List<Action> legalActions(GameState state) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }

    /** Checks: order preparable. Effects: order.advanceOneUnit, event. */
    public ActionResult prepare(GameState state, String orderId) {
        throw new UnsupportedOperationException("Owned by workstream: Staff and tables");
    }
}
