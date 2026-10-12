package restaurantrush.game;

import java.util.Optional;
import restaurantrush.customer.CustomerStatus;

public record CustomerView(String id, String typeName, CustomerStatus status, int satisfaction, Optional<String> orderId) {
}
