package com.lld;

/** One evaluation approach. Add an approach = add an implementation and register it. */
public interface Evaluator {
    String name();
    EvalResult evaluate(EvalContext ctx);
}
