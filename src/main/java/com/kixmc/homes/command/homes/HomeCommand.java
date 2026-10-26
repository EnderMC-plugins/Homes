package com.kixmc.homes.command.homes;

import com.kixmc.homes.Homes;
import com.kixmc.homes.userdata.UserDataHandler;
import com.kixmc.homes.userdata.UserLookupUtil;
import com.kixmc.homes.util.WorldUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class HomeCommand implements CommandExecutor, Listener {

    private static final String DEFAULT_HOME_NAME = "home";
    private static final long CONFIRM_EXPIRY_TICKS = 160L;
    private static final String COOLDOWN_PERMISSION_PREFIX = "homes.cooldown.";

    private final Homes plugin;
    private final Map<UUID, String> confirmTpPlayers = new HashMap<>();
    // player -> time in millis when the next teleport is allowed
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return true;

        if (!(sender instanceof Player player)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.home.invalidSender"), sender);
            return true;
        }

        execute(player, args);
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPre(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigHandler().getSettings().getBoolean("alternateCommandExecutor")) return;

        String[] parts = event.getMessage().split(" ");
        if (!parts[0].equalsIgnoreCase("/home")) return;

        event.setCancelled(true);
        execute(event.getPlayer(), Arrays.copyOfRange(parts, 1, parts.length));
    }

    public void execute(Player player, String[] args) {
        if (!player.hasPermission("homes.home")) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.noPermission"), player);
            return;
        }

        String homeName;
        UUID ownerUUID;

        switch (args.length) {
            case 0 -> {
                homeName = DEFAULT_HOME_NAME;
                ownerUUID = player.getUniqueId();
            }
            case 1 -> {
                homeName = args[0].toLowerCase();
                ownerUUID = player.getUniqueId();
            }
            case 2 -> {
                if (!player.hasPermission("homes.home.other")) {
                    plugin.getLang().send(plugin.getLang().getFormatted("command.noPermission"), player);
                    return;
                }
                ownerUUID = UserLookupUtil.getUUIDFromUsername(plugin, args[0]);
                if (ownerUUID == null) {
                    plugin.getLang().send(plugin.getLang().getFormatted("command.home.unknownPlayer")
                            .replace("%arg1", args[0]), player);
                    return;
                }
                homeName = args[1].toLowerCase();
            }
            default -> {
                plugin.getLang().send(plugin.getLang().getFormatted("command.home.usage"), player);
                return;
            }
        }

        teleportToHome(homeName, player, ownerUUID);
    }

    private boolean canBypassWarmup(Player player) {
        return plugin.getConfigHandler().getSettings().getInt("warmup") == 0
                || player.isOp()
                || player.hasPermission("homes.warmup.bypass");
    }

    private void teleportToHome(String name, Player player, UUID ownerUUID) {
        UserDataHandler user = new UserDataHandler(plugin, ownerUUID);
        FileConfiguration data = user.getUserConfig();

        ConfigurationSection homesSection = data.getConfigurationSection("homes");
        Set<String> homes = homesSection == null ? Set.of() : homesSection.getKeys(false);

        if (!homes.contains(name)) {
            if (player.getUniqueId().equals(ownerUUID)) {
                plugin.getLang().send(plugin.getLang().getFormatted("command.home.invalidHome")
                        .replace("%arg1", name), player);
            } else {
                plugin.getLang().send(plugin.getLang().getFormatted("command.home.invalidHomeOther")
                        .replace("%arg1", name)
                        .replace("%arg2", data.getString("player.info.lastSeenAs")), player);
            }
            return;
        }

        long remainingCooldown = cooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remainingCooldown > 0) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.home.cooldown")
                    .replace("%arg1", String.valueOf((remainingCooldown + 999) / 1000)), player);
            return;
        }

        if (canBypassWarmup(player)) {
            attemptTeleport(name, player, ownerUUID, user);
            return;
        }

        if (plugin.getMoveListener().getWarmupPlayers().contains(player.getUniqueId())) return;

        int warmup = plugin.getConfigHandler().getSettings().getInt("warmup");
        plugin.getLang().send(plugin.getLang().getFormatted("command.home.warmup")
                .replace("%arg1", String.valueOf(warmup)), player);
        plugin.getMoveListener().getWarmupPlayers().add(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.getMoveListener().getWarmupPlayers().contains(player.getUniqueId())) return;
            if (attemptTeleport(name, player, ownerUUID, user)) {
                plugin.getMoveListener().getWarmupPlayers().remove(player.getUniqueId());
            }
        }, 20L * warmup);
    }

    private boolean attemptTeleport(String name, Player player, UUID ownerUUID, UserDataHandler user) {
        FileConfiguration data = user.getUserConfig();
        String path = "homes." + name;

        int x = data.getInt(path + ".x");
        int y = data.getInt(path + ".y");
        int z = data.getInt(path + ".z");
        float yaw = (float) data.getDouble(path + ".yaw");
        float pitch = (float) data.getDouble(path + ".pitch");
        String worldName = data.getString(path + ".world");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.home.invalidWorld")
                    .replace("%arg1", worldName), player);
            return false;
        }

        if (!WorldUtil.isWorldAllowed(plugin.getConfigHandler().getSettings(), worldName)) {
            plugin.getLang().send(plugin.getLang().getFormatted("command.disallowedHomeDestination")
                    .replace("%arg1", worldName), player);
            return false;
        }

        Location loc = new Location(world, x, y, z, yaw, pitch);

        if (requiresUnsafeConfirmation(name, player, ownerUUID, loc)) return false;

        loc.add(0.5, 0, 0.5);
        player.teleport(loc);
        sendTeleportMessage(name, player, ownerUUID, user, x, y, z, worldName);

        int cooldown = resolveCooldown(player);
        if (cooldown > 0) cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);
        return true;
    }

    private int resolveCooldown(Player player) {
        if (player.hasPermission("homes.cooldown.bypass")) return 0;

        // the lowest granted homes.cooldown.<seconds> wins
        int lowest = -1;
        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String node = perm.getPermission().toLowerCase();
            if (!perm.getValue() || !node.startsWith(COOLDOWN_PERMISSION_PREFIX)) continue;
            try {
                int seconds = Integer.parseInt(node.substring(COOLDOWN_PERMISSION_PREFIX.length()));
                if (lowest < 0 || seconds < lowest) lowest = seconds;
            } catch (NumberFormatException ignored) {
                // homes.cooldown.bypass
            }
        }

        return lowest < 0 ? plugin.getConfigHandler().getSettings().getInt("teleportCooldown") : lowest;
    }

    private boolean requiresUnsafeConfirmation(String name, Player player, UUID ownerUUID, Location loc) {
        if (!plugin.getConfigHandler().getSettings().getBoolean("confirmUnsafeTeleports")) return false;
        if (WorldUtil.checkIsSafeLocation(loc,
                plugin.getConfigHandler().getSettings().getBoolean("considerMidAirUnsafe"))) {
            return false;
        }

        String confirmKey = name + ownerUUID;
        if (confirmKey.equals(confirmTpPlayers.get(player.getUniqueId()))) {
            return false;
        }

        plugin.getLang().send(plugin.getLang().getFormatted("command.home.unsafeTeleport"), player);
        confirmTpPlayers.put(player.getUniqueId(), confirmKey);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> confirmTpPlayers.remove(player.getUniqueId()), CONFIRM_EXPIRY_TICKS);
        return true;
    }

    private void sendTeleportMessage(String name, Player player, UUID ownerUUID,
                                     UserDataHandler user, int x, int y, int z, String world) {
        String message;
        if (player.getUniqueId().equals(ownerUUID)) {
            String key = name.equals(DEFAULT_HOME_NAME)
                    ? "command.home.teleportedHome"
                    : "command.home.teleportedHomeNamed";
            message = plugin.getLang().getFormatted(key);
        } else {
            message = plugin.getLang().getFormatted("command.home.teleportedOther")
                    .replace("%arg6", user.getUserConfig().getString("player.info.lastSeenAs"));
        }

        plugin.getLang().send(message
                .replace("%arg1", name)
                .replace("%arg2", world)
                .replace("%arg3", String.valueOf(x))
                .replace("%arg4", String.valueOf(y))
                .replace("%arg5", String.valueOf(z)), player);
    }

}