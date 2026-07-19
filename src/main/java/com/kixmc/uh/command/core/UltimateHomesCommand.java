package com.kixmc.uh.command.core;

import com.kixmc.uh.UltimateHomes;
import com.kixmc.uh.userdata.UserDataHandler;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
public class UltimateHomesCommand implements CommandExecutor {

    private final UltimateHomes plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args.length > 2) {
            sendOverview(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "help" -> {
                plugin.getConfigHandler().getLang().getStringList("command.ultimateHomes.help").forEach(line -> plugin.getLang().sendFormatted(line, sender, false));
                yield true;
            }
            case "reload" -> handleReload(sender);
            case "deleteallhomesfromplayer" -> handleDeleteAllHomesFromPlayer(sender, args);
            case "deleteallhomesinworld" -> handleDeleteAllHomesInWorld(sender, args);
            default -> {
                sendOverview(sender);
                yield true;
            }
        };
    }

    private void sendOverview(CommandSender sender) {
        plugin.getConfigHandler().getLang().getStringList("command.ultimateHomes.overview").forEach(line -> plugin.getLang().sendFormatted(line, sender, false));
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasPermission(sender, "ultimatehomes.reload")) return true;

        plugin.getConfigHandler().reload();
        plugin.getLang().send(plugin.getLang().getFormatted("command.reloaded"), sender);
        return true;
    }

    private boolean handleDeleteAllHomesFromPlayer(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ultimatehomes.deleteallhomes")) return true;

        if (!plugin.getConfigHandler().getSettings().getBoolean("enableDeleteAllHomesFromPlayerAdminCommand")) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.disabled"), sender);
            return true;
        }
        if (args.length != 2) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.deleteAllHomesFromPlayer.usage"), sender);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UserDataHandler user = new UserDataHandler(plugin, target.getUniqueId());
        user.getUserConfig().set("homes", null);
        user.getUserConfig().createSection("homes");
        user.saveUserFile();

        sender.sendMessage(ChatColor.GREEN + "Successfully deleted all of "
                + target.getName() + "'s sethomes (" + target.getUniqueId() + ")");
        plugin.getLang().send(plugin.getLang().getFormatted("command.deleteAllHomesFromPlayer.deleted").replace("%arg1%", target.getName()).replace("%arg2%", target.getUniqueId().toString()), sender);
        return true;
    }

    private boolean handleDeleteAllHomesInWorld(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ultimatehomes.deleteallhomesinworld")) return true;

        if (!plugin.getConfigHandler().getSettings().getBoolean("enableDeleteAllHomesInWorldAdminCommand")) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.disabled"), sender);
            return true;
        }
        if (args.length != 2) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.deleteAllHomesInWorld.usage"), sender);
            return true;
        }

        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.invalidWorld").replace("%arg1%", args[1]), sender);
            return true;
        }

        File[] userFiles = new File(plugin.getDataFolder(), "playerdata").listFiles();
        if (userFiles == null) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.noData"), sender);
            return true;
        }

        int deleted = 0;
        for (File userFile : userFiles) {
            deleted += deleteHomesInWorld(userFile, world.getName());
        }

        plugin.getLang().send(plugin.getLang().getFormatted("command.deleteAllHomesInWorld.deleted").replace("%arg1", String.valueOf(deleted)).replace("%arg2", world.getName()), sender);
        return true;
    }

    private int deleteHomesInWorld(File userFile, String worldName) {
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
        ConfigurationSection homes = userConfig.getConfigurationSection("homes");
        if (homes == null) return 0;

        int deleted = 0;
        for (String homeName : homes.getKeys(false)) {
            if (worldName.equalsIgnoreCase(homes.getString(homeName + ".world"))) {
                homes.set(homeName, null);
                deleted++;
            }
        }

        if (deleted > 0) {
            try {
                userConfig.save(userFile);
            } catch (IOException e) {
                plugin.getLogger().severe(
                        "Failed to save user file " + userFile.getName() + ": " + e.getMessage());
            }
        }
        return deleted;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        plugin.getLang().send(plugin.getLang().getFormatted("command.noPermission"), sender);
        return false;
    }

}