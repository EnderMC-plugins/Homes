package com.kixmc.homes.gui;

import com.kixmc.homes.Homes;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Reads the next chat message of a player, e.g. the name of a new home in the /homes GUI.
 */
@RequiredArgsConstructor
public class ChatPrompt implements Listener {

    private static final long TIMEOUT_TICKS = 20L * 60;

    private final Homes plugin;
    // chat events are async
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    /**
     * Sends the prompt (%arg1 is replaced with the cancel word) and passes the next chat message to onInput.
     * Typing the cancel word runs onCancel, after a minute without input the prompt expires.
     */
    public void start(Player player, String prompt, Consumer<String> onInput, Runnable onCancel) {
        Pending previous = pending.remove(player.getUniqueId());
        if (previous != null) previous.timeout().cancel();

        plugin.getLang().send(prompt.replace("%arg1", cancelWord()), player);
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(player.getUniqueId()) != null) {
                plugin.getLang().send(plugin.getLang().getFormatted("gui.prompt.cancelled"), player);
            }
        }, TIMEOUT_TICKS);
        pending.put(player.getUniqueId(), new Pending(timeout, onInput, onCancel));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Pending prompt = pending.remove(e.getPlayer().getUniqueId());
        if (prompt == null) return;

        e.setCancelled(true);
        prompt.timeout().cancel();

        String input = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            if (input.equalsIgnoreCase(cancelWord())) {
                plugin.getLang().send(plugin.getLang().getFormatted("gui.prompt.cancelled"), player);
                prompt.onCancel().run();
            } else {
                prompt.onInput().accept(input);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Pending prompt = pending.remove(e.getPlayer().getUniqueId());
        if (prompt != null) prompt.timeout().cancel();
    }

    private String cancelWord() {
        return plugin.getLang().getFormatted("gui.prompt.cancelWord");
    }

    private record Pending(BukkitTask timeout, Consumer<String> onInput, Runnable onCancel) {}

}
