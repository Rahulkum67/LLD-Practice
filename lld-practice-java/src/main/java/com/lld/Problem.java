package com.lld;

import java.util.List;

/** A practice problem. The rubric checks capabilities (via aliases), not a reference class diagram. */
public record Problem(String id, String title, String summary, String difficulty, List<String> requirements,
                      List<Concept> concepts, List<VariationPoint> variationPoints, List<Behaviour> behaviours) {

    public record Concept(String label, String why, List<String> aliases) {
        public static Concept of(String label, String why, String... aliases) { return new Concept(label, why, List.of(aliases)); }
    }

    public record VariationPoint(String id, String label, String hint, List<String> aliases) {
        public static VariationPoint of(String id, String label, String hint, String... aliases) {
            return new VariationPoint(id, label, hint, List.of(aliases));
        }
    }

    public record Behaviour(String label, List<String> aliases) {
        public static Behaviour of(String label, String... aliases) { return new Behaviour(label, List.of(aliases)); }
    }
}
