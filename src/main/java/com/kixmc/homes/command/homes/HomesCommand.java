package com.kixmc.homes.command.homes;

import com.kixmc.homes.Homes;
import com.kixmc.homes.gui.HomesMenu;
import com.kixmc.homes.userdata.UserDataHandler;
import com.kixmc.homes.userdata.UserLookupUtil;
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
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class HomesCommand implements CommandExecutor, Listener {

    private final Homes plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return true;

        if (!(sender instanceof Player player)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.homes.invalidSender"), sender);
            return true;
        }

        execute(player, args);
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPre(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return;

        String[] parts = event.getMessage().split(" ");
        if (!parts[0].equalsIgnoreCase("/homes")) return;

        event.setCancelled(true);
        execute(event.getPlayer(), Arrays.copyOfRange(parts, 1, parts.length));
    }

    private void execute(Player player, String[] args) {
        if (!player.hasPermission("homes.homes")) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.noPermission"), player);
            return;
        }

        switch (args.length) {
            case 0 -> {
                if (plugin.getConfigHandler().getSettings().getBoolean("homesGui")) new HomesMenu(plugin, player, 0).open(player);
                else listOwnHomes(player);
            }
            case 1 -> listOtherHomes(player, args[0]);
            default -> plugin.getLang().send(plugin.getLang().getFormatted("command.homes.usage"), player);
        }
    }

    private void listOwnHomes(Player player) {
        UserDataHandler user = new UserDataHandler(plugin, player.getUniqueId());
        sendHomeList(player, user, true, player.getName());
    }

    private void listOtherHomes(Player player, String targetName) {
        if (!player.hasPermission("homes.homes.other")) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.noPermission"), player);
            return;
        }

        UUID targetId = UserLookupUtil.getUUIDFromUsername(plugin, targetName);
        if (targetId == null) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.homes.unknownPlayer")
                    .replace("%arg1", targetName), player);
            return;
        }

        UserDataHandler other = new UserDataHandler(plugin, targetId);
        sendHomeList(player, other, false, targetName);
    }

    private void sendHomeList(Player requester, UserDataHandler user, boolean isOwn, String targetName) {
        ConfigurationSection homesSection = user.getUserConfig().getConfigurationSection("homes");
        Set<String> homes = homesSection == null ? java.util.Collections.emptySet() : homesSection.getKeys(false);

        if (homes.isEmpty()) {
            if (isOwn) {
                plugin.getLang().send(plugin.getLang().getFormatted("command.homes.noHomes"), requester);
            } else {
                plugin.getLang().send(plugin.getLang().getFormatted("command.homes.noHomesOther")
                        .replace("%arg1", targetName), requester);
            }
            return;
        }

        String color = plugin.getLang().getFormatted("command.homes.homeListColor");
        String separator = plugin.getLang().getFormatted("command.homes.homeListSeparator");

        StringBuilder list = new StringBuilder();
        int i = 0;
        int size = homes.size();
        for (String name : homes) {
            list.append(color).append(name);
            if (++i < size) list.append(separator);
        }

        if (isOwn) {
            requester.sendMessage(plugin.getLang().getFormatted("command.homes.homes")
                    .replace("%arg1", String.valueOf(size))
                    .replace("%arg2", list.toString()));
        } else {
            plugin.getLang().send(plugin.getLang().getFormatted("command.homes.homesOther")
                            .replace("%arg1", targetName)
                            .replace("%arg2", String.valueOf(size))
                            .replace("%arg3", list.toString()),
                    requester);
        }
    }

}