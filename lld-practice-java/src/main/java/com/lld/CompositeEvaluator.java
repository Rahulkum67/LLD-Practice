package com.lld;

import java.util.*;

/** Runs evaluators independently: one failing degrades feedback; all failing fails the evaluation. */
public class CompositeEvaluator implements FeedbackEngine {
    private final List<Evaluator> evaluators;

    public CompositeEvaluator(List<Evaluator> evaluators) { this.evaluators = List.copyOf(evaluators); }

    public Feedback evaluate(EvalContext ctx) {
        Feedback fb = new Feedback();
        int ok = 0;
        for (Evaluator ev : evaluators) {
            try {
                EvalResult r = ev.evaluate(ctx);
                fb.findings.addAll(r.findings());
                fb.scores.putAll(r.scores());
                ok++;
            } catch (Exception e) {
                fb.degraded = true;
                fb.notes.add(ev.name() + " evaluator unavailable: " + e.getMessage());
            }
        }
        if (ok == 0) throw new Errors.Evaluator(fb.notes.isEmpty() ? "no evaluators configured" : String.join("; ", fb.notes));
        List<String> order = List.of(Severity.ISSUE.value(), Severity.WARNING.value(), Severity.STRENGTH.value());
        fb.findings.sort(Comparator.comparingInt(x -> order.indexOf(x.severity())));
        fb.summary = count(fb, Severity.ISSUE) + " issues, " + count(fb, Severity.WARNING) + " warnings, " + count(fb, Severity.STRENGTH) + " strengths."
                + (fb.degraded ? " (Partial feedback: see notes.)" : "");
        return fb;
    }

    private static long count(Feedback fb, Severity s) { return fb.findings.stream().filter(f -> f.severity().equals(s.value())).count(); }
}
