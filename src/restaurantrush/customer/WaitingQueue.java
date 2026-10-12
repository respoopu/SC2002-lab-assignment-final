package restaurantrush.customer;

import java.util.List;
import java.util.Optional;

/** Keeps arrival order for display; the manager may seat any queued customer. */
public final class WaitingQueue {
    public void add(Customer customer) {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public Optional<Customer> find(String id) {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** IllegalArgumentException if absent. */
    public Customer remove(String id) {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    /** Unmodifiable, arrival order. */
    public List<Customer> view() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("Owned by workstream: Customers and waiting queue");
    }
}
