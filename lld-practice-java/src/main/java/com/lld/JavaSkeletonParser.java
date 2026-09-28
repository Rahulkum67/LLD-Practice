package com.lld;

import java.util.*;
import java.util.regex.*;

/**
 * Reads Java source (classes, interfaces, enums, records) with regex + brace matching.
 * Deliberately shallow: it extracts structure (types, bases, fields, method signatures), not semantics,
 * so method bodies may be empty or omitted.
 */
public class JavaSkeletonParser implements SubmissionParser {
    private static final String MODS = "(?:public|protected|private|static|final|abstract|sealed|non-sealed|synchronized|native|default|strictfp)";
    private static final Pattern TYPE = Pattern.compile(
        "((?:" + MODS + "\\s+)*)\\b(class|interface|enum|record)\\s+(\\w+)(?:\\s*<[^{]*?>)?(?:\\s*\\(([^)]*)\\))?([^{;]*)\\{");
    private static final Pattern METHOD = Pattern.compile("^((?:" + MODS + "\\s+)*)(?:<[^>]+>\\s*)?([\\w<>\\[\\],.? ]+?)\\s+(\\w+)\\s*\\((.*)\\)\\s*(?:throws [\\w., ]+)?$", Pattern.DOTALL);
    private static final Pattern FIELD = Pattern.compile("^((?:" + MODS + "\\s+)*)([\\w<>\\[\\],.? ]+?)\\s+(\\w+)$");
    public String format() { return "java"; }

    public DesignModel parse(String source) {
        String src = source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ")
                           .replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"", "\"\"").replaceAll("@\\w+(?:\\([^)]*\\))?", " ");
        int depth = 0;
        for (char c : src.toCharArray()) { if (c == '{') depth++; if (c == '}' && --depth < 0) break; }
        if (depth != 0) throw new Errors.Parse("unbalanced braces");
        List<ClassSpec> out = new ArrayList<>();
        Matcher m = TYPE.matcher(src);
        while (m.find()) {
            String mods = m.group(1), kw = m.group(2), name = m.group(3);
            String kind = kw.equals("interface") ? "interface" : kw.equals("enum") ? "enum"
                        : mods.contains("abstract") ? "abstract" : "class";
            int open = m.end() - 1, close = matching(src, open);
            List<String> methods = new ArrayList<>();
            List<ClassSpec.Attr> attrs = new ArrayList<>();
            if (m.group(4) != null) for (String p : m.group(4).split(",")) {          // record components
                String[] t = p.trim().split("\\s+(?=\\w+$)");
                if (t.length == 2) attrs.add(new ClassSpec.Attr(t[1], t[0]));
            }
            members(src.substring(open + 1, close), name, kw.equals("interface"), methods, attrs);
            out.add(new ClassSpec(name, kind, bases(m.group(5)), methods, attrs));
        }
        if (out.isEmpty()) throw new Errors.Parse("no classes found");
        return new DesignModel(out);
    }

    private static int matching(String s, int open) {
        int d = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '{') d++;
            else if (s.charAt(i) == '}' && --d == 0) return i;
        }
        throw new Errors.Parse("unbalanced braces");
    }

    private static List<String> bases(String rest) {
        String r = rest == null ? "" : rest;
        String prev;
        do { prev = r; r = r.replaceAll("<[^<>]*>", ""); } while (!r.equals(prev));
        List<String> out = new ArrayList<>();
        Matcher e = Pattern.compile("(?:extends|implements)\\s+([\\w\\s,.]+?)(?=\\s+(?:implements|permits)\\b|$)").matcher(r.trim());
        while (e.find()) for (String b : e.group(1).split(",")) {
            String n = b.trim().replaceAll(".*\\.", "");
            if (!n.isEmpty()) out.add(n);
        }
        return out;
    }

    /** Splits a class body into top-level members, skipping nested blocks. */
    private static void members(String body, String cls, boolean iface, List<String> methods, List<ClassSpec.Attr> attrs) {
        int depth = 0;
        StringBuilder head = new StringBuilder();
        for (char ch : body.toCharArray()) {
            if (depth > 0) { if (ch == '{') depth++; else if (ch == '}') depth--; continue; }
            if (ch == ';' || ch == '{') {
                member(head.toString().trim().replaceAll("\\s+", " "), cls, iface, methods, attrs);
                head.setLength(0);
                if (ch == '{') depth = 1;
            } else head.append(ch);
        }
    }

    private static void member(String head, String cls, boolean iface, List<String> methods, List<ClassSpec.Attr> attrs) {
        if (head.isEmpty() || head.matches(".*\\b(class|interface|enum|record)\\s+\\w+.*")) return;   // nested type: parsed separately
        int eq = head.indexOf('='), paren = head.indexOf('(');
        if (eq >= 0 && (paren < 0 || eq < paren)) head = head.substring(0, eq).trim();
        Matcher m = METHOD.matcher(head);
        if (m.matches()) {
            if (m.group(1).contains("private")) return;
            String ret = m.group(2).trim();
            methods.add(m.group(3) + "(" + m.group(4).trim() + "): " + ret);
            return;
        }
        Matcher f = FIELD.matcher(head);
        if (f.matches()) attrs.add(new ClassSpec.Attr(f.group(3), f.group(2).trim()));
    }
}
