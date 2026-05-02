package com.varyon.playtime;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.varyon.playtime.commands.PlaytimeCommand;
import com.varyon.playtime.config.ConfigManager;
import com.varyon.playtime.config.PlaytimeConfig;
import com.varyon.playtime.database.DatabaseManager;
import com.varyon.playtime.listeners.SessionListener;
import com.varyon.playtime.milestones.MilestoneManager;
import com.varyon.playtime.rewards.RewardManager;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Playtime extends JavaPlugin {

    private static Playtime INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger("Playtime");

    private ConfigManager configManager;
    private DatabaseManager db;
    private PlaytimeService service;
    private RewardManager rewardManager;
    private MilestoneManager milestoneManager;

    public Playtime(@NonNullDecl JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    public static Playtime get() {
        return INSTANCE;
    }

    @Override
    protected void setup() {
        Path dataPath = getDataDirectory().getParent().resolve("Varyon_VaryonPlaytime").normalize();
        File dataFolder = dataPath.toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        configManager = new ConfigManager(dataFolder);
        configManager.init();
        PlaytimeConfig cfg = configManager.getConfig();

        db = new DatabaseManager(dataFolder);
        db.init();
        service = new PlaytimeService(db);
        rewardManager = new RewardManager(db);
        milestoneManager = new MilestoneManager(db);

        String cmdName = cfg.command.name;
        String[] aliases = cfg.command.aliases.toArray(new String[0]);
        getCommandRegistry().registerCommand(new PlaytimeCommand(cmdName, aliases));

        getEventRegistry().registerGlobal(PlayerConnectEvent.class, SessionListener::onJoin);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, SessionListener::onQuit);

        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                () -> {
                    try {
                        rewardManager.checkRewards();
                        milestoneManager.checkMilestones();
                    } catch (Exception e) {
                        logger.error("Erreur dans la tâche de récompenses/milestones", e);
                    }
                },
                1L,
                1L,
                TimeUnit.MINUTES);

        logger.info("Playtime chargé. Commande principale : /" + cmdName);
    }

    @Override
    protected void shutdown() {
        SessionListener.saveAllSessions();
        if (db != null) {
            db.close();
        }
    }

    public PlaytimeService getService() {
        return service;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public MilestoneManager getMilestoneManager() {
        return milestoneManager;
    }

    public DatabaseManager getDatabaseManager() {
        return db;
    }
}
