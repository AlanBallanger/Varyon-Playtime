package com.varyon.playtime.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.playtime.Playtime;
import com.varyon.playtime.api.PlaytimeAPI;
import com.varyon.playtime.config.PlaytimeConfig;
import com.varyon.playtime.config.Reward;
import com.varyon.playtime.gui.PlaytimeLeaderboardGui;
import com.varyon.playtime.listeners.SessionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlaytimeCommand extends AbstractPlayerCommand {

    public PlaytimeCommand(String name, String... aliases) {
        super(name, Playtime.get().getConfigManager().getConfig().command.description);

        if (aliases != null && aliases.length > 0) {
            addAliases(aliases);
        }

        addUsageVariant(new ActionCommand(name));
        addUsageVariant(new DoubleArgCommand(name));
        addUsageVariant(new AdminThreeArgCommand(name));
        addUsageVariant(new AdminAddRewardCommand(name));
        addUsageVariant(new AdminSetTimeCommand(name));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        if (!ctx.sender().hasPermission("playtime.check")) {
            ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
            return;
        }

        long total = Playtime.get().getService().getTotalPlaytime(player.getUuid().toString());
        String msg = Playtime.get().getConfigManager().getConfig().messages.selfCheck
                .replace("%time%", format(total))
                .replace("%player%", player.getUsername());
        ctx.sendMessage(color(msg));
    }

    static Message color(String text) {
        if (!text.contains("&")) {
            return Message.raw(text);
        }
        List<Message> messageParts = new ArrayList<>();
        String[] parts = text.split("(?=&[0-9a-fk-or])");
        for (String part : parts) {
            if (part.length() < 2 || part.charAt(0) != '&') {
                messageParts.add(Message.raw(part));
                continue;
            }
            char code = part.charAt(1);
            String content = part.substring(2);
            String hex = hexFromCode(code);
            if (hex != null) {
                messageParts.add(Message.raw(content).color(hex));
            } else {
                messageParts.add(Message.raw(content));
            }
        }
        return Message.join(messageParts.toArray(new Message[0]));
    }

    private static String hexFromCode(char code) {
        switch (code) {
            case '0': return "#000000";
            case '1': return "#0000AA";
            case '2': return "#00AA00";
            case '3': return "#00AAAA";
            case '4': return "#AA0000";
            case '5': return "#AA00AA";
            case '6': return "#FFAA00";
            case '7': return "#AAAAAA";
            case '8': return "#555555";
            case '9': return "#5555FF";
            case 'a': return "#55FF55";
            case 'b': return "#55FFFF";
            case 'c': return "#FF5555";
            case 'd': return "#FF55FF";
            case 'e': return "#FFFF55";
            case 'f': return "#FFFFFF";
            default:  return null;
        }
    }

    static String format(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }

    static long parseTime(String input) {
        try {
            String number = input.replaceAll("[^0-9]", "");
            String unit = input.replaceAll("[0-9]", "").toLowerCase();
            if (number.isEmpty()) {
                return -1;
            }
            long val = Long.parseLong(number);
            switch (unit) {
                case "s":  return val * 1_000L;
                case "m":  return val * 60_000L;
                case "h":  return val * 3_600_000L;
                case "d":
                case "j":  return val * 86_400_000L;
                default:   return val;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private static void showTop(CommandContext ctx, PlayerRef player, String periodArg) {
        if (!ctx.sender().hasPermission("playtime.top")) {
            ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
            return;
        }

        PlaytimeConfig cfg = Playtime.get().getConfigManager().getConfig();
        String mode = cfg.resolvePeriodKey(periodArg);

        if (mode == null) {
            ctx.sendMessage(color(cfg.messages.errorInvalidPeriod
                    .replace("%valid_periods%", "daily, weekly, monthly, all")));
            return;
        }

        Map<String, Long> sorted = Playtime.get().getService().getTopPlayers(mode);
        ctx.sendMessage(color(cfg.messages.leaderboardHeader.replace("%period_name%", cfg.displayPeriod(mode))));

        if (sorted.isEmpty()) {
            ctx.sendMessage(color(cfg.messages.leaderboardEmpty));
            return;
        }

        int rank = 1;
        for (Map.Entry<String, Long> entry : sorted.entrySet()) {
            String line = cfg.messages.leaderboardEntry
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%player%", entry.getKey())
                    .replace("%time%", format(entry.getValue()));
            ctx.sendMessage(color(line));
            rank++;
        }
    }

    @SuppressWarnings("deprecation")
    static UUID resolvePlayerUuid(String username) {
        for (PlayerRef p : Universe.get().getPlayers()) {
            if (p.getUsername().equalsIgnoreCase(username)) {
                return p.getUuid();
            }
        }
        String uuidStr = Playtime.get().getDatabaseManager().getUuidByUsername(username);
        if (uuidStr != null) {
            try {
                return UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    static String resolveUsername(UUID uuid) {
        for (PlayerRef p : Universe.get().getPlayers()) {
            if (p.getUuid().equals(uuid)) {
                return p.getUsername();
            }
        }
        return uuid.toString();
    }

    private static class ActionCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;

        ActionCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("action", "Sous-commande", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(
                CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String arg = ctx.get(arg1);
            PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();

            if (arg.equalsIgnoreCase("help")) {
                showHelp(ctx);
                return;
            }
            if (arg.equalsIgnoreCase("rewards")) {
                listUserRewards(ctx, player, config);
                return;
            }
            if (arg.equalsIgnoreCase("admin")) {
                if (!ctx.sender().hasPermission("playtime.admin")) {
                    ctx.sendMessage(color(config.messages.noPermission));
                    return;
                }
                showAdminGuide(ctx);
                return;
            }
            if (arg.equalsIgnoreCase("menu") || arg.equalsIgnoreCase("gui")) {
                if (!ctx.sender().hasPermission("playtime.gui")) {
                    ctx.sendMessage(color(config.messages.noPermission));
                    return;
                }
                if (ctx.sender() instanceof Player senderPlayer) {
                    senderPlayer.getPageManager().openCustomPage(ref, store, new PlaytimeLeaderboardGui(player));
                }
                return;
            }
            if (arg.equalsIgnoreCase("reload")) {
                if (!ctx.sender().hasPermission("playtime.reload")) {
                    ctx.sendMessage(color(config.messages.reloadNoPermission));
                    return;
                }
                try {
                    Playtime.get().getConfigManager().load();
                    ctx.sendMessage(color(config.messages.reloadSuccess));
                } catch (Exception e) {
                    ctx.sendMessage(color(config.messages.reloadFailed));
                    e.printStackTrace();
                }
                return;
            }
            if (arg.equalsIgnoreCase("top")) {
                if (config.command.topStyle.equalsIgnoreCase("gui")) {
                    if (ctx.sender() instanceof Player senderPlayer) {
                        senderPlayer.getPageManager().openCustomPage(ref, store, new PlaytimeLeaderboardGui(player));
                    }
                } else {
                    showTop(ctx, player, "all");
                }
                return;
            }

            String cmd = config.command.name;
            ctx.sendMessage(color("&cCommande inconnue. Essayez /" + cmd + " help"));
        }

        private void showHelp(CommandContext ctx) {
            PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
            String cmd = config.command.name;
            ctx.sendMessage(color("&6--- Aide temps de jeu ---"));
            ctx.sendMessage(color("&e/" + cmd + "                      &7- Votre temps de jeu"));
            ctx.sendMessage(color("&e/" + cmd + " rewards              &7- Liste des récompenses"));
            ctx.sendMessage(color("&e/" + cmd + " top                  &7- Classement (période : total)"));
            ctx.sendMessage(color("&e/" + cmd + " top <période>        &7- daily · weekly · monthly · all"));
            ctx.sendMessage(color("&e/" + cmd + " menu                 &7- Interface graphique"));
            if (ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(color("&c--- Admin ---"));
                ctx.sendMessage(color("&c/" + cmd + " reload"));
                ctx.sendMessage(color("&c/" + cmd + " admin listRewards"));
                ctx.sendMessage(color("&c/" + cmd + " admin addReward <id> <période> <durée> <commande>"));
                ctx.sendMessage(color("&c/" + cmd + " admin removeReward <id>"));
                ctx.sendMessage(color("&c/" + cmd + " admin resetTime <joueur>"));
                ctx.sendMessage(color("&c/" + cmd + " admin setTime <joueur> <heures>"));
            }
        }

        private void showAdminGuide(CommandContext ctx) {
            String cmd = Playtime.get().getConfigManager().getConfig().command.name;
            ctx.sendMessage(color("&6--- Guide admin ---"));
            ctx.sendMessage(color("&e/" + cmd + " admin addReward <id> <période> <durée> <commande>"));
            ctx.sendMessage(color("&7  Périodes : daily, weekly, monthly, all"));
            ctx.sendMessage(color("&7  Durées   : 30m, 1h, 2j …"));
            ctx.sendMessage(color("&7  Variable dans la commande : &f%player%"));
            ctx.sendMessage(color("&e/" + cmd + " admin removeReward <id>"));
            ctx.sendMessage(color("&e/" + cmd + " admin resetTime <joueur>   &7- Remettre à zéro"));
            ctx.sendMessage(color("&e/" + cmd + " admin setTime <joueur> <heures>"));
        }

        private void listUserRewards(CommandContext ctx, PlayerRef player, PlaytimeConfig cfg) {
            ctx.sendMessage(color(cfg.messages.rewardListHeader));
            String uuid = player.getUuid().toString();

            for (Reward r : cfg.rewards) {
                boolean claimed = Playtime.get().getRewardManager().isClaimed(uuid, r);
                long playtime = PlaytimeAPI.get().getPlaytime(player.getUuid(), r.period);
                boolean eligible = playtime >= r.timeRequirement;

                String status = claimed ? cfg.messages.statusClaimed
                        : eligible ? cfg.messages.statusAvailable
                        : cfg.messages.statusLocked;

                String line = cfg.messages.rewardListEntry
                        .replace("%id%", r.id)
                        .replace("%period%", cfg.displayPeriod(r.period))
                        .replace("%status%", status + " &7(" + format(r.timeRequirement) + ")");
                ctx.sendMessage(color(line));
            }
        }
    }

    private static class DoubleArgCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;

        DoubleArgCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("arg1", "Premier argument", ArgTypes.STRING);
            this.arg2 = withRequiredArg("arg2", "Deuxième argument", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(
                CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String a1 = ctx.get(arg1);
            String a2 = ctx.get(arg2);

            if (a1.equalsIgnoreCase("top")) {
                showTop(ctx, player, a2);
                return;
            }

            if (a1.equalsIgnoreCase("admin") && a2.equalsIgnoreCase("listRewards")) {
                if (!ctx.sender().hasPermission("playtime.admin")) {
                    ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
                    return;
                }
                PlaytimeConfig cfg = Playtime.get().getConfigManager().getConfig();
                ctx.sendMessage(color("&6--- Récompenses configurées ---"));
                for (Reward r : cfg.rewards) {
                    ctx.sendMessage(color("&eID : &f" + r.id
                            + "  &7période : " + cfg.displayPeriod(r.period)
                            + "  temps : " + format(r.timeRequirement)));
                }
                return;
            }

            String cmd = Playtime.get().getConfigManager().getConfig().command.name;
            ctx.sendMessage(color("&cCommande inconnue. /" + cmd + " help"));
        }
    }

    private static class AdminThreeArgCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;
        private final RequiredArg<String> arg3;

        AdminThreeArgCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("admin", "admin", ArgTypes.STRING);
            this.arg2 = withRequiredArg("action", "action", ArgTypes.STRING);
            this.arg3 = withRequiredArg("target", "cible", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(
                CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (!ctx.get(arg1).equalsIgnoreCase("admin")) {
                return;
            }
            if (!ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
                return;
            }

            String action = ctx.get(arg2);
            String target = ctx.get(arg3);

            if (action.equalsIgnoreCase("removeReward")) {
                PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
                boolean removed = config.rewards.removeIf(r -> r.id.equals(target));
                if (removed) {
                    Playtime.get().getConfigManager().save();
                    ctx.sendMessage(color(config.messages.rewardRemoved.replace("%id%", target)));
                } else {
                    ctx.sendMessage(color(config.messages.rewardNotFound.replace("%id%", target)));
                }
                return;
            }

            if (action.equalsIgnoreCase("resetTime")) {
                UUID targetUuid = resolvePlayerUuid(target);
                if (targetUuid == null) {
                    ctx.sendMessage(color("&cJoueur introuvable : " + target));
                    return;
                }
                Playtime.get().getService().resetPlaytime(targetUuid.toString());
                SessionListener.overrideSession(targetUuid, 0L);
                ctx.sendMessage(color("&aTPS de &f" + target + " &aremis à zéro."));
                return;
            }

            ctx.sendMessage(color("&cAction inconnue."));
        }
    }

    private static class AdminSetTimeCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;
        private final RequiredArg<String> arg3;
        private final RequiredArg<String> arg4;

        AdminSetTimeCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("admin", "admin", ArgTypes.STRING);
            this.arg2 = withRequiredArg("action", "action", ArgTypes.STRING);
            this.arg3 = withRequiredArg("target", "cible", ArgTypes.STRING);
            this.arg4 = withRequiredArg("value", "valeur", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(
                CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (!ctx.get(arg1).equalsIgnoreCase("admin")
                    || !ctx.get(arg2).equalsIgnoreCase("setTime")) {
                return;
            }
            if (!ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
                return;
            }

            String target = ctx.get(arg3);
            String valueStr = ctx.get(arg4);

            long millis = parseTime(valueStr);
            if (millis < 0) {
                ctx.sendMessage(color("&cValeur invalide. Ex. : 10h, 30m, 2j, 3600s"));
                return;
            }

            UUID targetUuid = resolvePlayerUuid(target);
            if (targetUuid == null) {
                ctx.sendMessage(color("&cJoueur introuvable : " + target));
                return;
            }

            String username = resolveUsername(targetUuid);
            Playtime.get().getService().setPlaytime(targetUuid.toString(), username, millis);
            SessionListener.overrideSession(targetUuid, millis);
            ctx.sendMessage(color("&aTemps de &f" + target + " &adéfini à &e" + format(millis) + "&a."));
        }
    }

    private static class AdminAddRewardCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;
        private final RequiredArg<String> idArg;
        private final RequiredArg<String> periodArg;
        private final RequiredArg<String> timeArg;
        private final RequiredArg<String> commandArg;

        AdminAddRewardCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("admin", "admin", ArgTypes.STRING);
            this.arg2 = withRequiredArg("action", "action", ArgTypes.STRING);
            this.idArg = withRequiredArg("id", "Identifiant", ArgTypes.STRING);
            this.periodArg = withRequiredArg("period", "Période", ArgTypes.STRING);
            this.timeArg = withRequiredArg("time", "Durée", ArgTypes.STRING);
            this.commandArg = withRequiredArg("command", "Commande", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(
                CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (!ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
                return;
            }
            if (!ctx.get(arg1).equalsIgnoreCase("admin")
                    || !ctx.get(arg2).equalsIgnoreCase("addReward")) {
                return;
            }

            String id = ctx.get(idArg);
            String periodRaw = ctx.get(periodArg);
            String timeStr = ctx.get(timeArg);
            String cmdToRun = ctx.get(commandArg);

            PlaytimeConfig cfg = Playtime.get().getConfigManager().getConfig();
            String periodKey = cfg.resolvePeriodKey(periodRaw);
            if (periodKey == null) {
                ctx.sendMessage(color("&cPériode invalide. Utilisez : daily, weekly, monthly, all"));
                return;
            }

            long ms = parseTime(timeStr);
            if (ms <= 0) {
                ctx.sendMessage(color("&cDurée invalide. Ex. : 30m, 1h, 1j, 10s."));
                return;
            }

            List<String> cmds = new ArrayList<>();
            cmds.add(cmdToRun);
            String broadcast = "&6%player% &ea joué &6%time% &eet a reçu la récompense &6" + id + "&e !";
            Reward newReward = new Reward(id, periodKey, ms, cmds, broadcast);

            cfg.rewards.add(newReward);
            Playtime.get().getConfigManager().save();
            ctx.sendMessage(color(cfg.messages.rewardAdded.replace("%id%", id)));
        }
    }
}
