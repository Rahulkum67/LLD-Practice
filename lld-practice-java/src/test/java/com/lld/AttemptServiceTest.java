package com.lld;

import static com.lld.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AttemptServiceTest {
    @Test void fullLoopHistoryAndDelta() {
        AttemptService s = service(rubricOnly());
        Attempt a1 = s.startAttempt("u", "parking-lot", "outline");
        s.saveDraft(a1.id, "class ParkingLot\nclass Vehicle\nclass Floor");
        assertEquals(AttemptStatus.EVALUATED, s.submit(a1.id).status());

        Attempt a2 = s.startAttempt("u", "parking-lot", "outline");
        s.saveDraft(a2.id, GOOD);
        s.submit(a2.id);

        assertEquals(List.of(1, 2), s.history("u", "parking-lot").stream().map(a -> a.number).toList());
        Delta d = s.compareWithPrevious(a2.id).orElseThrow();
        assertFalse(d.resolved().isEmpty());
        assertTrue(d.scoreChanges().get("coverage") > 0);
        assertTrue(s.compareWithPrevious(a1.id).isEmpty());
    }

    @Test void attemptIdsAreUrlSafe() {
        AttemptService s = service(rubricOnly());
        for (int i = 0; i < 200; i++) assertTrue(s.startAttempt("u", "parking-lot", "outline").id.matches("[0-9a-f]{10}"));
    }

    @Test void historyIsPerLearnerAndProblem() {
        AttemptService s = service(rubricOnly());
        s.startAttempt("u", "parking-lot", "outline");
        s.startAttempt("v", "parking-lot", "outline");
        s.startAttempt("u", "elevator", "outline");
        assertEquals(1, s.history("u", "parking-lot").size());
        assertEquals(2, s.history("u", null).size());
    }

    @Test void rejectsEmptyUnparsableAndInvalidInput() {
        AttemptService s = service(rubricOnly());
        Attempt a = s.startAttempt("u", "parking-lot", "outline");
        assertThrows(Errors.Validation.class, () -> s.submit(a.id));                   // empty
        s.saveDraft(a.id, "gibberish");
        assertThrows(Errors.Parse.class, () -> s.submit(a.id));                        // unparsable
        assertEquals(AttemptStatus.DRAFT, s.getAttempt(a.id).status());                // still editable
        assertThrows(Errors.Validation.class, () -> s.startAttempt("u", "parking-lot", "uml"));
        assertThrows(Errors.NotFound.class, () -> s.startAttempt("u", "nope", "outline"));
        assertThrows(Errors.Validation.class, () -> s.startAttempt(" ", "parking-lot", "outline"));
        assertThrows(Errors.NotFound.class, () -> s.getAttempt("missing"));
    }

    @Test void cannotDoubleSubmitEditAfterSubmitOrRetryHealthyAttempt() {
        AttemptService s = service(rubricOnly());
        Attempt a = s.startAttempt("u", "parking-lot", "outline");
        s.saveDraft(a.id, GOOD);
        s.submit(a.id);
        assertThrows(Errors.InvalidTransition.class, () -> s.submit(a.id));
        assertThrows(Errors.InvalidTransition.class, () -> s.saveDraft(a.id, "x"));
        assertThrows(Errors.InvalidTransition.class, () -> s.retry(a.id));
    }

    @Test void failedEvaluationCanBeRetried() {
        AtomicBoolean down = new AtomicBoolean(true);
        Evaluator flaky = new Evaluator() {
            public String name() { return "flaky"; }
            public EvalResult evaluate(EvalContext c) {
                if (down.get()) throw new IllegalStateException("provider down");
                return new RubricEvaluator().evaluate(c);
            }
        };
        AttemptService s = service(new CompositeEvaluator(List.of(flaky)));
        Attempt a = s.startAttempt("u", "parking-lot", "outline");
        s.saveDraft(a.id, GOOD);
        Attempt failed = s.submit(a.id);
        assertEquals(AttemptStatus.FAILED, failed.status());
        assertTrue(failed.error().contains("provider down"));
        down.set(false);
        assertEquals(AttemptStatus.EVALUATED, s.retry(a.id).status());
    }

    @Test void stuckEvaluationTimesOutAndIsRetryable() {
        MutableClock clock = new MutableClock();
        InMemoryAttemptRepository repo = new InMemoryAttemptRepository();
        AttemptService s = service(rubricOnly(), repo, clock, 60_000);
        Attempt a = s.startAttempt("u", "parking-lot", "outline");
        s.saveDraft(a.id, GOOD);
        Attempt stuck = repo.get(a.id);                     // simulate a worker that died mid-evaluation
        stuck.submit(clock.now); stuck.startEvaluation(clock.now);
        repo.save(stuck);
        assertEquals(AttemptStatus.EVALUATING, s.getAttempt(a.id).status());
        clock.now += 61_000;
        assertEquals(AttemptStatus.FAILED, s.getAttempt(a.id).status());
        assertEquals(AttemptStatus.EVALUATED, s.retry(a.id).status());
    }

    @Test void slowEvaluationIsAcceptedWhenNobodyObservedTheTimeout() {
        MutableClock clock = new MutableClock();
        InMemoryAttemptRepository repo = new InMemoryAttemptRepository();
        // evaluator that advances time past the timeout while "working"
        Evaluator slow = new Evaluator() {
            public String name() { return "slow"; }
            public EvalResult evaluate(EvalContext c) { clock.now += 120_000; return new RubricEvaluator().evaluate(c); }
        };
        AttemptService s = service(new CompositeEvaluator(List.of(slow)), repo, clock, 60_000);
        Attempt a = s.startAttempt("u", "parking-lot", "outline");
        s.saveDraft(a.id, GOOD);
        s.submit(a.id);
        // the run finished after the deadline, but nobody observed it in between, so the result is accepted
        assertEquals(AttemptStatus.EVALUATED, s.getAttempt(a.id).status());
    }

    @Test void javaFormatWorksEndToEnd() {
        AttemptService s = service(rubricOnly());
        Attempt a = s.startAttempt("u", "parking-lot", "java");
        s.saveDraft(a.id, "interface PricingStrategy { Money calculate(Ticket t); }\nclass HourlyPricing implements PricingStrategy { public Money calculate(Ticket t) { return null; } }\nclass ParkingLot { Ticket park(Vehicle v) { return null; } }");
        Attempt done = s.submit(a.id);
        assertEquals(AttemptStatus.EVALUATED, done.status());
        assertTrue(done.feedback().scores.get("coverage") < 100);
    }
}
