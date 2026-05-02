package com.varyon.playtime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.varyon.playtime.Playtime;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {

    private final File configFile;
    private PlaytimeConfig config;
    private final Gson gson;
    private final Logger logger = LoggerFactory.getLogger("Playtime");

    public ConfigManager(File dataFolder) {
        this.configFile = new File(dataFolder, "config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public void init() {
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        load();
    }

    private void saveDefaultConfig() {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        try (InputStream in = Playtime.class.getResourceAsStream("/config.json")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
                logger.info("Modèle config.json copié depuis le JAR.");
            } else {
                config = new PlaytimeConfig();
                save();
                logger.warn("config.json absent du JAR, configuration par défaut créée.");
            }
        } catch (IOException e) {
            logger.error("Impossible de créer config.json", e);
        }
    }

    public void load() {
        try (Reader reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, PlaytimeConfig.class);
            if (config == null) {
                config = new PlaytimeConfig();
            }
            config.setDefaults();
            save();
            logger.info("Configuration chargée.");
        } catch (IOException e) {
            logger.error("Impossible de lire config.json", e);
            config = new PlaytimeConfig();
            config.setDefaults();
            save();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            logger.error("Impossible d’enregistrer config.json", e);
        }
    }

    public PlaytimeConfig getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }
}
