package com.lld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Stores copies so callers cannot mutate stored state by accident. Swap for JDBC/SQLite later. */
public class InMemoryAttemptRepository implements AttemptRepository {
    private final Map<String, Attempt> store = new ConcurrentHashMap<>();

    public void save(Attempt a) { store.put(a.id, a.copy()); }

    public Attempt get(String id) {
        Attempt a = store.get(id);
        if (a == null) throw new Errors.NotFound("attempt '" + id + "' not found");
        return a.copy();
    }

    public List<Attempt> listFor(String learnerId, String problemId) {
        return store.values().stream()
                .filter(a -> a.learnerId.equals(learnerId) && (problemId == null || a.problemId.equals(problemId)))
                .sorted(Comparator.comparingLong((Attempt a) -> a.createdAt).thenComparingInt(a -> a.number))
                .map(Attempt::copy).toList();
    }
}
