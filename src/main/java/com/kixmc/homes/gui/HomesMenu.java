package com.kixmc.homes.gui;

import com.kixmc.homes.Homes;
import com.kixmc.homes.command.homes.SethomeCommand;
import com.kixmc.homes.userdata.UserDataHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Own homes of a player (/homes). Left-click teleports, Q opens the edit menu, right-click three times deletes.
 */
public class HomesMenu extends Menu {

    private static final int DELETE_CLICKS = 3;

    private final ConfigurationSection homesSection;
    private final Map<Integer, String> homeSlots;
    private int pendingDeleteSlot = -1;
    private int deleteClicks;

    public HomesMenu(Homes plugin, Player player, int page) {
        super(plugin, plugin.getLang().getFormatted("gui.homes.title"), page);

        homesSection = new UserDataHandler(plugin, player.getUniqueId()).getUserConfig().getConfigurationSection("homes");
        List<String> homes = homesSection == null ? List.of() : new ArrayList<>(homesSection.getKeys(false));

        int maxHomes = plugin.getSethomeCommand().resolveMaxHomes(player, false);
        String max = maxHomes == SethomeCommand.UNLIMITED_HOMES ? "∞" : String.valueOf(maxHomes);
        setItem(TOP_CENTER_SLOT, item(Material.LIME_DYE, lang("gui.homes.create"),
                lang("gui.homes.count").replace("%arg1", String.valueOf(homes.size())).replace("%arg2", max),
                lang("gui.homes.createDescription"), lang("gui.homes.createHint")));
        setItem(BOTTOM_CENTER_SLOT, item(Material.BARRIER, lang("gui.close")));
        if (homes.isEmpty()) setItem(EMPTY_SLOT, item(Material.BARRIER, lang("gui.homes.noHomes")));

        homeSlots = fillContent(homes, home -> renderHome(home, DELETE_CLICKS));
    }

    @Override
    protected void openPage(Player player, int page) {
        new HomesMenu(plugin, player, page).open(player);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click) {
        if (slot == TOP_CENTER_SLOT) {
            player.closeInventory();
            if (!player.hasPermission("homes.sethome")) {
                plugin.getLang().send(lang("command.noPermission"), player);
                return;
            }
            plugin.getChatPrompt().start(player, lang("gui.createHome.prompt"),
                    input -> {
                        plugin.getSethomeCommand().execute(player, new String[]{input});
                        new HomesMenu(plugin, player, 0).open(player);
                    },
                    () -> new HomesMenu(plugin, player, 0).open(player));
            return;
        }
        if (slot == BOTTOM_CENTER_SLOT) {
            player.closeInventory();
            return;
        }

        String home = homeSlots.get(slot);
        if (home == null) return;

        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            new HomeEditMenu(plugin, player, home, 0, page).open(player);
        } else if (click.isLeftClick()) {
            player.closeInventory();
            plugin.getHomeCommand().execute(player, new String[]{home});
        } else if (click.isRightClick()) {
            confirmDelete(player, slot, home);
        }
    }

    private void confirmDelete(Player player, int slot, String home) {
        if (slot != pendingDeleteSlot) {
            // another home was right-clicked, start counting again
            if (pendingDeleteSlot != -1) setItem(pendingDeleteSlot, renderHome(homeSlots.get(pendingDeleteSlot), DELETE_CLICKS));
            pendingDeleteSlot = slot;
            deleteClicks = 0;
        }

        if (++deleteClicks < DELETE_CLICKS) {
            setItem(slot, renderHome(home, DELETE_CLICKS - deleteClicks));
            return;
        }

        plugin.getDelhomeCommand().execute(player, new String[]{home});
        openPage(player, page);
    }

    private ItemStack renderHome(String home, int remainingClicks) {
        String deleteLine = remainingClicks == DELETE_CLICKS
                ? lang("gui.homes.delete")
                : lang("gui.homes.deleteConfirm").replace("%arg1", String.valueOf(remainingClicks));
        return homeItem(homesSection, home, lang("gui.homeList.teleport"), lang("gui.homes.edit"), deleteLine);
    }

}
