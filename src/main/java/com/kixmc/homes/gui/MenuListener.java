package com.kixmc.homes.gui;

import com.kixmc.homes.Homes;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

@RequiredArgsConstructor
public class MenuListener implements Listener {

    private final Homes plugin;

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu menu)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return; // outside or own inventory
        if (!(e.getWhoClicked() instanceof Player player)) return;

        // opening/closing inventories inside the click event isn't safe, so handle it next tick
        ClickType click = e.getClick();
        Bukkit.getScheduler().runTask(plugin, () -> menu.handleClick(player, slot, click));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

}
