package fr.vyraah.oneblock.managers;

import fr.vyraah.oneblock.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.PluginManager;

public class EventManager {
    public PluginManager pm;

    public Main plugin;
    public EventManager(Main plugin) {
        this.plugin = plugin;
        pm = Bukkit.getPluginManager();
    }

    public void registersEvents() {
        // pm.registerEvents(new EventName(), plugin); <== EXEMPLE
    }
}
