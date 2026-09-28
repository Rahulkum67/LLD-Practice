package com.lld;

public enum AttemptStatus {
    DRAFT, SUBMITTED, EVALUATING, EVALUATED, FAILED;

    public String value() { return name().toLowerCase(); }

    public boolean canGoTo(AttemptStatus to) {
        return switch (this) {
            case DRAFT -> to == SUBMITTED;
            case SUBMITTED -> to == EVALUATING;
            case EVALUATING -> to == EVALUATED || to == FAILED;
            case FAILED -> to == EVALUATING;      // retry
            case EVALUATED -> false;
        };
    }
}
