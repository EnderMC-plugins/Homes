package com.kixmc.homes.userdata;

import com.kixmc.homes.Homes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public final class UserLookupUtil {

    public static UUID getUUIDFromUsername(Homes plugin, String name) {
        if (name == null || name.isBlank()) return null;
        String target = name.toLowerCase();

        File directory = new File(plugin.getDataFolder(), "playerdata");
        if (!directory.exists() || !directory.isDirectory()) return null;

        File[] files = directory.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null) return null;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String lastSeenName = config.getString("player.info.lastSeenAs");
            if (lastSeenName == null) continue;

            if (lastSeenName.equalsIgnoreCase(target)) {
                String fileName = file.getName();
                String uuidStr = fileName.substring(0, Math.max(0, fileName.length() - 4));
                try {
                    return UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return null;
    }

}