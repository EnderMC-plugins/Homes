package com.kixmc.uh.listener;

import com.kixmc.uh.UltimateHomes;
import com.kixmc.uh.userdata.UserDataHandler;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

@RequiredArgsConstructor
public class RespawnListener implements Listener {

    private final UltimateHomes plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (plugin.getConfigHandler().getSettings().getBoolean("respawnAtHome")) {
            Player p = e.getPlayer();
            UserDataHandler user = new UserDataHandler(plugin, p.getUniqueId());

            if (user.getUserConfig().getConfigurationSection("homes") != null
                    && user.getUserConfig().getConfigurationSection("homes").getKeys(false).contains("home")) {
                int x = user.getUserConfig().getInt("homes.home.x");
                int y = user.getUserConfig().getInt("homes.home.y");
                int z = user.getUserConfig().getInt("homes.home.z");
                float yaw = user.getUserConfig().getInt("homes.home.yaw");
                float pitch = user.getUserConfig().getInt("homes.home.pitch");
                String world = user.getUserConfig().getString("homes.home.world");

                Location loc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                loc.add(0.500, 0, 0.500);

                if (loc.getWorld().getEnvironment() == World.Environment.THE_END) return;

                e.setRespawnLocation(loc);
            }
        }
    }

}