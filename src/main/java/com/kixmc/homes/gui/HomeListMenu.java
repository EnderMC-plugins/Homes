package com.kixmc.homes.gui;

import com.kixmc.homes.Homes;
import com.kixmc.homes.userdata.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Homes of one player. Left-click teleports, shift + right-click deletes.
 */
public class HomeListMenu extends Menu {

    private final UUID targetId;
    private final String targetName;
    private final int playerListPage;
    private final Map<Integer, String> homeSlots;

    public HomeListMenu(Homes plugin, UUID targetId, String targetName, int page, int playerListPage) {
        super(plugin, plugin.getLang().getFormatted("gui.homeList.title").replace("%arg1", targetName), page);
        this.targetId = targetId;
        this.targetName = targetName;
        this.playerListPage = playerListPage;

        ConfigurationSection homesSection = new UserDataHandler(plugin, targetId).getUserConfig().getConfigurationSection("homes");
        List<String> homes = homesSection == null ? List.of() : new ArrayList<>(homesSection.getKeys(false));

        String nameKey = Bukkit.getPlayer(targetId) != null ? "gui.playerList.online" : "gui.playerList.offline";
        setItem(TOP_CENTER_SLOT, head(targetId, lang(nameKey).replace("%arg1", targetName),
                lang("gui.playerList.homeCount").replace("%arg1", String.valueOf(homes.size()))));
        setItem(BOTTOM_CENTER_SLOT, item(Material.OAK_DOOR, lang("gui.back")));
        if (homes.isEmpty()) setItem(EMPTY_SLOT, item(Material.BARRIER, lang("gui.homeList.noHomes")));

        homeSlots = fillContent(homes, home -> homeItem(homesSection, home,
                lang("gui.homeList.teleport"), lang("gui.homeList.delete")));
    }

    @Override
    protected void openPage(Player player, int page) {
        new HomeListMenu(plugin, targetId, targetName, page, playerListPage).open(player);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click) {
        if (slot == BOTTOM_CENTER_SLOT) {
            new PlayerListMenu(plugin, playerListPage).open(player);
            return;
        }

        String home = homeSlots.get(slot);
        if (home == null) return;

        if (click.isLeftClick()) teleport(player, home);
        else if (click == ClickType.SHIFT_RIGHT) delete(player, home);
    }

    private void teleport(Player player, String home) {
        FileConfiguration data = new UserDataHandler(plugin, targetId).getUserConfig();
        String path = "homes." + home;
        if (!data.contains(path)) {
            // deleted in the meantime
            openPage(player, page);
            return;
        }

        String worldName = data.getString(path + ".world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLang().send(lang("command.home.invalidWorld"), player);
            return;
        }

        int x = data.getInt(path + ".x");
        int y = data.getInt(path + ".y");
        int z = data.getInt(path + ".z");
        float yaw = (float) data.getDouble(path + ".yaw");
        float pitch = (float) data.getDouble(path + ".pitch");

        player.closeInventory();
        player.teleport(new Location(world, x + 0.5, y, z + 0.5, yaw, pitch));
        plugin.getLang().send(lang("command.home.teleportedOther")
                .replace("%arg6", targetName)
                .replace("%arg1", home)
                .replace("%arg2", worldName)
                .replace("%arg3", String.valueOf(x))
                .replace("%arg4", String.valueOf(y))
                .replace("%arg5", String.valueOf(z)), player);
    }

    private void delete(Player player, String home) {
        UserDataHandler user = new UserDataHandler(plugin, targetId);
        ConfigurationSection homes = user.getUserConfig().getConfigurationSection("homes");
        if (homes != null && homes.contains(home)) {
            homes.set(home, null);
            user.saveUserFile();
            plugin.getLang().send(lang("command.delhome.deletedOther")
                    .replace("%arg1", home)
                    .replace("%arg2", targetName), player);
        }

        openPage(player, page);
    }

}
