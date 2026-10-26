package com.kixmc.homes.command.homes;

import com.kixmc.homes.Homes;
import com.kixmc.homes.gui.PlayerListMenu;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class HomesAdminCommand implements CommandExecutor {

    private final Homes plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("homes.admin")) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.noPermission"), sender);
            return true;
        }

        // reload also works from the console
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.loadFiles();
            plugin.getLang().send(plugin.getLang().getFormatted("command.homesadmin.reloaded"), sender);
            return true;
        }
        if (args.length > 0) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.homesadmin.usage"), sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.homesadmin.invalidSender"), sender);
            return true;
        }

        new PlayerListMenu(plugin, 0).open(player);
        return true;
    }

}
