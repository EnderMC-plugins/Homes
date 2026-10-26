package com.kixmc.homes.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.kixmc.homes.Homes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Lang {

    private static final List<String> BUNDLED_LANGUAGES = List.of("de", "en", "fr");
    private static final String FALLBACK_LANGUAGE = "en";

    private final Homes plugin;
    private static final char COLOR_CHAR = ChatColor.COLOR_CHAR;

    // &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final boolean supportsHex;

    // flattened json keys, e.g. "command.home.usage"
    private final Map<String, String> messages = new HashMap<>();

    public Lang(Homes plugin) {
        this.plugin = plugin;
        this.supportsHex = checkHexSupport();
        loadMessages();
    }

    public String getFormatted(String key) {
        String message = messages.get(key);
        if (message == null) return "missing lang key: " + key;
        if (message.isEmpty()) return "";
        return colorize(message.replace("%pre%", plugin.getConfigHandler().getSettings().getString("prefix", "")));
    }

    public void send(String message, CommandSender receiver) {
        if (message == null || message.isEmpty()) return;
        receiver.sendMessage(message);
    }

    public String colorize(String input) {
        if (input == null || input.isEmpty()) return "";

        String translated = ChatColor.translateAlternateColorCodes('&', input);
        if (supportsHex) translated = translateHexCodes(translated);

        return translated;
    }

    private void loadMessages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        for (String language : BUNDLED_LANGUAGES) {
            if (!new File(langFolder, language + ".json").exists()) plugin.saveResource("lang/" + language + ".json", false);
        }

        String language = plugin.getConfigHandler().getSettings().getString("language", FALLBACK_LANGUAGE).toLowerCase();
        File langFile = new File(langFolder, language + ".json");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file lang/" + language + ".json not found, falling back to " + FALLBACK_LANGUAGE);
            language = FALLBACK_LANGUAGE;
            langFile = new File(langFolder, language + ".json");
        }

        // bundled messages first so keys missing from an edited file still resolve
        InputStream bundled = plugin.getResource("lang/" + language + ".json");
        if (bundled == null) bundled = plugin.getResource("lang/" + FALLBACK_LANGUAGE + ".json");
        try (Reader reader = new InputStreamReader(bundled, StandardCharsets.UTF_8)) {
            readMessages(reader);
        } catch (IOException | JsonParseException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load bundled language file", ex);
        }

        try (Reader reader = Files.newBufferedReader(langFile.toPath(), StandardCharsets.UTF_8)) {
            readMessages(reader);
        } catch (IOException | JsonParseException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load lang/" + langFile.getName() + ", using default messages", ex);
        }
    }

    private void readMessages(Reader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (root.isJsonObject()) flatten("", root.getAsJsonObject());
    }

    private void flatten(String path, JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) flatten(key, value.getAsJsonObject());
            else if (value.isJsonPrimitive()) messages.put(key, value.getAsString());
        }
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
