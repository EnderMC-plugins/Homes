package com.kixmc.homes.config;

import com.kixmc.homes.Homes;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigHandler {

    private final Homes plugin;

    public ConfigHandler(Homes plugin) {
        this.plugin = plugin;
        setupConfigs();
    }

    private File configFile;
    @Getter
    private FileConfiguration settings;

    private void setupConfigs() {

        // config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("config.yml", false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // sync before loading so newly added keys are available right away
        syncConfigFromSource(configFile, plugin.getResource("config.yml"));

        settings = new YamlConfiguration();

        try {
            settings.load(configFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void syncConfigFromSource(File target, InputStream source) {
        if (source == null) return;
        FileConfiguration newcfg = YamlConfiguration.loadConfiguration(new InputStreamReader(source));
        FileConfiguration oldcfg = YamlConfiguration.loadConfiguration(target);

        for (String key : newcfg.getKeys(true)) if (!oldcfg.contains(key)) oldcfg.set(key, newcfg.get(key));
        for (String key : oldcfg.getKeys(true)) if (!newcfg.contains(key)) oldcfg.set(key, null);

        try {
            oldcfg.save(target);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}
