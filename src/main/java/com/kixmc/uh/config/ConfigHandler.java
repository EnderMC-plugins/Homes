package com.kixmc.uh.config;

import com.kixmc.uh.UltimateHomes;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ConfigHandler {

    private final UltimateHomes plugin;

    public ConfigHandler(UltimateHomes plugin) {
        this.plugin = plugin;
        setupConfigs();
    }

    @Getter @Setter
    private File langFile;
    @Getter @Setter
    private FileConfiguration lang;

    @Getter @Setter
    private File settingsFile;
    @Getter @Setter
    private FileConfiguration settings;

    private void setupConfigs() {

        // lang.yml
        langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("lang.yml", false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // settings.yml
        settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            settingsFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("settings.yml", false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        lang = new YamlConfiguration();
        settings = new YamlConfiguration();

        try {
            lang.load(langFile);
            settings.load(settingsFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        syncConfigFromSource(langFile, plugin.getResource("lang.yml"));
        syncConfigFromSource(settingsFile, plugin.getResource("settings.yml"));

    }

    public void reload() {
        setupConfigs();
    }

    public void save() {
        try {
            lang.save(langFile);
            settings.save(settingsFile);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save file to " + langFile.getName() + "!", ex);
            ex.printStackTrace();
        }
    }

    public void syncConfigFromSource(File target, InputStream source) {
        if (source == null) return;
        FileConfiguration newcfg = YamlConfiguration.loadConfiguration(new InputStreamReader(source));
        FileConfiguration oldcfg = YamlConfiguration.loadConfiguration(target);

        // legacy conversions
        // settings.yml: "default-max-homes" -> "defaultMaxHomes"
        try {
            if (target.getName().equalsIgnoreCase("settings.yml")) {
                if (oldcfg.contains("default-max-homes") && !oldcfg.contains("defaultMaxHomes")) {
                    oldcfg.set("defaultMaxHomes", oldcfg.get("default-max-homes"));
                    oldcfg.set("default-max-homes", null);
                }
            }
        } catch (Exception ignored) {}

        for (String key : newcfg.getKeys(true)) if (!oldcfg.contains(key)) oldcfg.set(key, newcfg.get(key));
        for (String key : oldcfg.getKeys(true)) if (!newcfg.contains(key)) oldcfg.set(key, null);

        try {
            oldcfg.save(target);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}