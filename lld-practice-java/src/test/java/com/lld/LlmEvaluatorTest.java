package com.lld;

import static com.lld.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LlmEvaluatorTest {
    static final String OK = "{\"findings\":[{\"category\":\"extensibility\",\"severity\":\"warning\",\"title\":\"Lot knows fees\",\"message\":\"ParkingLot computes fees.\",\"suggestion\":\"Delegate.\"}]}";

    static LlmClient replying(String text) { return (s, p) -> text; }
    static LlmClient failing(RuntimeException e) { return (s, p) -> { throw e; }; }
    static Evaluator broken() {
        return new Evaluator() {
            public String name() { return "broken"; }
            public EvalResult evaluate(EvalContext c) { throw new IllegalStateException("down"); }
        };
    }

    @Test void parsesFencedJson() {
        List<Finding> f = LlmEvaluator.parseFindings("```json\n" + OK + "\n```");
        assertEquals("llm", f.get(0).source());
        assertEquals("extensibility", f.get(0).category());
    }

    @Test void invalidSeverityDefaultsAndFindingsAreCapped() {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 9; i++) items.append(i > 0 ? "," : "").append("{\"message\":\"m").append(i).append("\",\"severity\":\"catastrophic\"}");
        List<Finding> f = LlmEvaluator.parseFindings("{\"findings\":[" + items + "]}");
        assertEquals(5, f.size());
        assertEquals("warning", f.get(0).severity());
    }

    @Test void malformedOutputIsRejected() {
        for (String bad : List.of("not json", "{\"findings\": \"x\"}", "{\"findings\": []}", "{\"findings\":[{\"title\":\"no message\"}]}", "[1,2]"))
            assertThrows(Errors.Evaluator.class, () -> LlmEvaluator.parseFindings(bad), bad);
    }

    @Test void compositeDegradesWhenLlmFails() {
        Feedback fb = new CompositeEvaluator(List.of(new RubricEvaluator(), new LlmEvaluator(failing(new Errors.Evaluator("timeout"))))).evaluate(ctx(GOOD));
        assertTrue(fb.degraded);
        assertFalse(fb.findings.isEmpty());
        assertTrue(fb.notes.get(0).contains("timeout"));
    }

    @Test void compositeMergesLlmFindings() {
        Feedback fb = new CompositeEvaluator(List.of(new RubricEvaluator(), new LlmEvaluator(replying(OK)))).evaluate(ctx(GOOD));
        assertFalse(fb.degraded);
        assertTrue(fb.findings.stream().anyMatch(f -> f.source().equals("llm")));
    }

    @Test void allEvaluatorsFailingThrows() {
        assertThrows(Errors.Evaluator.class, () -> new CompositeEvaluator(List.of(broken())).evaluate(ctx(GOOD)));
    }

    @Test void jsonRoundTrip() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", "line\n\"quoted\"\\"); m.put("b", List.of(1, true)); m.put("c", null);
        Object back = Json.parse(Json.write(m));
        assertEquals("line\n\"quoted\"\\", ((Map<?, ?>) back).get("a"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":"));
    }
}
