package com.lld;

public enum Severity {
    ISSUE, WARNING, STRENGTH;
    public String value() { return name().toLowerCase(); }
    public static Severity parseOr(String s, Severity fallback) {
        for (Severity v : values()) if (v.value().equals(s)) return v;
        return fallback;
    }
}
