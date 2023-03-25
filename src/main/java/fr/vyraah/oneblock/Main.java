package fr.vyraah.oneblock;

import com.github.yannicklamprecht.worldborder.api.WorldBorderApi;
import fr.vyraah.oneblock.SQL.MySQL;
import fr.vyraah.oneblock.managers.CommandsManager;
import fr.vyraah.oneblock.managers.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    public WorldBorderApi worldBorderApi = null;
    public FileConfiguration config = getConfig();
    public static Main INSTANCE;
    private CommandsManager commandManager;

    public MySQL mysql = new MySQL();
    @Override
    public void onEnable() {

        // world border api implementation

        boolean canStart = true;

        try{
            RegisteredServiceProvider<WorldBorderApi> worldBorderApiRegisteredServiceProvider = getServer().getServicesManager().getRegistration(WorldBorderApi.class);
            worldBorderApi = worldBorderApiRegisteredServiceProvider.getProvider();
        }catch (NoClassDefFoundError e){
            Bukkit.getLogger().info("==================================================================================");
            Bukkit.getLogger().info("Ce plugin ne peut pas marcher tout seul :");
            Bukkit.getLogger().info("Veuillez ajouter le plugin WorldBorderApi a votre server");
            Bukkit.getLogger().info("lien : https://github.com/yannicklamprecht/WorldBorderAPI/releases/tag/1.182.0");
            Bukkit.getLogger().info("==================================================================================");
            canStart = false;
        }

        if(getServer().getPluginManager().getPlugin("Multiverse-Core") == null){
            Bukkit.getLogger().info("==================================================================================");
            Bukkit.getLogger().info("Ce plugin ne peut pas marcher tout seul :");
            Bukkit.getLogger().info("Veuillez ajouter le plugin Multiverse-Core a votre server");
            Bukkit.getLogger().info("lien : https://www.spigotmc.org/resources/multiverse-core.390/");
            Bukkit.getLogger().info("==================================================================================");
            canStart = false;
        }

        if(getServer().getPluginManager().getPlugin("VoidGen") == null){
            Bukkit.getLogger().info("==================================================================================");
            Bukkit.getLogger().info("Ce plugin ne peut pas marcher tout seul :");
            Bukkit.getLogger().info("Veuillez ajouter le plugin VoidGen a votre server");
            Bukkit.getLogger().info("lien : https://www.spigotmc.org/resources/voidgen.25391/");
            Bukkit.getLogger().info("==================================================================================");
            canStart = false;
        }

        if(!canStart) {
            onDisable();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try{
            Bukkit.getWorld("islands").getDifficulty();
        }catch (NullPointerException e){
            getServer().dispatchCommand(getServer().getConsoleSender(), "mv create test NORMAL -g VoidGen -t FLAT");
        }

        //--------------------------------------------------------------------------------

        // CHANGE PAR TES PROPRES VALEURS POUR TE CONNECTER A LA DB :)

        config.addDefault("mysql_host", "localhost");
        config.addDefault("mysql_port", 3306); // 3306 <= PORT PRINCPAL DE SQL (MariaDb).
        config.addDefault("mysql_user", "root");
        config.addDefault("mysql_password", "root");
        config.addDefault("mysql_database", "vyraahob");
        config.addDefault("next_island_x", 0); // utile pour savoir les coos de la prochaine ile
        config.addDefault("next_island_z", 0);
        config.options().copyDefaults(true);
        saveConfig();

        mysql.connect(config.getString("mysql_host"), config.getInt("mysql_port"), config.getString("mysql_database"), config.getString("mysql_user"), config.getString("mysql_password"));

        //--------------------------------------------------------------------------------

        INSTANCE = this;

        //--------------------------------------------------------------------------------

        Bukkit.getLogger().info("=================================");
        Bukkit.getLogger().info("Loading VyraahOneBlock...");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("===========================");

        //--------------------------------------------------------------------------------

        new EventManager(this).registersEvents();

        this.commandManager = new CommandsManager();
        this.commandManager.initCommands();

    }

    @Override
    public void onDisable(){

        Bukkit.getLogger().info("=================================");
        Bukkit.getLogger().info("Unloading VyraahOneBlock...");
        Bukkit.getLogger().info(" ");
        Bukkit.getLogger().info("===========================");

        mysql.disconnect();
    }
}