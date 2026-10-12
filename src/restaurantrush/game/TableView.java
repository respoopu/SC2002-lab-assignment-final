package restaurantrush.game;

import java.util.Optional;

public record TableView(String id, Optional<String> occupantId) {
}
