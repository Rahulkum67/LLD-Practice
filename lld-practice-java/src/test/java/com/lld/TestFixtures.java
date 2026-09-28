package com.lld;

import java.time.*;
import java.util.List;

final class TestFixtures {
    private TestFixtures() {}

    static final Catalog CATALOG = Catalog.defaultCatalog();
    static final String GOOD = """
        class ParkingLot
          - floors: List[Floor]
          + park(vehicle): Ticket
          + unpark(ticket): Money
        class Floor
          - spots: List[ParkingSpot]
        class ParkingSpot
        class Vehicle
        class Ticket
        interface PricingStrategy
          + calculate(ticket): Money
        class HourlyPricing implements PricingStrategy
        class FlatPricing implements PricingStrategy
        interface SpotAllocationStrategy
        class NearestSpotAllocation implements SpotAllocationStrategy
        class LowestFloorAllocation implements SpotAllocationStrategy
        """;

    static EvalContext ctx(String outline) {
        return new EvalContext(CATALOG.get("parking-lot"), new OutlineParser().parse(outline), outline);
    }

    static AttemptService service(FeedbackEngine engine, AttemptRepository repo, Clock clock, long timeoutMs) {
        return new AttemptService(repo, CATALOG, new ParserRegistry(new OutlineParser(), new JavaSkeletonParser()),
                engine, Runner.sync(), clock, timeoutMs);
    }

    static AttemptService service(FeedbackEngine engine) {
        return service(engine, new InMemoryAttemptRepository(), Clock.systemUTC(), 90_000);
    }

    static FeedbackEngine rubricOnly() { return new CompositeEvaluator(List.of(new RubricEvaluator())); }

    /** Clock the test can advance. */
    static final class MutableClock extends Clock {
        long now = 1_000_000;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
        public long millis() { return now; }
    }
}
