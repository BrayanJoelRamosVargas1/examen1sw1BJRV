package com.umlcase.infrastructure.interchange.xmi;

public class MultiplicityParser {

    public static class MultiplicityBounds {
        public final String lower;
        public final String upper;

        public MultiplicityBounds(String lower, String upper) {
            this.lower = lower;
            this.upper = upper;
        }
    }

    public MultiplicityBounds parse(String multiplicity) {
        if (multiplicity == null || multiplicity.trim().isEmpty()) {
            return new MultiplicityBounds("1", "1"); // default
        }
        
        String trimmed = multiplicity.trim();
        if (trimmed.equals("*")) {
            return new MultiplicityBounds("0", "*");
        }
        
        if (trimmed.contains("..")) {
            String[] parts = trimmed.split("\\.\\.");
            if (parts.length == 2) {
                return new MultiplicityBounds(parts[0], parts[1]);
            }
        }
        
        // Literal simple (e.g. "1")
        return new MultiplicityBounds(trimmed, trimmed);
    }
}
