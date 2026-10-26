package com.kixmc.homes.gui;

import com.kixmc.homes.Homes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Paged 6 row chest menu with a lime glass border. The 28 inner slots hold the page content.
 */
public abstract class Menu implements InventoryHolder {

    private static final int ROWS = 6;
    protected static final int TOP_CENTER_SLOT = 4;
    protected static final int BOTTOM_CENTER_SLOT = 49;
    protected static final int EMPTY_SLOT = 22;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int[] CONTENT_SLOTS = IntStream.range(0, ROWS * 9).filter(slot -> !isBorder(slot)).toArray();

    protected final Homes plugin;
    private final Inventory inventory;
    protected int page;
    private boolean hasNextPage;

    protected Menu(Homes plugin, String title, int page) {
        this.plugin = plugin;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, ROWS * 9, text(title));

        ItemStack border = item(Material.LIME_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isBorder(slot)) inventory.setItem(slot, border);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void handleClick(Player player, int slot, ClickType click) {
        if (slot == PREVIOUS_PAGE_SLOT && page > 0) openPage(player, page - 1);
        else if (slot == NEXT_PAGE_SLOT && hasNextPage) openPage(player, page + 1);
        else onClick(player, slot, click);
    }

    protected abstract void openPage(Player player, int page);

    protected abstract void onClick(Player player, int slot, ClickType click);

    /**
     * Renders the entries of the current page into the content slots and adds the page arrows.
     * Returns which entry ended up in which slot.
     */
    protected <T> Map<Integer, T> fillContent(List<T> entries, Function<T, ItemStack> render) {
        int perPage = CONTENT_SLOTS.length;
        // e.g. after the last entry of the last page was deleted
        page = Math.max(0, Math.min(page, (entries.size() - 1) / perPage));

        Map<Integer, T> slots = new HashMap<>();
        int from = page * perPage;
        for (int i = 0; i < perPage && from + i < entries.size(); i++) {
            T entry = entries.get(from + i);
            inventory.setItem(CONTENT_SLOTS[i], render.apply(entry));
            slots.put(CONTENT_SLOTS[i], entry);
        }

        hasNextPage = from + perPage < entries.size();
        if (page > 0) inventory.setItem(PREVIOUS_PAGE_SLOT, item(Material.ARROW, lang("gui.previousPage")));
        if (hasNextPage) inventory.setItem(NEXT_PAGE_SLOT, item(Material.ARROW, lang("gui.nextPage")));
        return slots;
    }

    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected String lang(String key) {
        return plugin.getLang().getFormatted(key);
    }

    /**
     * Icon chosen in the edit menu, a red bed if none was chosen.
     */
    protected static Material homeIcon(ConfigurationSection homes, String name) {
        String iconName = homes.getString(name + ".icon");
        Material icon = iconName == null ? null : Material.matchMaterial(iconName);
        return icon != null && icon.isItem() ? icon : Material.RED_BED;
    }

    /**
     * Home icon with the home's name, world and position, followed by the given action lines.
     */
    protected ItemStack homeItem(ConfigurationSection homes, String name, String... actions) {
        String[] lore = new String[actions.length + 3];
        lore[0] = lang("gui.homeList.world").replace("%arg1", homes.getString(name + ".world", "?"));
        lore[1] = lang("gui.homeList.coordinates")
                .replace("%arg1", String.valueOf(homes.getInt(name + ".x")))
                .replace("%arg2", String.valueOf(homes.getInt(name + ".y")))
                .replace("%arg3", String.valueOf(homes.getInt(name + ".z")));
        lore[2] = "";
        System.arraycopy(actions, 0, lore, 3, actions.length);
        return item(homeIcon(homes, name), lang("gui.homeList.home").replace("%arg1", name), lore);
    }

    /**
     * @param name custom name, or null to keep the item's translated vanilla name
     */
    protected static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.displayName(text(name));
        if (lore.length > 0) meta.lore(Arrays.stream(lore).map(Menu::text).toList());
        // e.g. attack damage on swords
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    protected static ItemStack head(UUID owner, String name, String... lore) {
        ItemStack item = item(Material.PLAYER_HEAD, name, lore);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        item.setItemMeta(meta);
        return item;
    }

    // lang strings are already colorized with § codes
    private static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    private static boolean isBorder(int slot) {
        int row = slot / 9;
        int column = slot % 9;
        return row == 0 || row == ROWS - 1 || column == 0 || column == 8;
    }

}
