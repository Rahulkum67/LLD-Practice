package com.lld;

import java.util.*;
import java.util.regex.Pattern;

public final class DesignModel {
    private final Map<String, ClassSpec> classes = new LinkedHashMap<>();

    public DesignModel(Collection<ClassSpec> specs) { specs.forEach(c -> classes.put(c.name(), c)); }

    public Collection<ClassSpec> classes() { return classes.values(); }
    public int size() { return classes.size(); }

    public static String norm(String s) { return s.toLowerCase().replaceAll("[^a-z0-9]", ""); }

    public List<ClassSpec> implementers(String name) {
        return classes.values().stream().filter(c -> c.bases().contains(name)).toList();
    }

    public boolean isReferenced(String name) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        for (ClassSpec c : classes.values()) {
            if (c.name().equals(name)) continue;
            if (c.bases().contains(name)
                    || c.attributes().stream().anyMatch(a -> p.matcher(a.type()).find())
                    || c.methods().stream().anyMatch(m -> p.matcher(m).find())) return true;
        }
        return false;
    }

    /** Exact normalized match first, then substring match for aliases of 4+ chars. */
    public Optional<ClassSpec> find(List<String> aliases) {
        List<String> al = aliases.stream().map(DesignModel::norm).toList();
        return classes.values().stream().filter(c -> al.contains(norm(c.name()))).findFirst()
                .or(() -> classes.values().stream()
                        .filter(c -> al.stream().anyMatch(a -> a.length() >= 4 && norm(c.name()).contains(a))).findFirst());
    }

    public boolean hasMethod(List<String> aliases) {
        List<String> al = aliases.stream().map(DesignModel::norm).toList();
        return classes.values().stream().flatMap(c -> c.methodNames().stream())
                .anyMatch(m -> al.stream().anyMatch(a -> norm(m).contains(a)));
    }
}
