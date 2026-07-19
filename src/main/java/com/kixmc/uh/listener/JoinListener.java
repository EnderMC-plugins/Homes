package com.kixmc.uh.listener;

import com.kixmc.uh.UltimateHomes;
import com.kixmc.uh.userdata.UserDataHandler;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class JoinListener implements Listener {

    private final UltimateHomes plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UserDataHandler user = new UserDataHandler(plugin, e.getPlayer().getUniqueId());
        user.tryCreateUser(e.getPlayer());

        if (e.getPlayer().isOp()) {
            if (plugin.getConfigHandler().getSettings().getBoolean("showDetectedConflictWarning") && plugin.isPotentialConflictDetected()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    e.getPlayer().sendMessage("");
                    e.getPlayer().sendMessage(ChatColor.RED + "WARNING: UltimateHomes has detected a potential CONFLICT with Essentials or another plugin providing Home commands. If you experience problems using home commands, change 'alternateCommandExecutor' from 'false' to 'true' in settings.yml in plugins/UltimateHomes, then use /uh reload to update your setting change. If you continue to experience problems, please reach out on the support Discord: https://discord.gg/HKnDTRj");
                    e.getPlayer().sendMessage(ChatColor.GRAY + "If you are not experiencing problems with home commands, you can hide this warning by setting 'showDetectedConflictWarning' to false in settings.yml.");
                    e.getPlayer().sendMessage("");
                }, 160L);
            }
        }

    }

}