package com.lld;

import java.util.*;

public class Feedback {
    public final List<Finding> findings = new ArrayList<>();
    public final Map<String, Integer> scores = new LinkedHashMap<>();
    public final List<String> notes = new ArrayList<>();
    public String summary = "";
    public boolean degraded;      // an evaluator failed; feedback is partial
}
