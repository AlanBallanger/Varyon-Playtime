package com.varyon.playtime.api;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.varyon.playtime.Playtime;
import com.varyon.playtime.PlaytimeService;
import com.varyon.playtime.config.PlaytimeConfig;
import com.varyon.playtime.config.Reward;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    public long getFirstLogin(UUID uuid) {
        return service.getFirstLogin(uuid.toString());
    }

    public long getLastLogin(UUID uuid) {
        return service.getLastLogin(uuid.toString());
    }

    public long[] getDailyRewardData(UUID uuid) {
        List<Reward> daily = orderedUiDailyRewards();
        long[] result = new long[daily.size() * 2];
        for (int i = 0; i < daily.size(); i++) {
            Reward r = daily.get(i);
            result[i * 2]     = r.timeRequirement;
            result[i * 2 + 1] = Playtime.get().getDatabaseManager().hasClaimedReward(uuid.toString(), r) ? 1L : 0L;
        }
        return result;
    }

    public String[] getDailyRewardIds() {
        return orderedUiDailyRewards().stream().map(r -> r.id).toArray(String[]::new);
    }

    private List<Reward> orderedUiDailyRewards() {
        PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
        return config.rewards.stream()
                .filter(r -> rewardIsUiDaily(r, config))
                .sorted(Comparator.comparingLong(r -> r.timeRequirement))
                .collect(Collectors.toList());
    }

    private static boolean rewardIsUiDaily(Reward r, PlaytimeConfig config) {
        if (r == null || r.id == null || r.id.isBlank()) {
            return false;
        }
        String key = config.resolvePeriodKey(r.period);
        if (key == null && r.period != null) {
            key = r.period.trim();
        }
        return "daily".equalsIgnoreCase(key != null ? key : "");
    }

    @SuppressWarnings("deprecation")
    public boolean claimReward(UUID uuid, String rewardId) {
        if (uuid == null || rewardId == null || rewardId.isBlank()) {
            return false;
        }
        PlayerRef player = findOnlinePlayer(uuid);
        if (player == null) {
            return false;
        }
        Reward r = findRewardById(rewardId.trim());
        if (r == null) {
            return false;
        }
        return Playtime.get().getRewardManager().tryManualClaim(player, r);
    }

    @SuppressWarnings("deprecation")
    public int claimAllEligibleDailyRewards(UUID uuid) {
        PlayerRef player = findOnlinePlayer(uuid);
        if (player == null) {
            return 0;
        }
        int n = 0;
        for (String id : getDailyRewardIds()) {
            Reward r = findRewardById(id);
            if (r != null && Playtime.get().getRewardManager().tryManualClaim(player, r)) {
                n++;
            }
        }
        return n;
    }

    private Reward findRewardById(String id) {
        for (Reward r : Playtime.get().getConfigManager().getConfig().rewards) {
            if (r != null && id.equals(r.id)) {
                return r;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static PlayerRef findOnlinePlayer(UUID uuid) {
        for (PlayerRef p : Universe.get().getPlayers()) {
            if (uuid.equals(p.getUuid())) {
                return p;
            }
        }
        return null;
    }
}
