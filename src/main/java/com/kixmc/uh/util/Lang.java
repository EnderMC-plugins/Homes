package com.kixmc.uh.util;

import com.kixmc.uh.UltimateHomes;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;
import java.util.regex.Pattern;

public class Lang {

    @Getter
    private final UltimateHomes plugin;
    public static final char COLOR_CHAR = ChatColor.COLOR_CHAR;

    // &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final boolean supportsHex;

    public Lang(UltimateHomes plugin) {
        this.plugin = plugin;
        this.supportsHex = checkHexSupport();
    }

    public String getFormatted(String key) {
        if (plugin.getConfigHandler().getLang().getString(key) == null) return "null lang.yml key: " + key;
        if (plugin.getConfigHandler().getLang().getString(key).isEmpty()) return "";
        return colorize(plugin.getConfigHandler().getLang().getString(key).replace("%pre%", plugin.getConfigHandler().getLang().getString("prefix")));
    }

    public void send(String message, CommandSender receiver) {
        if (message == null || message.isEmpty()) return;
        receiver.sendMessage(message);
    }

    public void sendFormatted(String message, CommandSender receiver, boolean ignoreEmpty) {
        if (message == null) return;
        if(ignoreEmpty && message.isEmpty()) return;
        receiver.sendMessage(colorize(message.replace("%pre%", plugin.getConfigHandler().getLang().getString("prefix"))));
    }

    public String colorize(String input) {
        if (input == null || input.isEmpty()) return "";

        String translated = ChatColor.translateAlternateColorCodes('&', input);
        if (supportsHex) translated = translateHexCodes(translated);

        return translated;
    }

    private static String translateHexCodes(String message) {
        return HEX_PATTERN.matcher(message).replaceAll(match -> {
            String hexCode = match.group(1);
            StringBuilder builder = new StringBuilder();

            builder.append(COLOR_CHAR).append('x');
            for (char c : hexCode.toCharArray()) builder.append(COLOR_CHAR).append(c);

            return builder.toString();
        });
    }

    private static boolean checkHexSupport() {
        try {
            String[] parts = Bukkit.getMinecraftVersion().split("\\.");
            if (parts.length < 2) return false;
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);

            // if major version is > 1 it's the new mojang version scheme
            if (major > 1) return true;

            // if major version is 1 check if minor is 16 or greater for hex support
            return minor >= 16;

        } catch (NumberFormatException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to check server version for hex support", ex);
            return false;
        }
    }

}