package com.kixmc.homes.listener;

import com.kixmc.homes.Homes;
import com.kixmc.homes.userdata.UserDataHandler;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class JoinListener implements Listener {

    private final Homes plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UserDataHandler user = new UserDataHandler(plugin, e.getPlayer().getUniqueId());
        user.tryCreateUser(e.getPlayer());

        if (e.getPlayer().isOp()) {
            if (plugin.getConfigHandler().getSettings().getBoolean("showDetectedConflictWarning") && plugin.isPotentialConflictDetected()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    e.getPlayer().sendMessage("");
                    plugin.getLang().send(plugin.getLang().getFormatted("conflictWarning.message"), e.getPlayer());
                    plugin.getLang().send(plugin.getLang().getFormatted("conflictWarning.hint"), e.getPlayer());
                    e.getPlayer().sendMessage("");
                }, 160L);
            }
        }

    }

}