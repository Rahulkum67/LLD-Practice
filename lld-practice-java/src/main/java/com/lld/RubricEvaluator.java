package com.lld;

import java.util.*;

/**
 * Deterministic: same design in, same findings out. Verifies only what is structurally checkable and
 * matches by alias, so different-but-valid namings pass. Nuance is left to the LLM evaluator.
 */
public class RubricEvaluator implements Evaluator {
    static final int GOD_CLASS_METHODS = 10;
    public String name() { return "rubric"; }

    public EvalResult evaluate(EvalContext ctx) {
        DesignModel m = ctx.model();
        Problem p = ctx.problem();
        List<Finding> f = new ArrayList<>();

        int found = 0;                                                  // 1. core concepts
        for (Problem.Concept c : p.concepts()) {
            if (m.find(c.aliases()).isPresent()) found++;
            else f.add(finding("coverage:" + c.label(), "coverage", Severity.ISSUE, "No class represents '" + c.label() + "'.", "Consider adding one: " + c.why() + "."));
        }
        if (found == p.concepts().size()) f.add(finding("coverage:all", "coverage", Severity.STRENGTH, "All core domain concepts are modelled.", ""));

        List<Integer> pts = new ArrayList<>();                          // 2. extension points
        for (Problem.VariationPoint v : p.variationPoints()) {
            int impls = m.classes().stream().filter(ClassSpec::isAbstraction)
                    .filter(c -> v.aliases().stream().anyMatch(a -> DesignModel.norm(c.name()).contains(DesignModel.norm(a))))
                    .mapToInt(c -> m.implementers(c.name()).size()).max().orElse(0);
            String id = "abstraction:" + v.id();
            if (impls >= 2) { pts.add(100); f.add(finding(id, "abstraction", Severity.STRENGTH, "'" + v.label() + "' is an extension point with " + impls + " implementations.", "")); }
            else if (impls == 1) { pts.add(50); f.add(finding(id, "abstraction", Severity.WARNING, "'" + v.label() + "' has an abstraction but only one implementation.", "Add a second variant (even a sketched one) to show the seam is real.")); }
            else { pts.add(0); f.add(finding(id, "abstraction", Severity.WARNING, "No abstraction found for '" + v.label() + "'.", v.hint() + " If you chose a simpler approach on purpose, note the trade-off.")); }
        }
        int abstraction = (int) Math.round(pts.stream().mapToInt(Integer::intValue).average().orElse(100));

        int ok = 0;                                                     // 3. behaviours
        for (Problem.Behaviour b : p.behaviours()) {
            if (m.hasMethod(b.aliases())) ok++;
            else f.add(finding("behaviour:" + b.label(), "behaviour", Severity.WARNING, "No method looks like '" + b.label() + "'.", "Where does this responsibility live?"));
        }

        int penalty = 0;                                                // 4. structure
        for (ClassSpec c : m.classes()) {
            if (c.methods().size() > GOD_CLASS_METHODS) {
                penalty += 25;
                f.add(finding("structure:god:" + c.name(), "structure", Severity.WARNING, "'" + c.name() + "' has " + c.methods().size() + " public methods and likely too many responsibilities.", "Group related methods and extract collaborators."));
            }
            if (m.size() > 1 && c.bases().isEmpty() && c.attributes().isEmpty() && c.methods().isEmpty() && !m.isReferenced(c.name())) {
                penalty += 10;
                f.add(finding("structure:orphan:" + c.name(), "structure", Severity.WARNING, "'" + c.name() + "' is empty and unrelated to other classes.", "Add responsibilities or remove it."));
            }
        }
        if (m.size() < 3) {
            penalty += 30;
            f.add(finding("structure:tiny", "structure", Severity.ISSUE, "Very few classes; responsibilities are probably not separated.", "Split by responsibility: who owns state, who applies rules?"));
        }
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("coverage", 100 * found / p.concepts().size());
        scores.put("abstraction", abstraction);
        scores.put("behaviour", p.behaviours().isEmpty() ? 100 : 100 * ok / p.behaviours().size());
        scores.put("structure", Math.max(0, 100 - penalty));
        return new EvalResult(f, scores);
    }

    private Finding finding(String id, String cat, Severity s, String msg, String suggestion) {
        return new Finding(id, cat, s.value(), msg, suggestion, name());
    }
}
