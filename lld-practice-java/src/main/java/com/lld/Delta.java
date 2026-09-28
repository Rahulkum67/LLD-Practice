package com.lld;

import java.util.*;

/** Difference between an attempt and the previous evaluated one (strengths excluded). */
public record Delta(int previousAttempt, List<String> resolved, List<String> added, List<String> persisting, Map<String, Integer> scoreChanges) {}
