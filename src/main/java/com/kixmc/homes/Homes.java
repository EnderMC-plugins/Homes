package com.kixmc.homes;

import com.kixmc.homes.command.core.TabCompleter;
import com.kixmc.homes.command.homes.DelhomeCommand;
import com.kixmc.homes.command.homes.HomeCommand;
import com.kixmc.homes.command.homes.HomesAdminCommand;
import com.kixmc.homes.command.homes.HomesCommand;
import com.kixmc.homes.command.homes.SethomeCommand;
import com.kixmc.homes.config.ConfigHandler;
import com.kixmc.homes.gui.ChatPrompt;
import com.kixmc.homes.gui.MenuListener;
import com.kixmc.homes.listener.JoinListener;
import com.kixmc.homes.listener.MoveListener;
import com.kixmc.homes.listener.RespawnListener;
import com.kixmc.homes.userdata.UserDataHandler;
import com.kixmc.homes.util.Lang;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Homes extends JavaPlugin {

    private boolean potentialConflictDetected = false;
    private JoinListener joinListener;
    private MoveListener moveListener;
    private RespawnListener respawnListener;
    private ConfigHandler configHandler;
    private Lang lang;
    private ChatPrompt chatPrompt;
    // used by the /homes GUI
    private SethomeCommand sethomeCommand;
    private HomeCommand homeCommand;
    private DelhomeCommand delhomeCommand;

    public void onEnable() {

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "--------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + "       Homes by kixmc & herrlamatv");
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + "       Enabled on version " + this.getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "--------------------------------------");
        Bukkit.getConsoleSender().sendMessage(" ");

        loadFiles();
        joinListener = new JoinListener(this);
        moveListener = new MoveListener(this);
        respawnListener = new RespawnListener(this);
        chatPrompt = new ChatPrompt(this);

        getServer().getPluginManager().registerEvents(joinListener, this);
        getServer().getPluginManager().registerEvents(moveListener, this);
        getServer().getPluginManager().registerEvents(respawnListener, this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(chatPrompt, this);

        // reload support
        for (Player p : Bukkit.getOnlinePlayers()) new UserDataHandler(this, p.getUniqueId()).tryCreateUser(p);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            CommandMap commandMap = Bukkit.getServer().getCommandMap();
            if ((commandMap.getCommand("sethome") != null)
                    || (commandMap.getCommand("home") != null)
                    || (commandMap.getCommand("homes") != null)
                    || (commandMap.getCommand("delhome") != null)) {
                potentialConflictDetected = true;
            }

            TabCompleter tabCompleter = new TabCompleter(this);

            sethomeCommand = new SethomeCommand(this);
            homeCommand = new HomeCommand(this);
            HomesCommand homesCommand = new HomesCommand(this);
            delhomeCommand = new DelhomeCommand(this);

            // for preprocess alt command executor
            getServer().getPluginManager().registerEvents(sethomeCommand, this);
            getServer().getPluginManager().registerEvents(homeCommand, this);
            getServer().getPluginManager().registerEvents(homesCommand, this);
            getServer().getPluginManager().registerEvents(delhomeCommand, this);

            getCommand("sethome").setExecutor(sethomeCommand);
            getCommand("sethome").setTabCompleter(tabCompleter);

            getCommand("home").setExecutor(homeCommand);
            getCommand("home").setTabCompleter(tabCompleter);

            getCommand("homes").setExecutor(homesCommand);
            getCommand("homes").setTabCompleter(tabCompleter);

            getCommand("delhome").setExecutor(delhomeCommand);
            getCommand("delhome").setTabCompleter(tabCompleter);

            getCommand("homesadmin").setExecutor(new HomesAdminCommand(this));
            getCommand("homesadmin").setTabCompleter(tabCompleter);
        }, 1L); // wait for server to start ticking

    }

    public void onDisable() {}

    /**
     * (Re)loads config.yml and the language files, on enable and with /homesadmin reload.
     */
    public void loadFiles() {
        configHandler = new ConfigHandler(this);
        lang = new Lang(this);
    }

}
