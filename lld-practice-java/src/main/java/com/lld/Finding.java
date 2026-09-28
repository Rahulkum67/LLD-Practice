package com.lld;

/** id is stable across attempts so two attempts can be diffed. */
public record Finding(String id, String category, String severity, String message, String suggestion, String source) {}
