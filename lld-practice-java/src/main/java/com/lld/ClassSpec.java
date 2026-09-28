package com.lld;

import java.util.List;

/** One class/interface in a submitted design, independent of the submission format. */
public record ClassSpec(String name, String kind, List<String> bases, List<String> methods, List<Attr> attributes) {
    public record Attr(String name, String type) {}

    public boolean isAbstraction() { return kind.equals("interface") || kind.equals("abstract"); }

    public List<String> methodNames() { return methods.stream().map(m -> m.split("\\(")[0].trim()).toList(); }
}
