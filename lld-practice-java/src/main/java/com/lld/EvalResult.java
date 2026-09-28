package com.lld;

import java.util.*;

public record EvalResult(List<Finding> findings, Map<String, Integer> scores) {
    public static EvalResult ofFindings(List<Finding> f) { return new EvalResult(f, Map.of()); }
}
