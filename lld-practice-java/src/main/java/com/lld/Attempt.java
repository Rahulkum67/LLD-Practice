package com.lld;

/** Aggregate that owns its lifecycle; illegal transitions throw. */
public class Attempt {
    public final String id, learnerId, problemId, format;
    public final int number;
    public final long createdAt;
    private String content = "", error = "";
    private AttemptStatus status = AttemptStatus.DRAFT;
    private Long submittedAt, evaluatingSince;
    private Feedback feedback;

    public Attempt(String id, String learnerId, String problemId, String format, int number, long createdAt) {
        this.id = id; this.learnerId = learnerId; this.problemId = problemId;
        this.format = format; this.number = number; this.createdAt = createdAt;
    }

    public String content() { return content; }
    public String error() { return error; }
    public AttemptStatus status() { return status; }
    public Long submittedAt() { return submittedAt; }
    public Long evaluatingSince() { return evaluatingSince; }
    public Feedback feedback() { return feedback; }

    public void editContent(String c) {
        if (status != AttemptStatus.DRAFT) throw new Errors.InvalidTransition("only drafts can be edited; start a new attempt to iterate");
        content = c == null ? "" : c;
    }
    public void submit(long now) { to(AttemptStatus.SUBMITTED); submittedAt = now; }
    public void startEvaluation(long now) { to(AttemptStatus.EVALUATING); evaluatingSince = now; error = ""; }
    public void complete(Feedback f) { to(AttemptStatus.EVALUATED); feedback = f; }
    public void fail(String why) { to(AttemptStatus.FAILED); error = why; }
    public boolean evaluationTimedOut(long now, long timeoutMs) {
        return status == AttemptStatus.EVALUATING && now - evaluatingSince > timeoutMs;
    }

    private void to(AttemptStatus next) {
        if (!status.canGoTo(next)) throw new Errors.InvalidTransition("cannot go from " + status.value() + " to " + next.value());
        status = next;
    }

    /** Shallow copy; Feedback is never mutated once attached. */
    public Attempt copy() {
        Attempt a = new Attempt(id, learnerId, problemId, format, number, createdAt);
        a.content = content; a.error = error; a.status = status;
        a.submittedAt = submittedAt; a.evaluatingSince = evaluatingSince; a.feedback = feedback;
        return a;
    }
}
