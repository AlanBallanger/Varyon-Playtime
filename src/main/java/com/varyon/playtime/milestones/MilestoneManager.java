package com.varyon.playtime.milestones;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.varyon.playtime.Playtime;
import com.varyon.playtime.api.PlaytimeAPI;
import com.varyon.playtime.config.Milestone;
import com.varyon.playtime.config.PlaytimeConfig;
import com.varyon.playtime.config.PlaytimeConfig.MilestoneSettings;
import com.varyon.playtime.database.DatabaseManager;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MilestoneManager {

    private final DatabaseManager db;
    private final Logger logger = LoggerFactory.getLogger("Playtime");

    public MilestoneManager(DatabaseManager db) {
        this.db = db;
    }

    @SuppressWarnings("deprecation")
    public void checkMilestones() {
        PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
        if (config.milestoneSettings == null || !config.milestoneSettings.enabled) {
            return;
        }
        if (config.milestones == null || config.milestones.isEmpty()) {
            return;
        }

        List<PlayerRef> players = new ArrayList<>(Universe.get().getPlayers());
        for (PlayerRef player : players) {
            processPlayer(player, config);
        }
    }

    @SuppressWarnings("deprecation")
    private void processPlayer(PlayerRef player, PlaytimeConfig config) {
        String uuid = player.getUuid().toString();
        long totalPlaytime = PlaytimeAPI.get().getPlaytime(player.getUuid(), "all");

        for (Milestone milestone : config.milestones) {
            long required = milestone.toMillis();
            if (required <= 0) {
                continue;
            }
            String milestoneId = milestone.uniqueId();
            if (totalPlaytime >= required && !db.hasMilestoneTriggered(uuid, milestoneId)) {
                triggerMilestone(player, milestone, required, config);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void triggerMilestone(PlayerRef player, Milestone milestone, long requiredMs, PlaytimeConfig config) {
        String uuid = player.getUuid().toString();
        String milestoneId = milestone.uniqueId();
        db.logMilestone(uuid, milestoneId);

        String username = player.getUsername();
        String timeFormatted = PlaytimeAPI.get().formatTime(requiredMs);

        String template;
        if (milestone.message != null && !milestone.message.isBlank()) {
            template = milestone.message;
        } else {
            template = resolveTierTemplate(requiredMs, config.milestoneSettings);
        }

        String msg = template
                .replace("%player%", username)
                .replace("%time%", timeFormatted);

        logger.info("[Milestone] " + username + " a atteint " + timeFormatted + " (" + milestoneId + ")");
        Universe.get().sendMessage(color(msg));
    }

    private String resolveTierTemplate(long requiredMs, MilestoneSettings settings) {
        long bronzeMax = parseTime(settings.tierBronzeMax);
        long silverMax = parseTime(settings.tierSilverMax);
        long goldMax   = parseTime(settings.tierGoldMax);

        if (bronzeMax > 0 && requiredMs < bronzeMax) {
            return settings.broadcastBronze;
        } else if (silverMax > 0 && requiredMs < silverMax) {
            return settings.broadcastSilver;
        } else if (goldMax > 0 && requiredMs < goldMax) {
            return settings.broadcastGold;
        } else {
            return settings.broadcastDiamond;
        }
    }

    private long parseTime(String s) {
        if (s == null || s.isBlank()) {
            return -1;
        }
        try {
            String num = s.replaceAll("[^0-9]", "");
            String unit = s.replaceAll("[0-9]", "").toLowerCase().trim();
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

    private Message color(String text) {
        if (!text.contains("&")) {
            return Message.raw(text);
        }
        List<Message> parts = new ArrayList<>();
        for (String part : text.split("(?=&[0-9a-fk-or])")) {
            if (part.length() < 2 || part.charAt(0) != '&') {
                parts.add(Message.raw(part));
                continue;
            }
            char code = part.charAt(1);
            String content = part.substring(2);
            String hex = hexFromCode(code);
            parts.add(hex != null ? Message.raw(content).color(hex) : Message.raw(content));
        }
        return Message.join(parts.toArray(new Message[0]));
    }

    private String hexFromCode(char c) {
        return switch (c) {
            case '0' -> "#000000";
            case '1' -> "#0000AA";
            case '2' -> "#00AA00";
            case '3' -> "#00AAAA";
            case '4' -> "#AA0000";
            case '5' -> "#AA00AA";
            case '6' -> "#FFAA00";
            case '7' -> "#AAAAAA";
            case '8' -> "#555555";
            case '9' -> "#5555FF";
            case 'a' -> "#55FF55";
            case 'b' -> "#55FFFF";
            case 'c' -> "#FF5555";
            case 'd' -> "#FF55FF";
            case 'e' -> "#FFFF55";
            case 'f' -> "#FFFFFF";
            default  -> null;
        };
    }
}
