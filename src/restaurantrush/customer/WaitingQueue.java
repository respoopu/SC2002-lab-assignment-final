package restaurantrush.customer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Keeps arrival order for display; the manager may seat any queued customer. */
public final class WaitingQueue {
    private final List<Customer> customers = new ArrayList<>();

    public void add(Customer customer) {
        customers.add(customer);
    }

    public Optional<Customer> find(String id) {
        return customers.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    /** IllegalArgumentException if absent. */
    public Customer remove(String id) {
        Customer customer = find(id)
                .orElseThrow(() -> new IllegalArgumentException("no queued customer " + id));
        customers.remove(customer);
        return customer;
    }

    /** Unmodifiable, arrival order. */
    public List<Customer> view() {
        return Collections.unmodifiableList(customers);
    }

    public boolean isEmpty() {
        return customers.isEmpty();
    }
}
