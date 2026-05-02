package com.varyon.playtime.api;

import com.varyon.playtime.Playtime;
import com.varyon.playtime.PlaytimeService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlaytimeAPI {

    private static PlaytimeAPI instance;
    private final PlaytimeService service;

    private PlaytimeAPI() {
        this.service = Playtime.get().getService();
    }

    public static PlaytimeAPI get() {
        if (instance == null) {
            instance = new PlaytimeAPI();
        }
        return instance;
    }

    public long getTotalPlaytime(UUID uuid) {
        return service.getTotalPlaytime(uuid.toString());
    }

    public long getPlaytime(UUID uuid, String period) {
        return service.getPlaytime(uuid.toString(), period);
    }

    public int getRank(UUID uuid, String period) {
        return service.getRank(uuid.toString(), period);
    }

    public Map<String, Long> getTopPlayers(String period) {
        return service.getTopPlayers(period);
    }

    public String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }
}
