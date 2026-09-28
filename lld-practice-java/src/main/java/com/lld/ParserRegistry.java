package com.lld;

import java.util.*;

public class ParserRegistry {
    private final Map<String, SubmissionParser> parsers = new TreeMap<>();

    public ParserRegistry(SubmissionParser... ps) { for (SubmissionParser p : ps) parsers.put(p.format(), p); }

    public SubmissionParser get(String format) {
        SubmissionParser p = parsers.get(format);
        if (p == null) throw new Errors.Validation("unsupported format '" + format + "'; use one of " + parsers.keySet());
        return p;
    }
    public List<String> formats() { return List.copyOf(parsers.keySet()); }
}
