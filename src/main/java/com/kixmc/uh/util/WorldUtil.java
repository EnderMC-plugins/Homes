package com.kixmc.uh.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class WorldUtil {

    public static boolean checkIsSafeLocation(Location location, boolean considerMidAirUnsafe) {
        try {
            Block feet = location.getBlock();
            if (feet.getType() == Material.LAVA || feet.getLocation().add(0, 1, 0).getBlock().getType() == Material.LAVA) return false;
            Block head = feet.getRelative(BlockFace.UP);
            if (head.getType().isOccluding() || head.getType() == Material.LAVA) return false;
            Block ground = feet.getRelative(BlockFace.DOWN);
            if (ground.getType() == Material.AIR && considerMidAirUnsafe) return false;
            return ground.getType().isSolid();
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isWorldAllowed(FileConfiguration settings, String worldName) {
        if (worldName == null) return false;
        String type = settings.getString("homeWorlds.type", "BLACKLIST");
        List<String> list = settings.getStringList("homeWorlds.list");

        boolean contains = false;
        for (String s : list) {
            if (s.equalsIgnoreCase(worldName)) {
                contains = true;
                break;
            }
        }

        if ("WHITELIST".equalsIgnoreCase(type)) return contains;

        // default blacklist behavior
        return !contains;
    }

}