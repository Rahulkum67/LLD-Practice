package com.lld;

import java.util.concurrent.*;

/** Decides where evaluation runs. Sync for tests, threaded for the web app, a queue consumer later. */
public interface Runner {
    void run(Runnable task);

    static Runner sync() { return Runnable::run; }

    static Runner threaded(int workers) {
        ExecutorService ex = Executors.newFixedThreadPool(workers, r -> { Thread t = new Thread(r, "evaluator"); t.setDaemon(true); return t; });
        return ex::execute;
    }
}
