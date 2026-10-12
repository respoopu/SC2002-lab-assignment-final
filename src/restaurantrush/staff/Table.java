package restaurantrush.staff;

import java.util.Optional;

/** A table owns its occupant. */
public final class Table {
    private final String id;
    private String occupantId;

    public Table(String id) {
        this.id = id;
    }

    public String id() { return id; }
    public boolean isFree() { return occupantId == null; }
    public Optional<String> occupantId() { return Optional.ofNullable(occupantId); }

    public void seat(String customerId) {
        if (occupantId != null) {
            throw new IllegalStateException(id + " is occupied by " + occupantId);
        }
        occupantId = customerId;
    }

    public void release() {
        if (occupantId == null) {
            throw new IllegalStateException(id + " is already free");
        }
        occupantId = null;
    }
}
