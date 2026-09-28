package com.lld;

import static com.lld.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RubricEvaluatorTest {
    private final RubricEvaluator rubric = new RubricEvaluator();

    @Test void goodDesignScoresHighWithNoIssues() {
        EvalResult r = rubric.evaluate(ctx(GOOD));
        assertEquals(100, r.scores().get("coverage"));
        assertEquals(100, r.scores().get("abstraction"));
        assertTrue(r.findings().stream().noneMatch(f -> f.severity().equals("issue")));
    }

    @Test void differentValidNamingIsStillCovered() {
        String alt = GOOD.replace("ParkingSpot", "Slot").replace("Ticket", "Receipt");
        assertEquals(100, rubric.evaluate(ctx(alt)).scores().get("coverage"));
    }

    @Test void missingConceptAndSingleImplementationAreReported() {
        EvalResult r = rubric.evaluate(ctx("class ParkingLot\nclass Vehicle\ninterface PricingStrategy\nclass A implements PricingStrategy"));
        Set<String> ids = r.findings().stream().map(Finding::id).collect(Collectors.toSet());
        assertTrue(ids.contains("coverage:Ticket"));
        assertEquals("warning", r.findings().stream().filter(f -> f.id().equals("abstraction:pricing")).findFirst().get().severity());
        assertTrue(r.scores().get("coverage") < 100);
    }

    @Test void pricingAbstractionDoesNotSatisfyAllocationPoint() {
        String pricingOnly = GOOD.substring(0, GOOD.indexOf("interface SpotAllocationStrategy"));
        assertTrue(rubric.evaluate(ctx(pricingOnly)).findings().stream().anyMatch(f -> f.id().equals("abstraction:allocation") && f.severity().equals("warning")));
    }

    @Test void godClassAndTinyDesignAreFlagged() {
        StringBuilder sb = new StringBuilder("class ParkingLot\n");
        for (int i = 0; i < 12; i++) sb.append("  + m").append(i).append("()\n");
        Set<String> ids = rubric.evaluate(ctx(sb.toString())).findings().stream().map(Finding::id).collect(Collectors.toSet());
        assertTrue(ids.contains("structure:god:ParkingLot"));
        assertTrue(ids.contains("structure:tiny"));
    }
}
