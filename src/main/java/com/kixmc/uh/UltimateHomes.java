package com.kixmc.uh;

import com.kixmc.uh.command.core.TabCompleter;
import com.kixmc.uh.command.core.UltimateHomesCommand;
import com.kixmc.uh.command.homes.DelhomeCommand;
import com.kixmc.uh.command.homes.HomeCommand;
import com.kixmc.uh.command.homes.HomesCommand;
import com.kixmc.uh.command.homes.SethomeCommand;
import com.kixmc.uh.config.ConfigHandler;
import com.kixmc.uh.listener.JoinListener;
import com.kixmc.uh.listener.MoveListener;
import com.kixmc.uh.listener.RespawnListener;
import com.kixmc.uh.userdata.UserDataHandler;
import com.kixmc.uh.util.Lang;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class UltimateHomes extends JavaPlugin {

    private boolean potentialConflictDetected = false;
    private JoinListener joinListener;
    private MoveListener moveListener;
    private RespawnListener respawnListener;
    private ConfigHandler configHandler;
    private Lang lang;

    public void onEnable() {

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "--------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + "       UltimateHomes by kixmc");
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + "       Enabled on version " + this.getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "--------------------------------------");
        Bukkit.getConsoleSender().sendMessage(" ");

        configHandler = new ConfigHandler(this);
        lang = new Lang(this);
        joinListener = new JoinListener(this);
        moveListener = new MoveListener(this);
        respawnListener = new RespawnListener(this);

        getServer().getPluginManager().registerEvents(joinListener, this);
        getServer().getPluginManager().registerEvents(moveListener, this);
        getServer().getPluginManager().registerEvents(respawnListener, this);

        new Metrics(this, 3928);

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

            SethomeCommand sethomeCommand = new SethomeCommand(this);
            HomeCommand homeCommand = new HomeCommand(this);
            HomesCommand homesCommand = new HomesCommand(this);
            DelhomeCommand delhomeCommand = new DelhomeCommand(this);
            UltimateHomesCommand ultimateHomesCommand = new UltimateHomesCommand(this);

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

            getCommand("ultimatehomes").setExecutor(ultimateHomesCommand);
            getCommand("ultimatehomes").setTabCompleter(tabCompleter);
        }, 1L); // wait for server to start ticking

    }

    public void onDisable() {}

}