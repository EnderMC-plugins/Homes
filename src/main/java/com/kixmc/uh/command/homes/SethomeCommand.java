package com.kixmc.uh.command.homes;

import com.kixmc.uh.UltimateHomes;
import com.kixmc.uh.userdata.UserDataHandler;
import com.kixmc.uh.util.WorldUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;

@RequiredArgsConstructor
public class SethomeCommand implements CommandExecutor, Listener {

    private static final String DEFAULT_HOME_NAME = "home";
    private static final String MAX_HOMES_PERMISSION_PREFIX = "ultimatehomes.sethome.max.";
    private static final int UNLIMITED_HOMES = Integer.MAX_VALUE;

    private final UltimateHomes plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return true;

        if (!(sender instanceof Player player)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.sethome.invalidSender"), sender);
            return true;
        }

        execute(player, args);
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPre(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return;

        String[] parts = event.getMessage().split(" ");
        if (!parts[0].equalsIgnoreCase("/sethome")) return;

        event.setCancelled(true);
        execute(event.getPlayer(), Arrays.copyOfRange(parts, 1, parts.length));
    }

    private void execute(Player player, String[] args) {
        if (!player.hasPermission("ultimatehomes.sethome")) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.noPermission"), player);
            return;
        }
        if (args.length > 1) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.sethome.usage"), player);
            return;
        }

        String currentWorld = player.getWorld().getName();
        if (!WorldUtil.isWorldAllowed(plugin.getConfigHandler().getSettings(), currentWorld)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.disallowedSethome")
                    .replace("%arg1", currentWorld), player);
            return;
        }

        if (plugin.getConfigHandler().getSettings().getBoolean("blockCreatingUnsafeSethomes")
                && !WorldUtil.checkIsSafeLocation(player.getLocation(),
                plugin.getConfigHandler().getSettings().getBoolean("considerMidAirUnsafe"))) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.sethome.unsafeLocation"), player);
            return;
        }

        boolean isDefault = args.length == 0;
        String name = isDefault ? DEFAULT_HOME_NAME : args[0].toLowerCase();
        int maxHomes = resolveMaxHomes(player, isDefault);

        createHome(player, name, maxHomes);
    }

    private int resolveMaxHomes(Player player, boolean isDefaultHome) {
        if (player.hasPermission("ultimatehomes.sethome.unlimited") || player.isOp()) {
            return UNLIMITED_HOMES;
        }

        int highest = -1;
        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String node = perm.getPermission().toLowerCase();
            if (!node.startsWith(MAX_HOMES_PERMISSION_PREFIX)) continue;
            try {
                highest = Math.max(highest,
                        Integer.parseInt(node.substring(MAX_HOMES_PERMISSION_PREFIX.length())));
            } catch (NumberFormatException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to parse max homes permission", ex);
            }
        }

        if (highest < 0) {
            return plugin.getConfigHandler().getSettings().getInt("defaultMaxHomes");
        }

        return isDefaultHome ? highest + 1 : highest;
    }

    private void createHome(Player player, String name, int maxHomes) {
        UserDataHandler user = new UserDataHandler(plugin, player.getUniqueId());
        FileConfiguration data = user.getUserConfig();

        ConfigurationSection homesSection = data.getConfigurationSection("homes");
        Set<String> homes = homesSection == null ? Set.of() : homesSection.getKeys(false);

        if (homes.size() >= maxHomes) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.sethome.noMoreHomes")
                    .replace("%arg1", String.valueOf(maxHomes)), player);
            return;
        }

        if (homes.contains(name) && !name.equalsIgnoreCase(DEFAULT_HOME_NAME)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.sethome.nameAlreadyExists")
                    .replace("%arg1", name), player);
            return;
        }

        if (!name.matches("[a-zA-Z0-9]+")) {
            String suggestion = name.replaceAll("[^a-zA-Z0-9]", "");
            if (suggestion.isEmpty()) {
                plugin.getLang().send(plugin.getLang()
                        .getFormatted("command.sethome.nonAlphanumericNoSuggestion"), player);
            } else {
                plugin.getLang().send(plugin.getLang().getFormatted("command.sethome.nonAlphanumeric")
                        .replace("%arg1", name).replace("%arg2", suggestion), player);
            }
            return;
        }

        Location loc = player.getLocation();
        String path = "homes." + name;
        data.set(path + ".x", loc.getBlockX());
        data.set(path + ".y", loc.getBlockY());
        data.set(path + ".z", loc.getBlockZ());
        data.set(path + ".yaw", loc.getYaw());
        data.set(path + ".pitch", loc.getPitch());
        data.set(path + ".world", loc.getWorld().getName());
        user.saveUserFile();

        String messageKey = name.equals(DEFAULT_HOME_NAME)
                ? "command.sethome.createdHome"
                : "command.sethome.createdNamedHome";
        plugin.getLang().send(plugin.getLang().getFormatted(messageKey)
                .replace("%arg1", name)
                .replace("%arg2", loc.getWorld().getName()), player);
    }

}