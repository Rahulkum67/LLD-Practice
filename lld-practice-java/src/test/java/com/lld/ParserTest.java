package com.lld;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ParserTest {
    @Test void outlineBuildsModelWithImplementers() {
        DesignModel m = new OutlineParser().parse(TestFixtures.GOOD);
        assertEquals(2, m.implementers("PricingStrategy").size());
        assertTrue(m.classes().stream().filter(c -> c.name().equals("PricingStrategy")).findFirst().get().isAbstraction());
    }

    @Test void outlineErrorsReportLineNumbers() {
        assertTrue(assertThrows(Errors.Parse.class, () -> new OutlineParser().parse("class A\nwhat is this")).getMessage().contains("line 2"));
        assertThrows(Errors.Parse.class, () -> new OutlineParser().parse("+ orphan(): x"));
        assertThrows(Errors.Parse.class, () -> new OutlineParser().parse("// only a comment"));
    }

    @Test void javaSourceIsParsedIntoStructure() {
        String src = """
            package demo;
            import java.util.*;
            /** pricing */
            public interface PricingStrategy { Money calculate(Ticket t); }
            class HourlyPricing implements PricingStrategy {
                @Override public Money calculate(Ticket t) { return new Money(1); }
            }
            public abstract class Vehicle { private String plate; abstract int size(); }
            class ParkingLot {
                private final List<Floor> floors = new ArrayList<>();
                private PricingStrategy pricing;
                public Ticket park(Vehicle v) { if (v == null) { throw new IllegalArgumentException("x;{"); } return null; }
                private void helper() {}
                ParkingLot(PricingStrategy p) { this.pricing = p; }
            }
            record Ticket(String id, Vehicle vehicle) {}
            enum Size { SMALL, LARGE }
            """;
        DesignModel m = new JavaSkeletonParser().parse(src);
        ClassSpec lot = m.classes().stream().filter(c -> c.name().equals("ParkingLot")).findFirst().get();
        assertEquals(1, m.implementers("PricingStrategy").size());
        assertEquals("interface", m.classes().stream().filter(c -> c.name().equals("PricingStrategy")).findFirst().get().kind());
        assertEquals("abstract", m.classes().stream().filter(c -> c.name().equals("Vehicle")).findFirst().get().kind());
        assertEquals(java.util.List.of("park"), lot.methodNames());          // private helper and constructor excluded
        assertEquals(2, lot.attributes().size());
        assertTrue(m.isReferenced("Vehicle"));                              // via Ticket record component and park()
        assertEquals("enum", m.classes().stream().filter(c -> c.name().equals("Size")).findFirst().get().kind());
    }

    @Test void javaParserRejectsBrokenInput() {
        assertThrows(Errors.Parse.class, () -> new JavaSkeletonParser().parse("class A { void f() { }"));
        assertThrows(Errors.Parse.class, () -> new JavaSkeletonParser().parse("just prose, no types"));
    }
}
