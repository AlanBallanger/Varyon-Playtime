package com.varyon.playtime.config;

import java.util.ArrayList;
import java.util.List;

public class Reward {
    public String id;
    public String period;
    public long timeRequirement;
    public List<String> commands;
    public String broadcastMessage;

    public Reward() {
        this.commands = new ArrayList<>();
    }

    public Reward(String id, String period, long timeRequirement, List<String> commands, String broadcastMessage) {
        this.id = id;
        this.period = period;
        this.timeRequirement = timeRequirement;
        this.commands = commands;
        this.broadcastMessage = broadcastMessage;
    }
}
