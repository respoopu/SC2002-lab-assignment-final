package restaurantrush.game;

import restaurantrush.menu.OrderStatus;

public record OrderView(String id, String customerId, String itemName, OrderStatus status, int unitsDone, int unitsRequired, int billCents) {
}
