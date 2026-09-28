package com.lld;

/** Domain exceptions. The web layer maps each to an HTTP status. */
public final class Errors {
    private Errors() {}
    public static class PracticeException extends RuntimeException {
        public PracticeException(String m) { super(m); }
        public PracticeException(String m, Throwable c) { super(m, c); }
    }
    public static class NotFound extends PracticeException { public NotFound(String m) { super(m); } }
    public static class Validation extends PracticeException { public Validation(String m) { super(m); } }
    public static class Parse extends Validation { public Parse(String m) { super(m); } }
    public static class InvalidTransition extends PracticeException { public InvalidTransition(String m) { super(m); } }
    public static class Evaluator extends PracticeException {
        public Evaluator(String m) { super(m); }
        public Evaluator(String m, Throwable c) { super(m, c); }
    }
}
