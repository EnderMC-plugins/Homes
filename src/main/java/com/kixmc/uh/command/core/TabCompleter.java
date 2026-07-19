package com.kixmc.uh.command.core;

import com.kixmc.uh.UltimateHomes;
import com.kixmc.uh.userdata.UserDataHandler;
import com.kixmc.uh.userdata.UserLookupUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TabCompleter implements org.bukkit.command.TabCompleter {

	private static final List<String> SUBCOMMANDS = List.of("reload", "deleteallhomesinworld", "deleteallhomesfromplayer");

	private final UltimateHomes plugin;

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (args.length == 0 || !(sender instanceof Player)) return null;

		Player player = (Player) sender;
		String partial = args[args.length - 1];

		switch (cmd.getLabel().toLowerCase()) {
			case "ultimatehomes":
			case "uh":
				return filterAndSort(partial, SUBCOMMANDS);
			case "homes":
				return args.length == 1
						? filterAndSort(partial, getOnlinePlayerNames())
						: null;
			case "sethome":
			case "home":
			case "delhome":
				return completeHomeCommand(player, args, partial);
			default:
				return null;
		}
	}

	private List<String> completeHomeCommand(Player player, String[] args, String partial) {
		if (args.length == 1) {
			List<String> candidates = new ArrayList<>(getHomeNames(player.getUniqueId()));
			if (player.hasPermission("ultimatehomes.home.other")
					|| player.hasPermission("ultimatehomes.homes.other")
					|| player.isOp()) {
				candidates.addAll(getOnlinePlayerNames());
			}
			return filterAndSort(partial, candidates);
		}

		if (args.length == 2 && !args[0].isEmpty()) {
			UUID targetId = UserLookupUtil.getUUIDFromUsername(plugin, args[0]);
			if (targetId != null) {
				return filterAndSort(partial, getHomeNames(targetId));
			}
		}

		return null;
	}

 private List<String> getHomeNames(UUID uuid) {
        ConfigurationSection homes = new UserDataHandler(plugin, uuid).getUserConfig().getConfigurationSection("homes");
        return homes == null ? Collections.emptyList() : new ArrayList<>(homes.getKeys(false));
    }

	private List<String> getOnlinePlayerNames() {
		List<String> names = new ArrayList<>();
		for (Player online : Bukkit.getOnlinePlayers()) {
			names.add(online.getName());
		}
		return names;
	}

	private List<String> filterAndSort(String partial, List<String> candidates) {
		List<String> completions = new ArrayList<>();
		StringUtil.copyPartialMatches(partial, candidates, completions);
		Collections.sort(completions);

		return completions;
	}

}