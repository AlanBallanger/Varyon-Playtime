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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.playtime.Playtime;
import com.varyon.playtime.api.PlaytimeAPI;
import com.varyon.playtime.config.PlaytimeConfig;
import com.varyon.playtime.config.Reward;
import com.varyon.playtime.gui.PlaytimeLeaderboardGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlaytimeCommand extends AbstractPlayerCommand {

    public PlaytimeCommand(String name, String... aliases) {
        super(name, Playtime.get().getConfigManager().getConfig().command.description);

        if (aliases != null && aliases.length > 0) {
            addAliases(aliases);
        }

        addUsageVariant(new ActionCommand(name));
        addUsageVariant(new DoubleArgCommand(name));
        addUsageVariant(new RemoveRewardCommand(name));
        addUsageVariant(new AdminRewardCommand(name));
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

    private static Message color(String text) {
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
            String hex = getHexFromCode(code);
            if (hex != null) {
                messageParts.add(Message.raw(content).color(hex));
            } else {
                messageParts.add(Message.raw(content));
            }
        }
        return Message.join(messageParts.toArray(new Message[0]));
    }

    private static String getHexFromCode(char code) {
        switch (code) {
            case '0':
                return "#000000";
            case '1':
                return "#0000AA";
            case '2':
                return "#00AA00";
            case '3':
                return "#00AAAA";
            case '4':
                return "#AA0000";
            case '5':
                return "#AA00AA";
            case '6':
                return "#FFAA00";
            case '7':
                return "#AAAAAA";
            case '8':
                return "#555555";
            case '9':
                return "#5555FF";
            case 'a':
                return "#55FF55";
            case 'b':
                return "#55FFFF";
            case 'c':
                return "#FF5555";
            case 'd':
                return "#FF55FF";
            case 'e':
                return "#FFFF55";
            case 'f':
                return "#FFFFFF";
            default:
                return null;
        }
    }

    private static String format(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }

    private static void showTop(CommandContext ctx, PlayerRef player, String periodArg) {
        if (!ctx.sender().hasPermission("playtime.top")) {
            ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
            return;
        }

        PlaytimeConfig cfg = Playtime.get().getConfigManager().getConfig();
        PlaytimeConfig.PeriodSettings p = cfg.periods;
        String mode = cfg.resolvePeriodKey(periodArg);

        if (mode == null) {
            String valid = String.join(", ", p.daily, p.weekly, p.monthly, p.all);
            ctx.sendMessage(color(cfg.messages.errorInvalidPeriod.replace("%valid_periods%", valid)));
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
            PlaytimeConfig.PeriodSettings periods = config.periods;

            if (arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("aide")) {
                showHelp(ctx);
                return;
            }
            if (arg.equalsIgnoreCase("rewards") || arg.equalsIgnoreCase("recompenses")) {
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
            if (arg.equalsIgnoreCase("menu") || arg.equalsIgnoreCase("gui") || arg.equalsIgnoreCase("interface")) {
                if (!ctx.sender().hasPermission("playtime.gui")) {
                    ctx.sendMessage(color(config.messages.noPermission));
                    return;
                }
                if (ctx.sender() instanceof Player senderPlayer) {
                    senderPlayer.getPageManager().openCustomPage(ref, store, new PlaytimeLeaderboardGui(player));
                }
                return;
            }
            if (arg.equalsIgnoreCase(periods.reload) || arg.equalsIgnoreCase("reload")) {
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
            if (arg.equalsIgnoreCase("top") || arg.equalsIgnoreCase("classement")) {
                if (config.command.topStyle.equalsIgnoreCase("gui")) {
                    if (ctx.sender() instanceof Player senderPlayer) {
                        senderPlayer.getPageManager().openCustomPage(ref, store, new PlaytimeLeaderboardGui(player));
                    }
                } else {
                    showTop(ctx, player, periods.all);
                }
                return;
            }
            if (arg.equalsIgnoreCase(periods.daily)
                    || arg.equalsIgnoreCase(periods.weekly)
                    || arg.equalsIgnoreCase(periods.monthly)
                    || arg.equalsIgnoreCase(periods.all)) {
                showTop(ctx, player, arg);
                return;
            }

            String cmd = config.command.name;
            ctx.sendMessage(color("&cCommande inconnue. Essayez /" + cmd + " aide"));
        }

        private void showHelp(CommandContext ctx) {
            PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
            String cmd = config.command.name;
            ctx.sendMessage(color("&6--- Aide temps de jeu ---"));
            ctx.sendMessage(color("&e/" + cmd + " &7- Afficher votre temps de jeu"));
            ctx.sendMessage(color("&e/" + cmd + " recompenses &7- &8|&7 /" + cmd + " rewards — Liste des récompenses"));
            ctx.sendMessage(color("&e/" + cmd + " classement &7- &8|&7 /" + cmd + " top — Classement (période « total »)"));
            ctx.sendMessage(color("&e/" + cmd + " classement [periode] &7- Classement (jour, semaine, mois, total)"));
            if (ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(color("&c--- Admin ---"));
                ctx.sendMessage(color("&c/" + cmd + " admin &7- Guide des récompenses"));
                ctx.sendMessage(color("&c/" + cmd + " admin listeRecompenses &7- Récompenses configurées"));
                ctx.sendMessage(color("&c/" + cmd + " admin ajouterRecompense &7- Ajouter une récompense"));
                ctx.sendMessage(color("&c/" + cmd + " admin supprimerRecompense <id> &7- Retirer une récompense"));
                ctx.sendMessage(color("&c/" + cmd + " " + config.periods.reload + " &7- &f| reload &7- Recharger la config"));
            }
        }

        private void showAdminGuide(CommandContext ctx) {
            String cmd = Playtime.get().getConfigManager().getConfig().command.name;
            ctx.sendMessage(color("&6--- Guide des récompenses ---"));
            ctx.sendMessage(color("&eAjouter une récompense :"));
            ctx.sendMessage(color("&f/" + cmd + " admin ajouterRecompense <id> <periode> <duree> <commande>"));
            ctx.sendMessage(color("&7- &eid&7 : nom unique (ex. or_journalier)"));
            ctx.sendMessage(color("&7- &eperiode&7 : jour, semaine, mois ou total"));
            ctx.sendMessage(color("&7- &eduree&7 : 30m, 1h, 1j, 10s"));
            ctx.sendMessage(color("&7- &ecommande&7 : commande console. Variable &f%player% &7(pseudo)."));
            ctx.sendMessage(color("&7  Exemple : &f/" + cmd
                    + " admin ajouterRecompense or_journalier jour 1h \"give %player% gold 10\""));
        }

        private void listUserRewards(CommandContext ctx, PlayerRef player, PlaytimeConfig cfg) {
            ctx.sendMessage(color(cfg.messages.rewardListHeader));

            String uuid = player.getUuid().toString();

            for (Reward r : cfg.rewards) {
                boolean claimed = Playtime.get().getRewardManager().isClaimed(uuid, r);
                long playtime = PlaytimeAPI.get().getPlaytime(player.getUuid(), r.period);
                boolean eligible = playtime >= r.timeRequirement;

                String status;
                if (claimed) {
                    status = cfg.messages.statusClaimed;
                } else if (eligible) {
                    status = cfg.messages.statusAvailable;
                } else {
                    status = cfg.messages.statusLocked;
                }

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

            if (a1.equalsIgnoreCase("top") || a1.equalsIgnoreCase("classement")) {
                showTop(ctx, player, a2);
                return;
            }

            if (a1.equalsIgnoreCase("admin")
                    && (a2.equalsIgnoreCase("listRewards") || a2.equalsIgnoreCase("listeRecompenses"))) {
                if (!ctx.sender().hasPermission("playtime.admin")) {
                    ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
                    return;
                }
                ctx.sendMessage(color("&6--- Récompenses configurées (admin) ---"));
                PlaytimeConfig cfg = Playtime.get().getConfigManager().getConfig();
                for (Reward r : cfg.rewards) {
                    ctx.sendMessage(color("&eID : &f" + r.id));
                    ctx.sendMessage(color("  &7Période : " + cfg.displayPeriod(r.period)));
                    ctx.sendMessage(color("  &7Temps : " + format(r.timeRequirement)));
                    ctx.sendMessage(color(
                            "  &7Commande : " + (r.commands.isEmpty() ? "Aucune" : r.commands.get(0))));
                }
                return;
            }

            String cmd = Playtime.get().getConfigManager().getConfig().command.name;
            ctx.sendMessage(color("&cCommande inconnue. Ex. : /" + cmd + " aide"));
        }
    }

    private static class RemoveRewardCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;
        private final RequiredArg<String> idArg;

        RemoveRewardCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("admin", "Admin", ArgTypes.STRING);
            this.arg2 = withRequiredArg("action", "Action admin", ArgTypes.STRING);
            this.idArg = withRequiredArg("id", "Identifiant récompense", ArgTypes.STRING);
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
            if (!ctx.get(arg2).equalsIgnoreCase("removeReward")
                    && !ctx.get(arg2).equalsIgnoreCase("supprimerRecompense")) {
                return;
            }

            if (!ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(color(Playtime.get().getConfigManager().getConfig().messages.noPermission));
                return;
            }

            String id = ctx.get(idArg);
            PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();

            boolean removed = config.rewards.removeIf(r -> r.id.equals(id));

            if (removed) {
                Playtime.get().getConfigManager().save();
                ctx.sendMessage(color(config.messages.rewardRemoved.replace("%id%", id)));
            } else {
                ctx.sendMessage(color(config.messages.rewardNotFound.replace("%id%", id)));
            }
        }
    }

    private static class AdminRewardCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;
        private final RequiredArg<String> idArg;
        private final RequiredArg<String> periodArg;
        private final RequiredArg<String> timeArg;
        private final RequiredArg<String> commandArg;

        AdminRewardCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("admin", "Admin", ArgTypes.STRING);
            this.arg2 = withRequiredArg("action", "Action admin", ArgTypes.STRING);
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
                    || (!ctx.get(arg2).equalsIgnoreCase("addReward")
                            && !ctx.get(arg2).equalsIgnoreCase("ajouterRecompense"))) {
                return;
            }

            String id = ctx.get(idArg);
            String periodRaw = ctx.get(periodArg);
            String timeStr = ctx.get(timeArg);
            String cmdToRun = ctx.get(commandArg);

            PlaytimeConfig cfg0 = Playtime.get().getConfigManager().getConfig();
            String periodKey = cfg0.resolvePeriodKey(periodRaw);
            if (periodKey == null) {
                ctx.sendMessage(color("&cPériode invalide. Utilisez : jour, semaine, mois ou total (ou leurs alias anglais)."));
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

            PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
            config.rewards.add(newReward);
            Playtime.get().getConfigManager().save();

            ctx.sendMessage(color(config.messages.rewardAdded.replace("%id%", id)));
        }

        private long parseTime(String input) {
            try {
                String number = input.replaceAll("[^0-9]", "");
                String unit = input.replaceAll("[0-9]", "").toLowerCase();
                if (number.isEmpty()) {
                    return -1;
                }
                long val = Long.parseLong(number);
                switch (unit) {
                    case "s":
                        return val * 1000;
                    case "m":
                        return val * 60 * 1000;
                    case "h":
                        return val * 60 * 60 * 1000;
                    case "d":
                    case "j":
                        return val * 24 * 60 * 60 * 1000;
                    default:
                        return val;
                }
            } catch (Exception e) {
                return -1;
            }
        }
    }
}
