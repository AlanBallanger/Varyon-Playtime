package com.varyon.playtime.config;

public class Milestone {
    public String time;
    public String message;

    public Milestone() {}

    public Milestone(String time, String message) {
        this.time = time;
        this.message = message;
    }

    public long toMillis() {
        if (time == null || time.isBlank()) {
            return -1;
        }
        try {
            String num = time.replaceAll("[^0-9]", "");
            String unit = time.replaceAll("[0-9]", "").toLowerCase().trim();
            if (num.isEmpty()) {
                return -1;
            }
            long val = Long.parseLong(num);
            return switch (unit) {
                case "s" -> val * 1_000L;
                case "m" -> val * 60_000L;
                case "h" -> val * 3_600_000L;
                case "j", "d" -> val * 86_400_000L;
                default -> -1;
            };
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String uniqueId() {
        return "milestone_" + time.replaceAll("[^a-zA-Z0-9]", "");
    }
}
