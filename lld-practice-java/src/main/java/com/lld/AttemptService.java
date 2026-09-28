package com.lld;

import java.time.Clock;
import java.util.*;

/** The practice loop and its failure handling. Evaluation runs via a Runner and never throws to callers. */
public class AttemptService {
    private final Object lock = new Object();
    private final AttemptRepository repo;
    private final Catalog catalog;
    private final ParserRegistry parsers;
    private final FeedbackEngine engine;
    private final Runner runner;
    private final Clock clock;
    private final long timeoutMs;

    public AttemptService(AttemptRepository repo, Catalog catalog, ParserRegistry parsers, FeedbackEngine engine,
                          Runner runner, Clock clock, long evaluationTimeoutMs) {
        this.repo = repo; this.catalog = catalog; this.parsers = parsers; this.engine = engine;
        this.runner = runner; this.clock = clock; this.timeoutMs = evaluationTimeoutMs;
    }

    public Catalog catalog() { return catalog; }
    public ParserRegistry parsers() { return parsers; }

    public Attempt startAttempt(String learnerId, String problemId, String format) {
        if (learnerId == null || learnerId.isBlank()) throw new Errors.Validation("learner_id is required");
        catalog.get(problemId);
        parsers.get(format);
        synchronized (lock) {
            int n = repo.listFor(learnerId, problemId).size() + 1;
            Attempt a = new Attempt(UUID.randomUUID().toString().replace("-", "").substring(0, 10), learnerId, problemId, format, n, clock.millis());
            repo.save(a);
            return a;
        }
    }

    public Attempt saveDraft(String id, String content) {
        synchronized (lock) {
            Attempt a = repo.get(id);
            a.editContent(content);
            repo.save(a);
            return a;
        }
    }

    public Attempt submit(String id) {
        synchronized (lock) {
            Attempt a = repo.get(id);
            if (a.status() != AttemptStatus.DRAFT) throw new Errors.InvalidTransition("attempt already " + a.status().value());
            if (a.content().isBlank()) throw new Errors.Validation("submission is empty");
            parsers.get(a.format).parse(a.content());          // reject unparsable input up front; draft stays editable
            a.submit(clock.millis());
            repo.save(a);
        }
        runner.run(() -> evaluate(id));
        return getAttempt(id);
    }

    public Attempt retry(String id) {
        if (getAttempt(id).status() != AttemptStatus.FAILED) throw new Errors.InvalidTransition("only failed evaluations can be retried");
        runner.run(() -> evaluate(id));
        return getAttempt(id);
    }

    /** Also heals attempts stuck in EVALUATING (dead/hung worker) by failing them so they can be retried. */
    public Attempt getAttempt(String id) {
        synchronized (lock) {
            Attempt a = repo.get(id);
            if (a.evaluationTimedOut(clock.millis(), timeoutMs)) { a.fail("evaluation timed out"); repo.save(a); }
            return a;
        }
    }

    public List<Attempt> history(String learnerId, String problemId) {
        return repo.listFor(learnerId, problemId).stream().map(a -> getAttempt(a.id)).toList();
    }

    public Optional<Delta> compareWithPrevious(String id) {
        Attempt cur = getAttempt(id);
        if (cur.feedback() == null) return Optional.empty();
        List<Attempt> all = repo.listFor(cur.learnerId, cur.problemId);
        Optional<Attempt> prev = all.stream().filter(a -> a.number < cur.number && a.status() == AttemptStatus.EVALUATED)
                .max(Comparator.comparingInt(a -> a.number));
        if (prev.isEmpty()) return Optional.empty();
        Map<String, String> before = open(prev.get().feedback()), now = open(cur.feedback());
        Map<String, Integer> diff = new LinkedHashMap<>();
        cur.feedback().scores.forEach((k, v) -> diff.put(k, v - prev.get().feedback().scores.getOrDefault(k, 0)));
        return Optional.of(new Delta(prev.get().number,
                before.entrySet().stream().filter(e -> !now.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),
                now.entrySet().stream().filter(e -> !before.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),
                now.entrySet().stream().filter(e -> before.containsKey(e.getKey())).map(Map.Entry::getValue).toList(), diff));
    }

    private static Map<String, String> open(Feedback fb) {
        Map<String, String> m = new LinkedHashMap<>();
        for (Finding f : fb.findings) if (!f.severity().equals(Severity.STRENGTH.value())) m.put(f.id(), f.message());
        return m;
    }

    /** Runs on a worker. Claims the attempt, evaluates outside the lock, then records the outcome. */
    private void evaluate(String id) {
        Attempt a;
        synchronized (lock) {
            a = repo.get(id);
            if (a.status() != AttemptStatus.SUBMITTED && a.status() != AttemptStatus.FAILED) return;
            a.startEvaluation(clock.millis());
            repo.save(a);
        }
        Feedback fb = null;
        String err = "";
        try {
            DesignModel model = parsers.get(a.format).parse(a.content());
            fb = engine.evaluate(new EvalContext(catalog.get(a.problemId), model, a.content()));
        } catch (Exception e) {
            err = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        synchronized (lock) {
            Attempt cur = repo.get(id);
            if (cur.status() != AttemptStatus.EVALUATING) return;     // timed out meanwhile: keep the retryable state
            if (fb != null) cur.complete(fb); else cur.fail(err);
            repo.save(cur);
        }
    }
}
