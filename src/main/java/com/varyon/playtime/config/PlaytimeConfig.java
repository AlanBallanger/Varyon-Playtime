package com.varyon.playtime.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaytimeConfig {

    public DatabaseSettings database = new DatabaseSettings();
    public CommandSettings command = new CommandSettings();
    public PeriodSettings periods = new PeriodSettings();
    public MessageSettings messages = new MessageSettings();
    public GuiSettings gui = new GuiSettings();
    public List<Reward> rewards = new ArrayList<>();
    public List<Milestone> milestones = new ArrayList<>();
    public MilestoneSettings milestoneSettings = new MilestoneSettings();

    public void setDefaults() {
        if (database == null) {
            database = new DatabaseSettings();
        }
        if (command == null) {
            command = new CommandSettings();
        }
        if (periods == null) {
            periods = new PeriodSettings();
        }
        if (messages == null) {
            messages = new MessageSettings();
        }
        if (gui == null) {
            gui = new GuiSettings();
        }
        if (rewards == null) {
            rewards = new ArrayList<>();
        }
        if (milestones == null) {
            milestones = new ArrayList<>();
        }
        if (milestoneSettings == null) {
            milestoneSettings = new MilestoneSettings();
        }

        if (command.topStyle == null) {
            command.topStyle = "text";
        }
        if (command.aliases == null) {
            command.aliases = Arrays.asList("pt", "play", "time");
        }
        if (gui != null) {
            if (gui.footerRankCaption == null) {
                gui.footerRankCaption = "Rang :";
            }
            if (gui.footerTimeCaption == null) {
                gui.footerTimeCaption = "Temps : ";
            }
        }
        heuristicallyUseFrenchCopy();
    }

    private void heuristicallyUseFrenchCopy() {
        if (looksLikeLegacyEnglishMessages(messages)) {
            messages = new MessageSettings();
        }
        if (looksLikeEnglishPeriodLabels(periods)) {
            periods = new PeriodSettings();
        }
        if (command != null && looksLikeEnglishCommandDescription(command)) {
            command.description = new CommandSettings().description;
        }
    }

    private static boolean looksLikeEnglishCommandDescription(CommandSettings c) {
        if (c == null || c.description == null) {
            return false;
        }
        String d = c.description.toLowerCase();
        return d.contains("check your playtime") || d.contains("playtime stats");
    }

    private static boolean looksLikeLegacyEnglishMessages(MessageSettings m) {
        if (m == null) {
            return false;
        }
        if (m.leaderboardHeader != null && m.leaderboardHeader.contains("Playtime Leaderboard")) {
            return true;
        }
        if (m.selfCheck != null && m.selfCheck.contains("Total Playtime")) {
            return true;
        }
        if (m.rewardListHeader != null && m.rewardListHeader.contains("Server Rewards")) {
            return true;
        }
        if (m.leaderboardEmpty != null && m.leaderboardEmpty.contains("No data")) {
            return true;
        }
        if (m.noPermission != null && m.noPermission.contains("You do not have permission")) {
            return true;
        }
        return false;
    }

    private static boolean looksLikeEnglishPeriodLabels(PeriodSettings p) {
        if (p == null) {
            return false;
        }
        return p.daily != null
                && p.daily.equalsIgnoreCase("daily")
                && p.weekly != null
                && p.weekly.equalsIgnoreCase("weekly")
                && p.monthly != null
                && p.monthly.equalsIgnoreCase("monthly")
                && p.all != null
                && p.all.equalsIgnoreCase("all");
    }

    public String displayPeriod(String internalKey) {
        if (internalKey == null) {
            return "";
        }
        PeriodSettings p = periods != null ? periods : new PeriodSettings();
        return switch (internalKey) {
            case "daily" -> p.daily != null ? p.daily : "jour";
            case "weekly" -> p.weekly != null ? p.weekly : "semaine";
            case "monthly" -> p.monthly != null ? p.monthly : "mois";
            case "all" -> p.all != null ? p.all : "total";
            default -> internalKey;
        };
    }

    public String resolvePeriodKey(String input) {
        if (input == null) {
            return null;
        }
        setDefaults();
        String t = input.trim();
        if (t.isEmpty()) {
            return null;
        }
        PeriodSettings p = periods;
        if (t.equalsIgnoreCase("daily") || (p.daily != null && t.equalsIgnoreCase(p.daily))) {
            return "daily";
        }
        if (t.equalsIgnoreCase("weekly") || (p.weekly != null && t.equalsIgnoreCase(p.weekly))) {
            return "weekly";
        }
        if (t.equalsIgnoreCase("monthly") || (p.monthly != null && t.equalsIgnoreCase(p.monthly))) {
            return "monthly";
        }
        if (t.equalsIgnoreCase("all") || (p.all != null && t.equalsIgnoreCase(p.all))) {
            return "all";
        }
        return null;
    }

    public static class DatabaseSettings {
        public String type = "sqlite";
        public String host = "localhost";
        public int port = 3306;
        public String databaseName = "playtime_db";
        public String username = "root";
        public String password = "password";
        public boolean useSSL = false;
    }

    public static class CommandSettings {
        public String name = "playtime";
        public String description = "Afficher votre temps de jeu et les classements";
        public List<String> aliases = Arrays.asList("pt", "play", "time");
        public String topStyle = "text";
    }

    public static class PeriodSettings {
        public String daily = "jour";
        public String weekly = "semaine";
        public String monthly = "mois";
        public String all = "total";
        public String reload = "recharger";
    }

    public static class MessageSettings {
        public String selfCheck = "&dTemps de jeu total : &e%time%";
        public String otherCheck = "&dTemps de jeu de &f%player%&d : &e%time%";
        public String leaderboardHeader = "&6--- Classement temps de jeu (&e%period_name%&6) ---";
        public String leaderboardEntry = "&6#%rank% &e%player% &7: &f%time%";
        public String leaderboardEmpty = "&7Aucune donnée pour l’instant.";
        public String reloadSuccess = "&aConfiguration rechargée.";
        public String reloadNoPermission = "&cVous n’avez pas la permission de recharger.";
        public String reloadFailed = "&cÉchec du rechargement. Voir la console.";
        public String errorInvalidPeriod = "&cPériode invalide. Utilisez : %valid_periods%";
        public String errorConsole = "&cRéservé aux joueurs.";
        public String noPermission = "&cVous n’avez pas la permission.";
        public String rewardAdded = "&aRécompense « %id% » ajoutée.";
        public String rewardRemoved = "&aRécompense « %id% » supprimée.";
        public String rewardNotFound = "&cRécompense « %id% » introuvable.";
        public String rewardBroadcast = "&6%player% &ea joué &6%time% &eet a reçu la récompense &6%reward%&e !";
        public String rewardListHeader = "&6--- Récompenses du serveur ---";
        public String rewardListEntry = "&e%id% &7(%period%) : &f%status%";
        public String statusClaimed = "&a[REÇUE]";
        public String statusAvailable = "&e[DISPONIBLE]";
        public String statusLocked = "&c[VERROUILLÉE]";
    }

    public static class GuiSettings {
        public String title = "CLASSEMENT";
        public String buttonAll = "TOTAL";
        public String buttonDaily = "QUOTIDIEN";
        public String buttonWeekly = "HEBDO";
        public String buttonMonthly = "MENSUEL";
        public String footerTitle = "VOS STATISTIQUES :";
        public String footerRankCaption = "Rang :";
        public String footerTimeCaption = "Temps : ";
        public String rankPrefix = "Rang : n°";
        public String timePrefix = "Temps : ";
        public String rankIfNone = "—";
    }

    public static class MilestoneSettings {
        public boolean enabled = true;
        public String tierBronzeMax = "10h";
        public String tierSilverMax = "50h";
        public String tierGoldMax = "100h";
        public String broadcastBronze = "&8[&6✦ Bronze&8] &f%player% &7vient d'atteindre &e%time% &7de jeu !";
        public String broadcastSilver = "&8[&7✦✦ Argent&8] &f%player% &7vient d'atteindre &f%time% &7de jeu !";
        public String broadcastGold   = "&6✦✦✦ &f%player% &6a atteint &e%time% &6de jeu ! &6✦✦✦";
        public String broadcastDiamond = "&b◆◆◆ &f%player% &ba atteint &3%time% &bde jeu ! &b◆◆◆";
    }
}
