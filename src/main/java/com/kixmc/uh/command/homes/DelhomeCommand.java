package com.kixmc.uh.command.homes;

import com.kixmc.uh.UltimateHomes;
import com.kixmc.uh.userdata.UserDataHandler;
import com.kixmc.uh.userdata.UserLookupUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.UUID;

@RequiredArgsConstructor
public class DelhomeCommand implements CommandExecutor, Listener {

    private static final String DEFAULT_HOME_NAME = "home";

    private final UltimateHomes plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return true;

        if (!(sender instanceof Player player)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.delhome.invalidSender"), sender);
            return true;
        }

        execute(player, args);
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPre(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return;

        String[] parts = event.getMessage().split(" ");
        if (!parts[0].equalsIgnoreCase("/delhome")) return;

        event.setCancelled(true);
        execute(event.getPlayer(), Arrays.copyOfRange(parts, 1, parts.length));
    }

    private void execute(Player player, String[] args) {
        if (!player.hasPermission("ultimatehomes.delhome")) {
            sendLang(player, "command.noPermission");
            return;
        }

        switch (args.length) {
            case 0:
                deleteHome(player, player.getUniqueId(), DEFAULT_HOME_NAME);
                break;
            case 1:
                deleteHome(player, player.getUniqueId(), args[0]);
                break;
            case 2:
                deleteOtherPlayersHome(player, args[0], args[1]);
                break;
            default:
                sendLang(player, "command.delhome.usage");
                break;
        }
    }

    private void deleteOtherPlayersHome(Player player, String targetName, String homeName) {
        if (!player.hasPermission("ultimatehomes.delhome.other")) {
            sendLang(player, "command.noPermission");
            return;
        }

        UUID targetId = UserLookupUtil.getUUIDFromUsername(plugin, targetName);
        if (targetId == null) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.delhome.unknownPlayer")
                    .replace("%arg1", targetName), player);
            return;
        }

        deleteHome(player, targetId, homeName);
    }

    private void deleteHome(Player player, UUID homeOwnerId, String name) {
        UserDataHandler user = new UserDataHandler(plugin, homeOwnerId);
        boolean isOwn = player.getUniqueId().equals(homeOwnerId);
        ConfigurationSection homes = user.getUserConfig().getConfigurationSection("homes");

        if (homes == null || !homes.contains(name)) {
            sendResult(player, user, isOwn,
                    "command.delhome.invalidHome", "command.delhome.invalidHomeOther", name);
            return;
        }

        homes.set(name, null);
        user.saveUserFile();

        sendResult(player, user, isOwn,
                "command.delhome.deleted", "command.delhome.deletedOther", name);
    }

    private void sendResult(Player player, UserDataHandler user, boolean isOwn,
                            String ownKey, String otherKey, String homeName) {
        String message = plugin.getLang().getFormatted(isOwn ? ownKey : otherKey)
                .replace("%arg1", homeName);
        if (!isOwn) {
            message = message.replace("%arg2",
                    user.getUserConfig().getString("player.info.lastSeenAs", "unknown"));
        }
        plugin.getLang().send(message, player);
    }

    private void sendLang(Player player, String key) {
        plugin.getLang().send(plugin.getLang().getFormatted(key), player);
    }

}