package com.lld;

import java.util.List;

public interface AttemptRepository {
    void save(Attempt a);
    Attempt get(String id);
    /** Ordered by creation time. problemId may be null for all problems. */
    List<Attempt> listFor(String learnerId, String problemId);
}
