package com.kixmc.homes.listener;

import com.kixmc.homes.Homes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.UUID;

@RequiredArgsConstructor
public class MoveListener implements Listener {

    private final Homes plugin;

    @Getter
    private final ArrayList<UUID> warmupPlayers = new ArrayList<>();

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (warmupPlayers.contains(e.getPlayer().getUniqueId())) {
            if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
                warmupPlayers.remove(e.getPlayer().getUniqueId());
                plugin.getLang().send(plugin.getLang().getFormatted("command.home.teleportationCancelled"), e.getPlayer());
            }
        }
    }

}