package com.lld;

import java.util.*;
import java.util.regex.*;

/**
 * Small text DSL:
 *   interface PricingStrategy
 *     + calculate(ticket): Money
 *   class HourlyPricing implements PricingStrategy
 *   class ParkingLot
 *     - floors: List[Floor]
 *     + park(vehicle): Ticket
 */
public class OutlineParser implements SubmissionParser {
    private static final Pattern HEADER = Pattern.compile("^(abstract\\s+class|class|interface|enum)\\s+(\\w+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    public String format() { return "outline"; }

    private static final class Builder {
        String name, kind; List<String> bases = new ArrayList<>(), methods = new ArrayList<>();
        List<ClassSpec.Attr> attrs = new ArrayList<>();
        ClassSpec build() { return new ClassSpec(name, kind, bases, methods, attrs); }
    }

    public DesignModel parse(String content) {
        List<ClassSpec> out = new ArrayList<>();
        Builder cur = null;
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim(), where = "line " + (i + 1) + ": ";
            if (line.isEmpty() || line.startsWith("//")) continue;
            Matcher h = HEADER.matcher(line);
            if (h.matches()) {
                if (cur != null) out.add(cur.build());
                String rest = h.group(3).trim();
                if (!rest.isEmpty() && !rest.matches("(?i)^(extends|implements)\\b.*"))
                    throw new Errors.Parse(where + "unexpected text after class name: '" + rest + "'");
                cur = new Builder();
                cur.name = h.group(2);
                cur.kind = h.group(1).toLowerCase().split("\\s+")[0];
                for (String b : rest.replaceAll("(?i)\\b(extends|implements)\\b", ",").split(","))
                    if (!b.isBlank()) cur.bases.add(b.trim());
            } else if ("+-#~".indexOf(line.charAt(0)) >= 0) {
                if (cur == null) throw new Errors.Parse(where + "member declared before any class");
                String body = line.substring(1).trim();
                if (body.contains("(")) cur.methods.add(body);
                else {
                    int c = body.indexOf(':');
                    cur.attrs.add(new ClassSpec.Attr(c < 0 ? body : body.substring(0, c).trim(), c < 0 ? "" : body.substring(c + 1).trim()));
                }
            } else {
                throw new Errors.Parse(where + "expected 'class X', 'interface X' or a member starting with + - #");
            }
        }
        if (cur != null) out.add(cur.build());
        if (out.isEmpty()) throw new Errors.Parse("no classes found");
        return new DesignModel(out);
    }
}
