package com.kixmc.homes.gui;

import com.kixmc.homes.Homes;
import com.kixmc.homes.userdata.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * All players with a playerdata file, online players first.
 */
public class PlayerListMenu extends Menu {

    private final Map<Integer, PlayerEntry> playerSlots;

    public PlayerListMenu(Homes plugin, int page) {
        super(plugin, plugin.getLang().getFormatted("gui.playerList.title"), page);

        List<PlayerEntry> players = loadPlayers();
        long online = players.stream().filter(PlayerEntry::online).count();

        setItem(TOP_CENTER_SLOT, item(Material.BOOK, lang("gui.playerList.info"),
                lang("gui.playerList.infoOnline").replace("%arg1", String.valueOf(online)),
                lang("gui.playerList.infoOffline").replace("%arg1", String.valueOf(players.size() - online))));
        setItem(BOTTOM_CENTER_SLOT, item(Material.BARRIER, lang("gui.close")));

        playerSlots = fillContent(players, this::renderPlayer);
    }

    @Override
    protected void openPage(Player player, int page) {
        new PlayerListMenu(plugin, page).open(player);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click) {
        if (slot == BOTTOM_CENTER_SLOT) {
            player.closeInventory();
            return;
        }

        PlayerEntry target = playerSlots.get(slot);
        if (target != null) new HomeListMenu(plugin, target.uuid(), target.name(), 0, page).open(player);
    }

    private ItemStack renderPlayer(PlayerEntry entry) {
        String nameKey = entry.online() ? "gui.playerList.online" : "gui.playerList.offline";
        return head(entry.uuid(), lang(nameKey).replace("%arg1", entry.name()),
                lang("gui.playerList.homeCount").replace("%arg1", String.valueOf(entry.homeCount())),
                lang("gui.playerList.openHomes"));
    }

    private List<PlayerEntry> loadPlayers() {
        File[] files = new File(plugin.getDataFolder(), "playerdata").listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null) return List.of();

        List<PlayerEntry> players = new ArrayList<>();
        for (File file : files) {
            UUID uuid;
            try {
                uuid = UUID.fromString(file.getName().substring(0, file.getName().length() - 4));
            } catch (IllegalArgumentException ex) {
                continue;
            }

            FileConfiguration data = new UserDataHandler(plugin, uuid).getUserConfig();
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            String name = onlinePlayer != null
                    ? onlinePlayer.getName()
                    : data.getString("player.info.lastSeenAs", uuid.toString());
            ConfigurationSection homes = data.getConfigurationSection("homes");

            players.add(new PlayerEntry(uuid, name, onlinePlayer != null, homes == null ? 0 : homes.getKeys(false).size()));
        }

        players.sort(Comparator.comparing((PlayerEntry entry) -> !entry.online())
                .thenComparing(PlayerEntry::name, String.CASE_INSENSITIVE_ORDER));
        return players;
    }

    private record PlayerEntry(UUID uuid, String name, boolean online, int homeCount) {}

}
