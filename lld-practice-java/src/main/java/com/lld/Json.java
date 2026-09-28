package com.lld;

import java.util.*;

/** Minimal JSON reader/writer so the project has zero runtime dependencies. */
public final class Json {
    private Json() {}

    public static String write(Object o) { StringBuilder sb = new StringBuilder(); w(o, sb); return sb.toString(); }

    private static void w(Object o, StringBuilder sb) {
        if (o == null) sb.append("null");
        else if (o instanceof String s) str(s, sb);
        else if (o instanceof Number || o instanceof Boolean) sb.append(o);
        else if (o instanceof Enum<?> e) str(e.name().toLowerCase(), sb);
        else if (o instanceof Map<?, ?> m) {
            sb.append('{'); boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) { if (!first) sb.append(','); first = false; str(String.valueOf(e.getKey()), sb); sb.append(':'); w(e.getValue(), sb); }
            sb.append('}');
        } else if (o instanceof Collection<?> c) {
            sb.append('['); boolean first = true;
            for (Object x : c) { if (!first) sb.append(','); first = false; w(x, sb); }
            sb.append(']');
        } else throw new IllegalArgumentException("cannot serialize " + o.getClass());
    }

    private static void str(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\""); case '\\' -> sb.append("\\\\"); case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r"); case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        sb.append('"');
    }

    /** Returns Map / List / String / Double / Boolean / null. Throws IllegalArgumentException on bad input. */
    public static Object parse(String s) {
        Reader r = new Reader(s);
        Object v = r.value();
        r.ws();
        if (r.i != s.length()) throw new IllegalArgumentException("trailing characters at " + r.i);
        return v;
    }

    private static final class Reader {
        final String s; int i;
        Reader(String s) { this.s = s; }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        char peek() { if (i >= s.length()) throw new IllegalArgumentException("unexpected end"); return s.charAt(i); }
        void expect(char c) { ws(); if (peek() != c) throw new IllegalArgumentException("expected '" + c + "' at " + i); i++; }

        Object value() {
            ws();
            char c = peek();
            if (c == '{') return obj();
            if (c == '[') return arr();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return true; }
            if (s.startsWith("false", i)) { i += 5; return false; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (st == i) throw new IllegalArgumentException("unexpected '" + c + "' at " + i);
            return Double.parseDouble(s.substring(st, i));
        }
        Map<String, Object> obj() {
            Map<String, Object> m = new LinkedHashMap<>();
            expect('{'); ws();
            if (peek() == '}') { i++; return m; }
            do { ws(); String k = string(); expect(':'); m.put(k, value()); ws(); } while (s.charAt(i++) == ',');
            if (s.charAt(i - 1) != '}') throw new IllegalArgumentException("expected '}' at " + (i - 1));
            return m;
        }
        List<Object> arr() {
            List<Object> l = new ArrayList<>();
            expect('['); ws();
            if (peek() == ']') { i++; return l; }
            do { l.add(value()); ws(); } while (s.charAt(i++) == ',');
            if (s.charAt(i - 1) != ']') throw new IllegalArgumentException("expected ']' at " + (i - 1));
            return l;
        }
        String string() {
            if (peek() != '"') throw new IllegalArgumentException("expected string at " + i);
            i++;
            StringBuilder sb = new StringBuilder();
            while (peek() != '"') {
                char c = s.charAt(i++);
                if (c != '\\') { sb.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n'); case 't' -> sb.append('\t'); case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b'); case 'f' -> sb.append('\f');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(e);
                }
            }
            i++;
            return sb.toString();
        }
    }
}
