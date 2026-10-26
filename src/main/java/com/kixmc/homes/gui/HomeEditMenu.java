package com.kixmc.homes.gui;

import com.kixmc.homes.Homes;
import com.kixmc.homes.userdata.UserDataHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edit one of the own homes (Q on a home in /homes): choose an icon or rename it.
 */
public class HomeEditMenu extends Menu {

    // two full pages
    private static final List<Material> ICONS = List.of(
            Material.RED_BED,
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD, Material.ELYTRA, Material.TOTEM_OF_UNDYING,
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT, Material.NETHERITE_INGOT,
            Material.NETHER_STAR, Material.DRAGON_EGG, Material.BEACON,
            Material.ENCHANTING_TABLE, Material.ANVIL, Material.BREWING_STAND, Material.CRAFTING_TABLE, Material.FURNACE,
            Material.CHEST, Material.ENDER_CHEST, Material.SHULKER_BOX,
            Material.GRASS_BLOCK, Material.OAK_LOG, Material.OAK_SAPLING, Material.WHEAT, Material.GOLDEN_APPLE, Material.CAKE,
            Material.FISHING_ROD, Material.WATER_BUCKET, Material.LAVA_BUCKET,
            Material.COMPASS, Material.CLOCK, Material.MAP, Material.ENDER_PEARL, Material.ENDER_EYE, Material.EXPERIENCE_BOTTLE,
            Material.REDSTONE, Material.TNT, Material.NETHERRACK, Material.END_STONE, Material.OBSIDIAN, Material.RESPAWN_ANCHOR,
            Material.CREEPER_HEAD, Material.OAK_BOAT, Material.MINECART);
    private static final int RENAME_SLOT = 48;

    private final String home;
    private final int homesPage;
    private final Map<Integer, Material> iconSlots;

    public HomeEditMenu(Homes plugin, Player player, String home, int page, int homesPage) {
        super(plugin, plugin.getLang().getFormatted("gui.editHome.title").replace("%arg1", home), page);
        this.home = home;
        this.homesPage = homesPage;

        ConfigurationSection homes = homesOf(player);
        Material currentIcon = homeIcon(homes, home);

        setItem(TOP_CENTER_SLOT, homeItem(homes, home));
        setItem(RENAME_SLOT, item(Material.NAME_TAG, lang("gui.editHome.rename"), lang("gui.editHome.renameHint")));
        setItem(BOTTOM_CENTER_SLOT, item(Material.OAK_DOOR, lang("gui.back")));

        iconSlots = fillContent(ICONS, icon -> renderIcon(icon, icon == currentIcon));
    }

    @Override
    protected void openPage(Player player, int page) {
        new HomeEditMenu(plugin, player, home, page, homesPage).open(player);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click) {
        if (slot == BOTTOM_CENTER_SLOT) {
            new HomesMenu(plugin, player, homesPage).open(player);
            return;
        }
        if (slot == RENAME_SLOT) {
            player.closeInventory();
            plugin.getChatPrompt().start(player, lang("gui.renameHome.prompt").replace("%arg2", home),
                    input -> rename(player, input),
                    () -> openPage(player, page));
            return;
        }

        Material icon = iconSlots.get(slot);
        if (icon == null) return;

        UserDataHandler user = new UserDataHandler(plugin, player.getUniqueId());
        if (!user.getUserConfig().contains("homes." + home)) {
            // deleted in the meantime
            new HomesMenu(plugin, player, homesPage).open(player);
            return;
        }

        user.getUserConfig().set("homes." + home + ".icon", icon.name());
        user.saveUserFile();
        openPage(player, page);
    }

    private void rename(Player player, String input) {
        String newName = input.toLowerCase();
        UserDataHandler user = new UserDataHandler(plugin, player.getUniqueId());
        FileConfiguration data = user.getUserConfig();
        ConfigurationSection homes = data.getConfigurationSection("homes");

        if (homes == null || !homes.contains(home)) {
            new HomesMenu(plugin, player, homesPage).open(player);
            return;
        }
        if (newName.equals(home)) {
            openPage(player, page);
            return;
        }
        if (homes.contains(newName)) {
            plugin.getLang().send(lang("command.sethome.nameAlreadyExists").replace("%arg1", newName), player);
            openPage(player, page);
            return;
        }
        if (!plugin.getSethomeCommand().isValidName(player, newName)) {
            openPage(player, page);
            return;
        }

        // rebuild the section so the renamed home keeps its place in the list
        Map<String, Object> renamed = new LinkedHashMap<>();
        for (String key : homes.getKeys(false)) {
            Object value = homes.isConfigurationSection(key) ? homes.getConfigurationSection(key).getValues(false) : homes.get(key);
            renamed.put(key.equals(home) ? newName : key, value);
        }
        data.createSection("homes", renamed);
        user.saveUserFile();

        plugin.getLang().send(lang("gui.renameHome.renamed").replace("%arg1", home).replace("%arg2", newName), player);
        new HomeEditMenu(plugin, player, newName, page, homesPage).open(player);
    }

    private ItemStack renderIcon(Material icon, boolean selected) {
        // no custom name, so the client shows the translated item name
        ItemStack item = item(icon, null, lang(selected ? "gui.editHome.selected" : "gui.editHome.select"));
        if (selected) {
            ItemMeta meta = item.getItemMeta();
            addGlint(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    // setEnchantmentGlintOverride only exists since 1.20.5, before that the lore line marks the selection
    private static void addGlint(ItemMeta meta) {
        try {
            ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class).invoke(meta, Boolean.TRUE);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private ConfigurationSection homesOf(Player player) {
        FileConfiguration data = new UserDataHandler(plugin, player.getUniqueId()).getUserConfig();
        ConfigurationSection homes = data.getConfigurationSection("homes");
        return homes != null ? homes : data.createSection("homes");
    }

}
