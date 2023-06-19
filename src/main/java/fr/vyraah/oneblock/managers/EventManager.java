package fr.vyraah.oneblock.managers;

import fr.vyraah.oneblock.Listeners.GenericEvents;
import fr.vyraah.oneblock.Listeners.OneblockEvent;
import fr.vyraah.oneblock.Listeners.Wells;
import fr.vyraah.oneblock.Main;
import fr.vyraah.oneblock.Listeners.GuisEvents;
import org.bukkit.Bukkit;
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
        pm.registerEvents(new GuisEvents(), plugin);
        pm.registerEvents(new GenericEvents(), plugin);
        pm.registerEvents(new Wells(), plugin);
        pm.registerEvents(new OneblockEvent(), plugin);
    }
}
