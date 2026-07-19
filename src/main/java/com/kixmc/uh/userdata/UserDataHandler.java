package com.kixmc.uh.userdata;

import com.kixmc.uh.UltimateHomes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class UserDataHandler {

    private final UltimateHomes plugin;
    private final UUID uuid;

    private File userFile;
    @Getter
    private FileConfiguration userConfig;

    private void init() {
        File baseDir = new File(plugin.getDataFolder(), "playerdata");
        if (!baseDir.exists()) baseDir.mkdirs();

        this.userFile = new File(baseDir, uuid + ".yml");
        this.userConfig = YamlConfiguration.loadConfiguration(this.userFile);
    }

    public void tryCreateUser(final Player player) {
        if (userFile == null) init();
        if (userFile.exists()) return;

        try {
            userConfig.set("player.info.lastSeenAs", player.getName());
            if (userConfig.getConfigurationSection("homes") == null) userConfig.createSection("homes");

            userConfig.save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveUserFile() {
        if (userFile == null) init();
        try {
            userConfig.save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getUserConfig() {
        if (userConfig == null) init();
        return userConfig;
    }

}